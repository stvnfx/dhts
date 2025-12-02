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

        // 1. Delete PeerInfo records older than 45 minutes.
        long deletedPeers = PeerInfo.delete("lastSeen < ?1", cutoff);

        // 2. Update InfoHashStat.peerCount based on remaining PeerInfo entries.
        // This could be expensive. We can do it via a query update or iterating.
        // "UPDATE InfoHashStat i SET peerCount = (SELECT count(p) FROM PeerInfo p WHERE p.infoHash = i.hash)"
        // But HQL might be tricky with subqueries in update.
        // We can use native query or iterate.
        // Given the scale, native query is better.

        InfoHashStat.getEntityManager().createQuery(
            "UPDATE InfoHashStat i SET i.peerCount = (SELECT count(p) FROM PeerInfo p WHERE p.infoHash = i.hash)"
        ).executeUpdate();

        // 3. Delete InfoHashStat entries where peerCount is 0.
        // Note: We might want to keep them if they were recently announced even if 0 peers?
        // But the prompt says "Delete InfoHashStat entries where peerCount is 0".
        // Maybe we should also check lastAnnounced?
        // "cleanupStaleData" implies removing things we don't track anymore.
        // If peerCount is 0, it means we have no peers for it (all expired).
        // But maybe we just received an announce but no peers yet? (Unlikely, announce comes from a peer).
        // I'll stick to the instruction.

        InfoHashStat.delete("peerCount = 0");
    }
}
