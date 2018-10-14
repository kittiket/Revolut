package ev.demo.revolut.application.controller;

import ev.demo.revolut.application.infrastructure.Application;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/application")
public class ApplicationController {
    private static Logger logger = Logger.getLogger(ApplicationController.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/ping")
    public String ping() {
        logger.info("Ping is called");

        return "Server is working!";
    }

    @POST
    @Path("/stop")
    public void stopServer() {
        logger.info("Stop is called");

        Application.stopServer();
    }
}
