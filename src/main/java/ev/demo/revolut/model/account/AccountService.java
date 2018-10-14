package ev.demo.revolut.model.account;

import ev.demo.revolut.db.exception.EntityNotFoundException;
import ev.demo.revolut.model.ValidationException;
import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.account.entity.AccountCreationInput;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class AccountService {

    private AccountRepository accountRepository = new AccountRepository();

    public AccountService() {

    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(String accountId) {
        return accountRepository.find(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account with ID '" + accountId + "' is not found!"));
    }

    public Optional<Account> findAccountById(String accountId) {
        return accountRepository.find(accountId);
    }

    public Account createAccount(AccountCreationInput creationInput) {
        validateCreationInput(creationInput);

        Account account = new Account();
        account.setName(creationInput.getName());
        account.setOwnerId(creationInput.getOwnerId());
        account.setAmount(creationInput.getAmount());
        account.setCurrency(creationInput.getCurrency());

        return accountRepository.insert(account);
    }

    public void deleteAccountById(String accountId) {
        boolean deleted = accountRepository.delete(accountId);
        if (!deleted) {
            throw new EntityNotFoundException("Account with ID '" + accountId + "' is not found!");
        }
    }

    public void transferAmount(Connection connection, String accountFromId, String accountToId, BigDecimal amountToRemove, BigDecimal amountToAdd) {
        Account accountFrom = accountRepository.findForUpdate(connection, accountFromId)
                .orElseThrow(() -> new EntityNotFoundException("Account with ID '" + accountFromId + "' is not found!"));

        Account accountTo = accountRepository.findForUpdate(connection, accountToId)
                .orElseThrow(() -> new EntityNotFoundException("Account with ID '" + accountToId + "' is not found!"));

        if (accountFrom.getAmount().compareTo(amountToRemove) < 0 ) {
            throw new ValidationException("Amount of Account " + accountFrom.getId() + " cannot be reduced to '" + amountToRemove + "', current amount is '" + accountFrom.getAmount() + "'.");
        }

        accountFrom.setAmount(accountFrom.getAmount().subtract(amountToRemove));
        accountTo.setAmount(accountTo.getAmount().add(amountToAdd));

        accountRepository.update(connection, accountFrom);
        accountRepository.update(connection, accountTo);
    }

    private void validateCreationInput(AccountCreationInput creationInput) {
        if (creationInput == null) {
            throw new ValidationException("Account Creation Input cannot be null!");
        }

        if (StringUtils.isEmpty(creationInput.getCurrency())) {
            throw new ValidationException("Currency cannot be empty!");
        }

        if (StringUtils.isEmpty(creationInput.getOwnerId())) {
            throw new ValidationException("OwnerId cannot be empty!");
        }

        if (creationInput.getAmount() == null) {
            throw new ValidationException("Amount cannot be null!");
        }

        if (creationInput.getAmount().signum() == -1) {
            throw new ValidationException("Amount cannot be negative!");
        }
    }
}
