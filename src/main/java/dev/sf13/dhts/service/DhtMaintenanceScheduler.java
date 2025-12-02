package dev.sf13.dhts.service;

import dev.sf13.dhts.model.InfoHashStat;
import dev.sf13.dhts.model.PeerInfo;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DhtMaintenanceScheduler {

    // Run every 45 minutes
    @Scheduled(every = "45m")
    @Transactional
    void cleanupStaleData() {
        Instant cutoff = Instant.now().minus(45, ChronoUnit.MINUTES);

        // 1. Mark PeerInfo records older than 45 minutes as stale.
        PeerInfo.update("stale = true WHERE lastSeen < ?1", cutoff);

        // 2. Update InfoHashStat.peerCount based on active (non-stale) PeerInfo entries.
        InfoHashStat.getEntityManager().createQuery(
            "UPDATE InfoHashStat i SET i.peerCount = (SELECT count(p) FROM PeerInfo p WHERE p.infoHash = i.hash AND p.stale = false)"
        ).executeUpdate();

        // 3. Delete InfoHashStat entries where peerCount is 0.
        InfoHashStat.delete("peerCount = 0");
    }
}
