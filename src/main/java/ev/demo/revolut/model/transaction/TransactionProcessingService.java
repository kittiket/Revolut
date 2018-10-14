package ev.demo.revolut.model.transaction;

import ev.demo.revolut.model.account.AccountService;
import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionStatus;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TransactionProcessingService {

    private static Logger logger = Logger.getLogger(TransactionProcessingService.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private TransactionRepository transactionRepository = new TransactionRepository();
    private AccountService accountService = new AccountService();
    private ExchangeRateService exchangeRateService = new ExchangeRateService();

    private Comparator<Transaction> creationDateAndId = (transaction1, transaction2) -> {
        if (transaction1.getCreatedAt().equals(transaction2.getCreatedAt())) {
            return transaction1.getId().compareTo(transaction2.getId());
        } else {
            return transaction1.getCreatedAt().compareTo(transaction2.getCreatedAt());
        }
    };

    public static void start() {
        scheduler.scheduleAtFixedRate(() -> new TransactionProcessingService().processAllTransactions(), 5, 5, TimeUnit.SECONDS);
        logger.info("TransactionProcessing ScheduledExecutor started");
    }

    public static void stop() {
        scheduler.shutdown();
        logger.info("TransactionProcessing ScheduledExecutor stopped");
    }

    void processAllTransactions() {
        Instant currentTime = Instant.now();

        List<Transaction> transactions = transactionRepository.findAllNewOrInProgress();
        List<Transaction> expiredTransactions = transactions.stream().filter(transaction -> !currentTime.isBefore(transaction.getExpiredAt())).collect(Collectors.toList());
        transactions.removeAll(expiredTransactions);

        processActiveTransactions(transactions);

        expiredTransactions.forEach(transaction -> setErrorStatusIfActual(transaction.getId(), "Transaction expired at " + transaction.getExpiredAt()));
    }

    private void processActiveTransactions(List<Transaction> transactions) {
        //don't process next account transaction while there is another one in progress for the same Account
        Map<String, List<Transaction>> transactionsGroupedByAccounts = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getAccountFrom));

        for (List<Transaction> transactionsOfAccount : transactionsGroupedByAccounts.values()) {
            Optional<Transaction> transactionToProcess = transactionsOfAccount.stream()
                    .min(creationDateAndId)
                    .filter(transaction -> TransactionStatus.NEW == transaction.getStatus());

            transactionToProcess.ifPresent(transaction -> executor.submit(() -> processTransaction(transaction.getId())));
        }
    }

    private void processTransaction(String transactionId) {
        logger.debug("Processing transaction '" + transactionId + "' started");

        try{
            Optional<Transaction> lockedTransaction = setInProgressStatusIfActual(transactionId);
            if (!lockedTransaction.isPresent()) {
                logger.debug("Transaction '" + transactionId + "' is currently processing by another thread");
                return;
            }

            Transaction transaction = lockedTransaction.get();

            Account accountFrom = accountService.getAccountById(transaction.getAccountFrom());
            Account accountTo = accountService.getAccountById(transaction.getAccountTo());

            BigDecimal amountToRemove = exchangeRateService.convert(transaction.getAmount(), transaction.getCurrency(), accountFrom.getCurrency());
            BigDecimal amountToAdd = exchangeRateService.convert(transaction.getAmount(), transaction.getCurrency(), accountTo.getCurrency());

            transactionRepository.runInNewTransaction(connection -> {
                accountService.transferAmount(connection, transaction.getAccountFrom(), transaction.getAccountTo(), amountToRemove, amountToAdd);
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.update(connection, transaction);
            });

        } catch (Exception e) {
            logger.error("Processing Transaction '" + transactionId + "' failed with exception " + e.toString() + ": " + e.getMessage() + "!");
            setErrorStatusIfActual(transactionId, "Processing failed with error "  + e.toString() + ": " + e.getMessage());
        }

        logger.debug("Processing transaction '" + transactionId + "' completed");
    }

    private Optional<Transaction> setInProgressStatusIfActual(String transactionId) {
        return transactionRepository.lockAndTryChange(transactionId, transaction -> {
            if (transaction.getStatus() == TransactionStatus.NEW) {
                transaction.setStatus(TransactionStatus.IN_PROGRESS);
                return true;
            }
            return false;
        });
    }

    private void setErrorStatusIfActual(String transactionId, String errorMessage) {
        try{
            transactionRepository.lockAndTryChange(transactionId, transaction -> {
                if (transaction.getStatus() == TransactionStatus.IN_PROGRESS || transaction.getStatus() == TransactionStatus.NEW) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transaction.setError(errorMessage);
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            logger.error("Processing failed Transaction '" + transactionId + "' failed with exception " + e.toString() + ": " + e.getMessage() + "!");
        }
    }
}
