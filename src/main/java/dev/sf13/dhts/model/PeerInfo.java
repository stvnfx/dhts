package dev.sf13.dhts.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(indexes = {
    @Index(name = "idx_peerinfo_hash", columnList = "infoHash"),
    @Index(name = "idx_peerinfo_last_seen", columnList = "lastSeen")
})
public class PeerInfo extends PanacheEntity {
    public String infoHash;
    public String ip;
    public int port;
    public Instant lastSeen = Instant.now();
    public boolean stale = false;
}
