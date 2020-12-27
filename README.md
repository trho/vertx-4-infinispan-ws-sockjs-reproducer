# The issue
Since vert.x 4.0.0.Beta3 websocket connections for sockjs bridge are not established
for a load-balanced verticle with several instances until the same instance was queried twice.
With 2 instances the second websocket connection fails, with 3+ at least the first one.

The complete setup of this reproducer is needed, changing one of the following "fixes" the issue:

 * replacing several verticles by one verticle (Main.java:43)
 * ClusteredSessionStore with LocalSessionStore in (MainVerticle.java:57)
 * Infinispan with Hazelcast cluster manager (Main.java:26)

To reproduce the issue run start.sh and follow the instructions:

Open browser http://localhost:8888/static/index.html
and refresh until the output changes from
  'websocket: closed. Reason: All transports failed, code : 2000'
to"
  'websocket: connected'

# Analysis

It seems that the initial eventbus/info request and the upgrade request have to be processed by the same eventbus thread once
to sucessfully establish a connection.
See also the output at the end of this file.
Afterward this is not relevant anymore and every websocket connection is established.

I traced the execution and the endHandler is never called in Http1xServerRequest.webSocket(..):

io.vertx.core.http.impl.Http1xServerRequest.java
```
  public Future<ServerWebSocket> toWebSocket() {
    return webSocket().map(ws -> { // stuck
      ws.accept(); // not reached for the first 5-7 requests
      return ws;
    });
  }

  private InboundBuffer<Object> pendingQueue() {
    if (pending == null) {
      pending = new InboundBuffer<>(conn.getContext(), 8);
      pending.drainHandler(v -> conn.doResume());
      pending.handler(buffer -> {
        if (buffer == InboundBuffer.END_SENTINEL) { // not reached

private void webSocket(PromiseInternal<ServerWebSocket> promise) {
...
   endHandler(v -> {
      if (!failed[0]) { // not reached
        // Handle the request once we have the full body.
        request = new DefaultFullHttpRequest(
          request.protocolVersion(),
          request.method(),
          request.uri(),
          body.getByteBuf(),
          request.headers(),
          EmptyHttpHeaders.INSTANCE
        );
        conn.createWebSocket(this, promise);
      }
```
# Expected output of the reproducer (I removed traces of static resources):

2020-12-27 17:59:29,275 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088369270
2020-12-27 17:59:29,448 [vert.x-eventloop-thread-3] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1963499860 accepting request GET http://localhost:8888/eventbus/387/ua1glmxf/websocket

2020-12-27 17:59:31,077 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088371072
2020-12-27 17:59:31,106 [vert.x-eventloop-thread-7] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1406752118 accepting request GET http://localhost:8888/eventbus/696/pvtvizhi/websocket

2020-12-27 17:59:32,171 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088372167
2020-12-27 17:59:32,191 [vert.x-eventloop-thread-6] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 992128360 accepting request GET http://localhost:8888/eventbus/112/rfy1lqdf/websocket

2020-12-27 17:59:33,265 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088373259
2020-12-27 17:59:33,291 [vert.x-eventloop-thread-4] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1764023878 accepting request GET http://localhost:8888/eventbus/797/du00zq42/websocket

2020-12-27 17:59:34,423 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088374418
2020-12-27 17:59:34,447 [vert.x-eventloop-thread-10] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 2113040178 accepting request GET http://localhost:8888/eventbus/322/vc502vlv/websocket

2020-12-27 17:59:35,842 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088375837
2020-12-27 17:59:35,859 [vert.x-eventloop-thread-1] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1632580694 accepting request GET http://localhost:8888/eventbus/930/jkyhjcuk/websocket

2020-12-27 17:59:37,079 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088377074
2020-12-27 17:59:37,101 [vert.x-eventloop-thread-9] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 189513399 accepting request GET http://localhost:8888/eventbus/030/rlafjmvw/websocket

2020-12-27 17:59:38,393 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088378388
2020-12-27 17:59:38,419 [vert.x-eventloop-thread-2] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 11896205 accepting request GET http://localhost:8888/eventbus/589/0vl5mmsa/websocket

2020-12-27 17:59:39,659 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/info?t=1609088379654
2020-12-27 17:59:39,673 [vert.x-eventloop-thread-8] TRACE io.vertx.ext.web.impl.RouterImpl - Router: 1156067491 accepting request GET http://localhost:8888/eventbus/613/eqkbqzbz/websocket
2020-12-27 17:59:39,756 [vert.x-eventloop-thread-8] INFO  Main - SOCKET_CREATED
