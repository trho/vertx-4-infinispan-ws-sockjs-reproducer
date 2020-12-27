package com.github.trho.reproducer;

import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.ext.web.sstore.ClusteredSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);


//  static private HttpServerResponse getConfiguredResponse(RoutingContext routingContext, String type) {
//    String address = routingContext.request().getHeader("Origin");
//    if (address == null) {
//      return routingContext.response();
//    }
//    return routingContext.response()
//      .putHeader("content-type", type)
//      .putHeader("Access-Control-Allow-Origin", address)
//      .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
//      .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
//      .putHeader("Access-Control-Allow-Credentials", "true");
//  }
//
//  private void doCORSResponse(RoutingContext routingContext) {
//    String origin = routingContext.request().getHeader("Origin");
//    if (origin == null) {
//      routingContext.next();
//    }
//    getConfiguredResponse(routingContext, "text/plain")
//      .setStatusCode(204)
//      .end();
//  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router = Router.router(vertx);

    router.post().handler(BodyHandler.create());

    // works
//    SessionStore store = LocalSessionStore.create(vertx);
    // breaks with Beta3
    SessionStore store = ClusteredSessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(store);
    // default 30 min
    sessionHandler.setSessionTimeout(30 * 60 * 1000);
    sessionHandler.setNagHttps(false);
    router.route().handler(sessionHandler);

    router.route("/static/*").handler(StaticHandler.create("webroot"));

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions();


// CORS
//    router.route()
//      .handler(CorsHandler.create("*")
//        .allowedMethod(HttpMethod.GET)
//        .allowedMethod(HttpMethod.POST)
//        .allowCredentials(true)
//        .allowedMethod(HttpMethod.OPTIONS).allowedHeader("Content-Type, Authorization")
//      );

    // router.options("/eventbus/*").handler(this::doCORSResponse);
    router.mountSubRouter("/eventbus", sockJSHandler.bridge(options,
      event -> {
        JsonObject rawMessage = event.getRawMessage();
        logger.info("{}", event.type());
        event.complete(true);
      }));

    HttpServerOptions httpoptions = new HttpServerOptions();

    vertx.createHttpServer(httpoptions).requestHandler(router)
      .listen(8888, startResult -> {
        if (startResult.succeeded()) {
          logger.info("[DEPLOY] [SUCCESS] in {} --> {}", deploymentID(), startResult.result().actualPort());
        } else {
          logger.error(">[DEPLOY] [ERROR]  in {} --> {}", deploymentID(), startResult.cause().getMessage());
        }
      });
  }
}
