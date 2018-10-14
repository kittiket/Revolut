package ev.demo.revolut.application.controller;

import ev.demo.revolut.db.exception.EntityNotFoundException;
import ev.demo.revolut.model.ValidationException;
import org.glassfish.grizzly.utils.Exceptions;

import javax.ws.rs.core.Response;

class ResponseBuilder {

    Response getResponse(Object entity) {
        return Response.status(Response.Status.OK).entity(entity).build();
    }

    Response createResponse(Object entity) {
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    Response deleteResponse() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    Response badRequestResponse(String errorMessage) {
        return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
    }

    Response exceptionResponse(Exception exception) {
        if (exception instanceof EntityNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).entity(Exceptions.getStackTraceAsString(exception)).build();
        }
        if (exception instanceof ValidationException) {
            return Response.status(422).entity(Exceptions.getStackTraceAsString(exception)).build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Exceptions.getStackTraceAsString(exception)).build();
    }
}
