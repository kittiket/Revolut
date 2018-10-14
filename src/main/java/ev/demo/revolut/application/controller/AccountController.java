package ev.demo.revolut.application.controller;

import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.account.AccountService;
import ev.demo.revolut.model.account.entity.AccountCreationInput;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/accounts")
public class AccountController {
    private static Logger logger = Logger.getLogger(AccountController.class);

    private ResponseBuilder responseBuilder = new ResponseBuilder();
    private AccountService accountService = new AccountService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAccounts() {
        logger.debug("getAllAccounts is called");
        try{
            List<Account> accounts = accountService.getAllAccounts();
            return responseBuilder.getResponse(accounts);

        } catch (Exception e) {
            logger.error("getAllAccounts failed with exception!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response getAccountById(@PathParam("id") String accountId) {
        logger.debug("getAccountById is called for id '" + accountId + "'");

        if (StringUtils.isEmpty(accountId)) {
            return responseBuilder.badRequestResponse("Account ID cannot be empty");
        }

        try{
            Account account = accountService.getAccountById(accountId);
            return responseBuilder.getResponse(account);

        } catch (Exception e) {
            logger.error("getTransactionById failed with exception for id '" + accountId + "'!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(AccountCreationInput creationInput) {
        logger.debug("createAccount is called for creationInput '" + creationInput + "'");

        if (creationInput == null) {
            return responseBuilder.badRequestResponse("Creation Input cannot be empty");
        }

        try{
            Account createdAccount = accountService.createAccount(creationInput);
            return responseBuilder.createResponse(createdAccount);

        } catch (Exception e) {
            logger.error("createAccount failed with exception!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAccountById(@PathParam("id")String accountId) {
        logger.debug("deleteAccountById is called for id '" + accountId + "'");

        if (StringUtils.isEmpty(accountId)) {
            return responseBuilder.badRequestResponse("Account ID cannot be empty");
        }

        try{
            accountService.deleteAccountById(accountId);
            return responseBuilder.deleteResponse();

        } catch (Exception e) {
            logger.error("deleteAccountById is failed with exception for id '" + accountId + "'!", e);
            return responseBuilder.exceptionResponse(e);
        }
    }
}
