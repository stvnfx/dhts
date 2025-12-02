package dev.sf13.dhts.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class InfoHashStat extends PanacheEntityBase {
    @Id
    @Column(length = 40)
    public String hash; // The 40-character hex info hash

    public Instant lastAnnounced = Instant.now();
    public long peerCount = 0; // Calculated metric
}
