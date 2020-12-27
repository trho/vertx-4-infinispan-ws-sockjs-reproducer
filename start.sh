echo "-------------------------"
echo "use java sdk version >=11"
echo "-------------------------"

mvn package -DskipTests=true

echo ""
echo "-------------------------\033[0;31m Reproducer \033[0m----------------------------"
echo "Open browser http://localhost:8888/static/index.html"
echo "and refresh until the output changes from "
echo "  'websocket: closed. Reason: All transports failed, code : 2000'"
echo "to"
echo "  'websocket: connected'"
echo "-----------------------------------------------------------------"
java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Djava.net.preferIPv4Stack=true -jar target/vertx-4-infinispan-ws-sockjs-reproducer-1.0.0-SNAPSHOT-fat.jar
