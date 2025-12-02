package dev.sf13.dhts.bt;

import bt.dht.MldhtService;
import bt.metainfo.TorrentId;
import bt.net.Peer;
import bt.peer.PeerOptions;
import bt.service.IRuntimeLifecycleBinder;
import com.google.inject.Binder;
import com.google.inject.Module;
import dev.sf13.dhts.events.NewAnnounceEvent;
import dev.sf13.dhts.service.DhtMetricsReporter;
import io.quarkus.arc.Arc;
import jakarta.enterprise.event.Event;
import lbms.plugins.mldht.kad.DHT;
import lbms.plugins.mldht.kad.DHT.IncomingMessageListener;
import lbms.plugins.mldht.kad.messages.AnnounceRequest;
import lbms.plugins.mldht.kad.messages.MessageBase;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DhtModule implements Module {

    @Override
    public void configure(Binder binder) {
        try {
            binder.bind(DhtListenerSetup.class).toConstructor(
                DhtListenerSetup.class.getConstructor(IRuntimeLifecycleBinder.class, bt.dht.DHTService.class)
            ).asEagerSingleton();
        } catch (NoSuchMethodException e) {
            binder.addError(e);
        }
    }

    public static class DhtListenerSetup implements Runnable {
        private final MldhtService dhtService;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public DhtListenerSetup(IRuntimeLifecycleBinder lifecycleBinder, bt.dht.DHTService dhtService) {
            this.dhtService = (MldhtService) dhtService;
            lifecycleBinder.onStartup(this);
        }

        @Override
        public void run() {
            try {
                java.lang.reflect.Field dhtField = MldhtService.class.getDeclaredField("dht");
                dhtField.setAccessible(true);
                DHT dht = (DHT) dhtField.get(dhtService);

                dht.addIncomingMessageListener(new IncomingMessageListener() {
                    @Override
                    public void received(DHT dht, MessageBase message) {
                        if (message instanceof AnnounceRequest) {
                            AnnounceRequest req = (AnnounceRequest) message;
                            try {
                                Event<NewAnnounceEvent> event = Arc.container().beanManager().getEvent().select(NewAnnounceEvent.class);

                                int port = req.getOrigin().getPort();
                                Peer peer = new Peer() {
                                    @Override public InetAddress getInetAddress() { return req.getOrigin().getAddress(); }
                                    @Override public boolean isPortUnknown() { return false; }
                                    @Override public int getPort() { return port; }
                                    @Override public Optional<bt.net.PeerId> getPeerId() { return Optional.empty(); }
                                    @Override public PeerOptions getOptions() { return PeerOptions.defaultOptions(); }
                                };

                                TorrentId infoHash = TorrentId.fromBytes(req.getInfoHash().getHash());
                                event.fire(new NewAnnounceEvent(infoHash, peer));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                // Periodic task to update metrics
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        int count = dht.getNode().getNumEntriesInRoutingTable();
                        DhtMetricsReporter reporter = Arc.container().instance(DhtMetricsReporter.class).get();
                        reporter.setDhtNodesCount(count);
                    } catch (Exception e) {
                        // ignore or log
                    }
                }, 10, 10, TimeUnit.SECONDS);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
