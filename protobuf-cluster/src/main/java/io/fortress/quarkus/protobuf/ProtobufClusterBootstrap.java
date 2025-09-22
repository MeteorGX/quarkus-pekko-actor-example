package io.fortress.quarkus.protobuf;

import io.fortress.quarkus.pekko.actor.extension.ActorContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.management.javadsl.PekkoManagement;

@ApplicationScoped
public class ProtobufClusterBootstrap {


    @Startup
    @Default
    @Produces
    @ApplicationScoped
    public PekkoManagement createPekkoManagement(ActorContainer container) {
        Cluster cluster = Cluster.get(container.system());
        PekkoManagement management = PekkoManagement.get(container.system());
        cluster.registerOnMemberUp(() -> {
            management.start();
        });

        cluster.registerOnMemberRemoved(() -> {
            management.stop();
        });
        return management;
    }

}
