package ev.demo.revolut.model.transaction;

import ev.demo.revolut.db.jdbc.h2.H2DataBaseProvider;
import ev.demo.revolut.model.account.AccountService;
import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.account.entity.AccountCreationInput;
import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionCreationInput;
import ev.demo.revolut.model.transaction.entity.TransactionStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TransactionProcessingTest {

    private final static long WAITING_TIME_OUT_MILLIS = 10000;
    private final static long CHECK_TIME_OUT_MILLIS = 10;

    private TransactionProcessingService processingService = new TransactionProcessingService();
    private TransactionService transactionService = new TransactionService();
    private AccountService accountService = new AccountService();
    private ExchangeRateService exchangeRateService = new ExchangeRateService();
    private TransactionRepository transactionRepository = new TransactionRepository();

    private Function<String, Boolean> isTransactionProcessed = (String id) -> {
        Transaction transaction = transactionService.getTransactionById(id);
        return TransactionStatus.COMPLETED == transaction.getStatus() || TransactionStatus.FAILED == transaction.getStatus();
    };

    static {
        org.apache.log4j.BasicConfigurator.configure();
        H2DataBaseProvider.init();
        TransactionProcessingService.start();
    }

    @Test
    public void processCorrectTransaction() {
        Account account1 = createAccount(1000, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer = new BigDecimal(100);


        Transaction transaction = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        assertEquals(TransactionStatus.NEW, transaction.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction.getId()));

        Transaction processedTransaction = transactionService.getTransactionById(transaction.getId());
        assertNotNull(processedTransaction);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction.getStatus());

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount().subtract(amountToTransfer), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount().add(amountToTransfer), processedAccount2.getAmount());
    }

    @Test
    public void processCorrectTransaction_differentCurrencies() {
        String currency1 = "USD";
        String currency2 = "EUR";
        String currency3 = "RUR";
        BigDecimal exchangeRate31 = exchangeRateService.getExchangeRate(currency3, currency1);
        BigDecimal exchangeRate32 = exchangeRateService.getExchangeRate(currency3, currency2);

        Account account1 = createAccount(1000, currency1);
        Account account2 = createAccount(2000, currency2);

        BigDecimal amountToTransfer = new BigDecimal(100);


        Transaction transaction = createTransaction(account1.getId(), account2.getId(), amountToTransfer, currency3);
        assertEquals(TransactionStatus.NEW, transaction.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction.getId()));

        Transaction processedTransaction = transactionService.getTransactionById(transaction.getId());
        assertNotNull(processedTransaction);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction.getStatus());

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount().subtract(amountToTransfer.multiply(exchangeRate31)), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount().add(amountToTransfer.multiply(exchangeRate32)), processedAccount2.getAmount());
    }

    @Test
    public void processIncorrectTransaction() {
        Account account1 = createAccount(100, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer = new BigDecimal(200);


        Transaction transaction = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        assertEquals(TransactionStatus.NEW, transaction.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction.getId()));

        Transaction processedTransaction = transactionService.getTransactionById(transaction.getId());
        assertNotNull(processedTransaction);
        assertEquals(TransactionStatus.FAILED, processedTransaction.getStatus());
        assertTrue(processedTransaction.getError().contains("ValidationException"));

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount(), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount(), processedAccount2.getAmount());
    }

    @Test
    public void processSeveralTransactionsOneByOne() {
        Account account1 = createAccount(1000, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer = new BigDecimal(200);


        Transaction transaction1 = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        Transaction transaction2 = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        Transaction transaction3 = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        assertEquals(TransactionStatus.NEW, transaction1.getStatus());
        assertEquals(TransactionStatus.NEW, transaction2.getStatus());
        assertEquals(TransactionStatus.NEW, transaction3.getStatus());

        Transaction waitingTransaction2 = transactionService.getTransactionById(transaction2.getId());
        assertNotNull(waitingTransaction2);
        assertEquals(TransactionStatus.NEW, waitingTransaction2.getStatus());
        Transaction waitingTransaction3 = transactionService.getTransactionById(transaction3.getId());
        assertNotNull(waitingTransaction3);
        assertEquals(TransactionStatus.NEW, waitingTransaction3.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction1.getId()));

        Transaction processedTransaction1 = transactionService.getTransactionById(transaction1.getId());
        assertNotNull(processedTransaction1);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction1.getStatus());

        waitingTransaction3 = transactionService.getTransactionById(transaction3.getId());
        assertNotNull(waitingTransaction3);
        assertEquals(TransactionStatus.NEW, waitingTransaction3.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction2.getId()));

        Transaction processedTransaction2 = transactionService.getTransactionById(transaction2.getId());
        assertNotNull(processedTransaction2);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction2.getStatus());

        waitForCondition(() -> isTransactionProcessed.apply(transaction3.getId()));

        Transaction processedTransaction3 = transactionService.getTransactionById(transaction3.getId());
        assertNotNull(processedTransaction3);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction3.getStatus());


        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount().subtract(amountToTransfer.multiply(new BigDecimal(3))), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount().add(amountToTransfer.multiply(new BigDecimal(3))), processedAccount2.getAmount());
    }

    @Test
    public void processTransactionOnlyOnceInSeveralThreads() {
        Account account1 = createAccount(1000, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer = new BigDecimal(100);


        Transaction transaction = createTransaction(account1.getId(), account2.getId(), amountToTransfer, "USD");
        assertEquals(TransactionStatus.NEW, transaction.getStatus());

        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> processingService.processAllTransactions());
        }

        waitForCondition(() -> isTransactionProcessed.apply(transaction.getId()));

        Transaction processedTransaction = transactionService.getTransactionById(transaction.getId());
        assertNotNull(processedTransaction);
        assertEquals(TransactionStatus.COMPLETED, processedTransaction.getStatus());

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount().subtract(amountToTransfer), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount().add(amountToTransfer), processedAccount2.getAmount());
    }

    @Test
    public void processExpiredTransaction() {
        Account account1 = createAccount(1000, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer = new BigDecimal(100);

        Transaction transaction = createExpiredTransaction(account1.getId(), account2.getId(), amountToTransfer, TransactionStatus.NEW);

        waitForCondition(() -> isTransactionProcessed.apply(transaction.getId()));

        Transaction processedTransaction = transactionService.getTransactionById(transaction.getId());
        assertNotNull(processedTransaction);
        assertEquals(TransactionStatus.FAILED, processedTransaction.getStatus());
        assertNotNull(processedTransaction.getError());

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount(), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount(), processedAccount2.getAmount());
    }

    @Test
    public void processExpiredAndNormalTransaction() {
        Account account1 = createAccount(1000, "USD");
        Account account2 = createAccount(2000, "USD");

        BigDecimal amountToTransfer1 = new BigDecimal(100);
        BigDecimal amountToTransfer2 = new BigDecimal(200);

        Transaction activeTransaction = createTransaction(account1.getId(), account2.getId(), amountToTransfer1, "USD");
        Transaction expiredTransaction = createExpiredTransaction(account1.getId(), account2.getId(), amountToTransfer2, TransactionStatus.NEW);

        waitForCondition(() -> isTransactionProcessed.apply(activeTransaction.getId()));
        waitForCondition(() -> isTransactionProcessed.apply(expiredTransaction.getId()));

        Transaction activeProcessedTransaction = transactionService.getTransactionById(activeTransaction.getId());
        assertNotNull(activeProcessedTransaction);
        assertEquals(TransactionStatus.COMPLETED, activeProcessedTransaction.getStatus());

        Transaction expiredProcessedTransaction = transactionService.getTransactionById(expiredTransaction.getId());
        assertNotNull(expiredProcessedTransaction);
        assertEquals(TransactionStatus.FAILED, expiredProcessedTransaction.getStatus());
        assertNotNull(expiredProcessedTransaction.getError());

        Account processedAccount1 = accountService.getAccountById(account1.getId());
        assertNotNull(processedAccount1);
        assertEquals(account1.getAmount().subtract(amountToTransfer1), processedAccount1.getAmount());

        Account processedAccount2 = accountService.getAccountById(account2.getId());
        assertNotNull(processedAccount2);
        assertEquals(account2.getAmount().add(amountToTransfer1), processedAccount2.getAmount());
    }


    private Account createAccount(int amount, String currency) {
        AccountCreationInput creationInput = new AccountCreationInput();
        creationInput.setName("account");
        creationInput.setOwnerId("user1");
        creationInput.setAmount(new BigDecimal(amount));
        creationInput.setCurrency(currency);

        Account account = accountService.createAccount(creationInput);
        assertNotNull(account);
        return account;
    }

    private Transaction createTransaction(String accountFrom, String accountTo, BigDecimal amount, String currency) {
        TransactionCreationInput creationInput = new TransactionCreationInput();
        creationInput.setAccountFrom(accountFrom);
        creationInput.setAccountTo(accountTo);
        creationInput.setAmount(amount);
        creationInput.setCurrency(currency);
        creationInput.setCreatedBy("user1");

        Transaction transaction = transactionService.createTransaction(creationInput);
        assertNotNull(transaction);
        return transaction;
    }

    private Transaction createExpiredTransaction(String accountFrom, String accountTo, BigDecimal amount, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setAccountFrom(accountFrom);
        transaction.setAccountTo(accountTo);
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setStatus(status);
        transaction.setCreatedBy("user1");
        transaction.setCreatedAt(Instant.now().minusSeconds(10));
        transaction.setExpiredAt(Instant.now().minusSeconds(1));

        return transactionRepository.insert(transaction);
    }


    private void waitForCondition(BooleanSupplier condition) {
        Instant endTime = Instant.now().plusMillis(WAITING_TIME_OUT_MILLIS);

        while (!condition.getAsBoolean() && Instant.now().isBefore(endTime)) {
            try {
                Thread.sleep(CHECK_TIME_OUT_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        assertTrue("Failed with timeout", Instant.now().isBefore(endTime));
    }
}
