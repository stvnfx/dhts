package dev.sf13.dhts.service;

import dev.sf13.dhts.events.NewAnnounceEvent;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.DHT;
import lbms.plugins.mldht.kad.DHT.IncomingMessageListener;
import lbms.plugins.mldht.kad.DHT.LogLevel;
import lbms.plugins.mldht.kad.messages.AnnounceRequest;
import lbms.plugins.mldht.kad.messages.MessageBase;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Startup
public class MldhtClient {

    private static final Logger LOG = Logger.getLogger(MldhtClient.class);

    @Inject
    Event<NewAnnounceEvent> eventBus;

    @Inject
    DhtMetricsReporter metricsReporter;

    private DHT dht;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void init() {
        LOG.info("Initializing MLDHT...");

        try {
            dht = new DHT(DHT.DHTtype.IPV4_DHT);

            // Basic configuration (can be extended)
            dht.setLogLevel(LogLevel.Info);

            dht.addIncomingMessageListener(new IncomingMessageListener() {
                @Override
                public void received(DHT dht, MessageBase message) {
                    if (message instanceof AnnounceRequest) {
                        AnnounceRequest req = (AnnounceRequest) message;
                        try {
                            String infoHashHex = req.getInfoHash().toString();
                            InetAddress ip = req.getOrigin().getAddress();
                            int port = req.getOrigin().getPort();

                            eventBus.fireAsync(new NewAnnounceEvent(infoHashHex, ip, port));
                        } catch (Exception e) {
                            LOG.error("Error processing AnnounceRequest", e);
                        }
                    }
                }
            });

            // Bootstrap nodes
            dht.addDHTNode("router.bittorrent.com", 6881);
            dht.addDHTNode("dht.transmissionbt.com", 6881);
            dht.addDHTNode("router.utorrent.com", 6881);

            dht.start(new DHTConfiguration() {
                @Override
                public boolean isPersistingID() {
                    return false;
                }

                @Override
                public Path getStoragePath() {
                    return Paths.get(".");
                }

                @Override
                public int getListeningPort() {
                    return 49001;
                }

                @Override
                public boolean noRouterBootstrap() {
                    return false;
                }

                @Override
                public boolean allowMultiHoming() {
                    return false;
                }
            });

            // Periodically report metrics
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    int count = dht.getNode().getNumEntriesInRoutingTable();
                    metricsReporter.setDhtNodesCount(count);
                } catch (Exception e) {
                    LOG.error("Error updating DHT metrics", e);
                }
            }, 10, 10, TimeUnit.SECONDS);

            LOG.info("MLDHT initialized and started.");

        } catch (Exception e) {
            LOG.error("Failed to initialize MLDHT", e);
            throw new RuntimeException(e);
        }
    }
}
