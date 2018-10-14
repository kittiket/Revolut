package ev.demo.revolut.application.infrastructure;

import ev.demo.revolut.db.jdbc.h2.H2DataBaseProvider;
import ev.demo.revolut.model.transaction.TransactionProcessingService;
import org.apache.log4j.Logger;
import java.io.IOException;

public class Application {

    private static Logger logger = Logger.getLogger(Application.class);

    private static final String BASE_URL = "http://localhost:8484/demo/";

    private static Server server;

    public static void main(String[] args) throws IOException {
        startServer();
    }

    private static void startServer() {
        org.apache.log4j.BasicConfigurator.configure();
        logger.info("Server starting for URL '" + BASE_URL + "'.");

        H2DataBaseProvider.init();
        TransactionProcessingService.start();

        server = new Server(BASE_URL);
        server.start();

        logger.info("Server started for URL '" + BASE_URL + "'.");
    }

    public static void stopServer() {
        logger.info("Server stopping for URL '" + BASE_URL + "'.");

        server.stop();
        TransactionProcessingService.stop();

        logger.info("Server stopped for URL '" + BASE_URL + "'.");
        System.exit(0);
    }
}
