package ev.demo.revolut.model.transaction;


import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionProcessingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExecutorService executor;

    @InjectMocks
    private TransactionProcessingService processingService = new TransactionProcessingService();

    @Test
    public void processAllTransactions_executorCalled(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 0));

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);

        processingService.processAllTransactions();

        verify(executor).submit(any(Runnable.class));
    }

    @Test
    public void processAllTransactions_runOnlyOneOldestPerAccount(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 10));
        transactions.add(createTransaction("account1", 5));
        transactions.add(createTransaction("account1", 0));

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);
        when(executor.submit(any(Runnable.class))).then(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        });

        processingService.processAllTransactions();

        verify(executor).submit(any(Runnable.class));
        verify(transactionRepository).lockAndTryChange(eq(transactions.get(2).getId()), any());
    }

    @Test
    public void processAllTransactions_runOnlyOneOldestAndSmallestIdPerAccount(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 0));
        transactions.add(createTransaction("account1", 0));
        transactions.add(createTransaction("account1", 0));

        Instant createdAt = Instant.now().minusSeconds(5);
        transactions.get(1).setCreatedAt(createdAt);
        transactions.get(2).setCreatedAt(createdAt);
        transactions.get(1).setId("b");
        transactions.get(2).setId("a");

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);
        when(executor.submit(any(Runnable.class))).then(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        });

        processingService.processAllTransactions();

        verify(executor).submit(any(Runnable.class));
        verify(transactionRepository).lockAndTryChange(eq(transactions.get(2).getId()), any());
    }

    @Test
    public void processAllTransactions_dontRunNewIfInProgressExists(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 1));
        transactions.add(createTransaction("account1", 2));
        transactions.get(0).setStatus(TransactionStatus.IN_PROGRESS);

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);

        processingService.processAllTransactions();

        verify(executor, times(0)).submit(any(Runnable.class));
    }

    @Test
    public void processAllTransactions_runSecondAccountTransactionOnly(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 1));
        transactions.add(createTransaction("account1", 2));
        transactions.add(createTransaction("account2", 3));
        transactions.get(0).setStatus(TransactionStatus.IN_PROGRESS);


        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);
        when(executor.submit(any(Runnable.class))).then(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        });

        processingService.processAllTransactions();

        verify(executor).submit(any(Runnable.class));
        verify(transactionRepository).lockAndTryChange(eq(transactions.get(2).getId()), any());
    }

    @Test
    public void processAllTransactions_runTransactionsOfAllAccounts(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 1));
        transactions.add(createTransaction("account1", 2));
        transactions.add(createTransaction("account2", 3));

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);

        processingService.processAllTransactions();

        verify(executor, times(2)).submit(any(Runnable.class));
    }

    @Test
    public void processAllTransactions_dontSubmitExpiredTransaction(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("account1", 1));
        transactions.add(createExpiredTransaction("account2"));
        transactions.add(createTransaction("account3", 3));

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(transactions);

        processingService.processAllTransactions();

        verify(executor, times(2)).submit(any(Runnable.class));
    }

    @Test
    public void processAllTransactions_processExpiredTransactions(){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createExpiredTransaction("account1"));
        transactions.add(createExpiredTransaction("account2"));
        transactions.add(createTransaction("account3", 0));

        when(transactionRepository.findAllNewOrInProgress()).thenReturn(new ArrayList<>(transactions));
        when(transactionRepository.lockAndTryChange(any(), any())).then(i -> {
            String transactionId = i.getArgument(0);
            Function<Transaction, Boolean> changeAction = i.getArgument(1);

            Transaction transaction = transactions.stream().filter(t -> transactionId.equals(t.getId())).findFirst().get();
            changeAction.apply(transaction);

            return Optional.of(transaction);
        });

        processingService.processAllTransactions();

        assertEquals(TransactionStatus.FAILED, transactions.get(0).getStatus());
        assertNotNull(transactions.get(0).getError());
        assertTrue(transactions.get(0).getError().contains("expired"));

        assertEquals(TransactionStatus.FAILED, transactions.get(1).getStatus());
        assertNotNull(transactions.get(1).getError());
        assertTrue(transactions.get(1).getError().contains("expired"));

        verify(executor, times(1)).submit(any(Runnable.class));
    }

    private Transaction createTransaction(String accountFrom, int createdAtDelay) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setAccountFrom(accountFrom);
        transaction.setAccountTo("anotherAccount");
        transaction.setAmount(new BigDecimal(100.90));
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.NEW);
        transaction.setCreatedAt(Instant.now().plusMillis(createdAtDelay));
        transaction.setExpiredAt(transaction.getCreatedAt().plusSeconds(60));
        return transaction;
    }

    private Transaction createExpiredTransaction(String accountFrom) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setAccountFrom(accountFrom);
        transaction.setAccountTo("anotherAccount");
        transaction.setAmount(new BigDecimal(100.15));
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.NEW);
        transaction.setCreatedAt(Instant.now().minusSeconds(2));
        transaction.setExpiredAt(Instant.now().minusSeconds(1));
        return transaction;
    }
}
