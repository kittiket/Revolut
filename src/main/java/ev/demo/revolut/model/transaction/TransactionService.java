package ev.demo.revolut.model.transaction;

import ev.demo.revolut.db.exception.EntityNotFoundException;
import ev.demo.revolut.model.ValidationException;
import ev.demo.revolut.model.account.AccountService;
import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionCreationInput;
import ev.demo.revolut.model.transaction.entity.TransactionStatus;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

public class TransactionService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60*10;

    private TransactionRepository transactionRepository = new TransactionRepository();
    private AccountService accountService = new AccountService();

    public TransactionService() {

    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.find(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction with ID '" + transactionId + "' is not found!"));
    }

    public Transaction createTransaction(TransactionCreationInput creationInput) {
        validateCreationInput(creationInput);

        Transaction transaction = new Transaction();
        transaction.setAccountFrom(creationInput.getAccountFrom());
        transaction.setAccountTo(creationInput.getAccountTo());
        transaction.setAmount(creationInput.getAmount());
        transaction.setCurrency(creationInput.getCurrency());
        transaction.setStatus(TransactionStatus.NEW);
        transaction.setCreatedBy(creationInput.getCreatedBy());
        transaction.setCreatedAt(Instant.now());
        transaction.setExpiredAt(transaction.getCreatedAt().plusSeconds(DEFAULT_TIMEOUT_SECONDS));

        return transactionRepository.insert(transaction);
    }

    public void deleteTransactionById(String transactionId) {
        boolean deleted = transactionRepository.delete(transactionId);
        if (!deleted) {
            throw new EntityNotFoundException("Transaction with ID '" + transactionId + "' is not found!");
        }
    }

    private void validateCreationInput(TransactionCreationInput creationInput) {
        if (creationInput == null) {
            throw new ValidationException("Transaction Creation Input cannot be null!");
        }

        if (StringUtils.isEmpty(creationInput.getAccountFrom())) {
            throw new ValidationException("AccountFrom cannot be empty!");
        }
        if (!accountService.findAccountById(creationInput.getAccountFrom()).isPresent()) {
            throw new ValidationException("AccountFrom is not found!");
        }

        if (StringUtils.isEmpty(creationInput.getAccountTo())) {
            throw new ValidationException("AccountTo cannot be empty!");
        }
        if (!accountService.findAccountById(creationInput.getAccountTo()).isPresent()) {
            throw new ValidationException("AccountTo is not found!");
        }

        if (creationInput.getAccountFrom().equals(creationInput.getAccountTo())) {
            throw new ValidationException("AccountFrom and AccountTo cannot be the same account!");
        }

        if (creationInput.getAmount() == null) {
            throw new ValidationException("Amount cannot be null!");
        }

        if (creationInput.getAmount().signum() == 0) {
            throw new ValidationException("Amount cannot be zero!");
        }

        if (creationInput.getAmount().signum() == -1) {
            throw new ValidationException("Amount cannot be negative!");
        }

        if (StringUtils.isEmpty(creationInput.getCurrency())) {
            throw new ValidationException("Currency cannot be empty!");
        }
    }
}
