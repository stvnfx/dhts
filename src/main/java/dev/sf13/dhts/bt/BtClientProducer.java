package dev.sf13.dhts.bt;

import bt.Bt;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.runtime.BtClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.nio.file.Path;

@ApplicationScoped
public class BtClientProducer {

    @Produces
    @ApplicationScoped
    @Startup
    public BtClient createClient() {
        return Bt.client()
                .autoLoadModules()
                .module(new DHTModule(new DHTConfig() {
                    @Override
                    public boolean shouldUseRouterBootstrap() {
                        return true;
                    }
                }))
                .module(new dev.sf13.dhts.bt.DhtModule())
                .build();
    }
}
