package ev.demo.revolut.application.infrastructure;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

class Server {
    private String url;
    private HttpServer httpServer;

    Server(String url) {
        this.url = url;
    }

    void start(){
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("ev.demo.revolut.application.controller");
        URI endPointUrl = UriBuilder.fromPath(url).build();
        httpServer = GrizzlyHttpServerFactory.createHttpServer(endPointUrl, resourceConfig);
    }

    void stop(){
        httpServer.shutdown();
    }
}
