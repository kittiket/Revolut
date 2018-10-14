package ev.demo.revolut.application.controller;

import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.TransactionService;
import ev.demo.revolut.model.transaction.entity.TransactionCreationInput;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/transactions")
public class TransactionController {

    private static Logger logger = Logger.getLogger(TransactionController.class);

    private ResponseBuilder responseBuilder = new ResponseBuilder();
    private TransactionService transactionService = new TransactionService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTransactions() {
        logger.debug("getAllTransactions is called");

        try{
            List<Transaction> transactions = transactionService.getAllTransactions();
            return responseBuilder.getResponse(transactions);

        } catch (Exception e) {
            logger.error("getAllTransactions is failed with exception!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response getTransactionById(@PathParam("id") String transactionId) {
        logger.debug("getTransactionById is called for id '" + transactionId + "'");

        if (StringUtils.isEmpty(transactionId)) {
            return responseBuilder.badRequestResponse("Transaction ID cannot be empty");
        }

        try{
            Transaction transaction = transactionService.getTransactionById(transactionId);
            return responseBuilder.getResponse(transaction);

        }  catch (Exception e) {
            logger.error("getTransactionById is failed with exception for id '" + transactionId + "'!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTransaction(TransactionCreationInput creationInput) {
        logger.debug("createTransaction is called for creationInput '" + creationInput + "'");

        if (creationInput == null) {
            return responseBuilder.badRequestResponse("Creation Input cannot be empty");
        }

        try{
            Transaction transaction = transactionService.createTransaction(creationInput);
            return responseBuilder.createResponse(transaction);

        } catch (Exception e) {
            logger.error("createTransaction is failed with exception!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTransactionById(@PathParam("id") String transactionId) {
        logger.debug("deleteTransactionById is called for id '" + transactionId + "'");

        if (StringUtils.isEmpty(transactionId)) {
            return responseBuilder.badRequestResponse("Transaction ID cannot be empty");
        }

        try{
            transactionService.deleteTransactionById(transactionId);
            return responseBuilder.deleteResponse();

        } catch (Exception e) {
            logger.error("deleteTransactionById is failed with exception for id '" + transactionId + "'!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }
}