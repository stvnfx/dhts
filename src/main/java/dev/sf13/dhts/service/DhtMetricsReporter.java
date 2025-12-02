package dev.sf13.dhts.service;

import dev.sf13.dhts.model.InfoHashStat;
import dev.sf13.dhts.model.PeerInfo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@Startup
public class DhtMetricsReporter {

    @Inject
    MeterRegistry registry;

    private final AtomicInteger dhtNodesCount = new AtomicInteger(0);

    @PostConstruct
    void init() {
        Gauge.builder("dht.info_hashes.total", () -> InfoHashStat.count())
             .description("Total unique torrents tracked")
             .register(registry);

        Gauge.builder("dht.peers.total", () -> PeerInfo.count())
             .description("Total unique peers recently seen")
             .register(registry);

        Gauge.builder("dht.peers.dht_nodes.good", dhtNodesCount::get)
             .description("Number of stable DHT nodes in the routing table")
             .register(registry);
    }

    public void setDhtNodesCount(int count) {
        dhtNodesCount.set(count);
    }
}
