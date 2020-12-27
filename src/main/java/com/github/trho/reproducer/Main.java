package com.github.trho.reproducer;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager;
import org.infinispan.manager.DefaultCacheManager;

/**
 * User: Tobias Rho
 * Date: 10.12.20
 * Time: 10:28
 */
public class Main {

  public static void main(String... args) {

    ClusterManager mgr = null;
    try {
      // Hazelcast configuration
//      Config hazelcastConfig = new Config();
//      hazelcastConfig.setClusterName("local-hazelcast-cluster");
//      mgr = new HazelcastClusterManager(hazelcastConfig);
      // vertx4
     mgr = new InfinispanClusterManager(new DefaultCacheManager("infinispan-default.xml"));
      // vertx3
//      mgr = new InfinispanClusterManager(new DefaultCacheManager("infinispan-default-vertx-3.xml"));
  } catch (Exception e) {
      e.printStackTrace();
    }

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        for (int i = 0; i < 8; i++) {
          vertx.deployVerticle(new MainVerticle());
        }

      } else {
        // failed!
      }
    });
  }
}
