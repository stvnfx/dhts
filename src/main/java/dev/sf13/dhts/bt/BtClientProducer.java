package dev.sf13.dhts.bt;

import bt.Bt;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.runtime.BtClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import bt.net.InetPeerAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@ApplicationScoped
public class BtClientProducer {

    @Produces
    @ApplicationScoped
    @Startup
    public BtClient createClient() {
        BtClient client = Bt.client()
                .autoLoadModules()
                .module(new DHTModule(new DHTConfig() {
                    @Override
                    public boolean shouldUseRouterBootstrap() {
                        return true;
                    }

                    @Override
                    public Collection<InetPeerAddress> getBootstrapNodes() {
                        return Arrays.asList(
                            new InetPeerAddress("router.bittorrent.com", 6881),
                            new InetPeerAddress("dht.transmissionbt.com", 6881),
                            new InetPeerAddress("router.utorrent.com", 6881)
                        );
                    }
                }))
                .module(new dev.sf13.dhts.bt.DhtModule())
                .build();

        client.startAsync();
        return client;
    }
}
