package dev.sf13.dhts.service;

import dev.sf13.dhts.events.NewAnnounceEvent;
import dev.sf13.dhts.model.InfoHashStat;
import dev.sf13.dhts.model.PeerInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.net.InetAddress;

@ApplicationScoped
public class DhtPersistenceService {

    private final MeterRegistry registry;
    private final Counter infoHashInserted;
    private final Counter peerUpdates;
    private final Counter announcesReceived;

    public DhtPersistenceService(MeterRegistry registry) {
        this.registry = registry;
        this.infoHashInserted = registry.counter("dht.db.info_hash.inserted");
        this.peerUpdates = registry.counter("dht.db.peer.updates");
        this.announcesReceived = registry.counter("dht.network.announces.received");
    }

    @Transactional
    public void processAnnounce(@Observes NewAnnounceEvent event) {
        announcesReceived.increment();
        String hashHex = event.getInfoHash().toString();

        // Update InfoHashStat
        InfoHashStat stat = InfoHashStat.findById(hashHex);
        if (stat == null) {
            stat = new InfoHashStat();
            stat.hash = hashHex;
            stat.peerCount = 0; // Will be updated by scheduler or lazily
            stat.persist();
            infoHashInserted.increment();
        }
        stat.lastAnnounced = Instant.now();

        // Update PeerInfo
        InetAddress addr = event.getPeer().getInetAddress();
        String peerIp = (addr != null) ? addr.getHostAddress() : null;
        int peerPort = event.getPeer().getPort();

        if (peerIp != null) {
            // Check if peer exists for this hash
            PeerInfo peerInfo = PeerInfo.find("infoHash = ?1 and ip = ?2 and port = ?3", hashHex, peerIp, peerPort).firstResult();
            if (peerInfo == null) {
                peerInfo = new PeerInfo();
                peerInfo.infoHash = hashHex;
                peerInfo.ip = peerIp;
                peerInfo.port = peerPort;
                peerInfo.persist();
            }
            peerInfo.lastSeen = Instant.now();
            peerInfo.stale = false;
            peerUpdates.increment();
        }
    }
}
