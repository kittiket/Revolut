package ev.demo.revolut.model.transaction;

import ev.demo.revolut.db.exception.EntityNotFoundException;
import ev.demo.revolut.model.ValidationException;
import ev.demo.revolut.model.account.AccountService;
import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionCreationInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService = new TransactionService();

    @Test
    public void getAllTransactions_returnAll() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransaction("id1"));
        transactions.add(createTransaction("id2"));

        when(transactionRepository.findAll()).thenReturn(transactions);

        List<Transaction> transactionsFromService = transactionService.getAllTransactions();
        assertNotNull(transactionsFromService);
        assertEquals(transactionsFromService.size(), transactions.size());
        assertEquals(transactionsFromService.get(0).getId(), transactions.get(0).getId());
        assertEquals(transactionsFromService.get(1).getId(), transactions.get(1).getId());
    }

    @Test
    public void getAllTransactions_returnEmptyList() {
        when(transactionRepository.findAll()).thenReturn(new ArrayList<>());

        List<Transaction> transactionsFromService = transactionService.getAllTransactions();
        assertNotNull(transactionsFromService);
        assertTrue(transactionsFromService.isEmpty());
    }

    @Test
    public void getTransactionById_returnTransaction() {
        String id = "id1";
        when(transactionRepository.find(id)).thenReturn(Optional.of(createTransaction(id)));

        Transaction transactionFromService = transactionService.getTransactionById(id);
        assertNotNull(transactionFromService);
        assertEquals(id, transactionFromService.getId());
    }

    @Test(expected = EntityNotFoundException.class)
    public void getTransactionById_throwExceptionIfNotFound() {
        String id = "id1";
        when(transactionRepository.find(id)).thenReturn(Optional.empty());

        transactionService.getTransactionById(id);
    }

    @Test
    public void createTransaction_returnTransaction() {
        String id = "id1";

        TransactionCreationInput creationInput = getFullCreationInput();

        when(transactionRepository.insert(any())).thenAnswer(i -> {
            Transaction transaction = i.getArgument(0);
            transaction.setId(id);
            return transaction;
        });
        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        Transaction transactionFromService = transactionService.createTransaction(creationInput);
        assertNotNull(transactionFromService);
        assertEquals(id, transactionFromService.getId());
        assertEquals(creationInput.getAccountFrom(), transactionFromService.getAccountFrom());
        assertEquals(creationInput.getAccountTo(), transactionFromService.getAccountTo());
        assertEquals(creationInput.getAmount(), transactionFromService.getAmount());
        assertEquals(creationInput.getCurrency(), transactionFromService.getCurrency());
        assertEquals(creationInput.getCreatedBy(), transactionFromService.getCreatedBy());
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionInputEmpty() {
        transactionService.createTransaction(null);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountFromNull() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAccountFrom(null);

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountFromEmpty() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAccountFrom("");

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountFromNotFound() {
        TransactionCreationInput creationInput = getFullCreationInput();

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.empty());

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountToNull() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAccountTo(null);

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountToEmpty() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAccountTo("");

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountToNotFound() {
        TransactionCreationInput creationInput = getFullCreationInput();

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.empty());

        transactionService.createTransaction(creationInput);
    }
    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAccountFromAndToTheSame() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAccountTo(creationInput.getAccountFrom());

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAmountNull() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAmount(null);

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAmountZero() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAmount(new BigDecimal(0));

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfAmountNegative() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setAmount(new BigDecimal(-1));

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfCurrencyNull() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setCurrency(null);

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createTransaction_exceptionIfCurrencyEmpty() {
        TransactionCreationInput creationInput = getFullCreationInput();
        creationInput.setCurrency("");

        when(accountService.findAccountById(creationInput.getAccountFrom())).thenReturn(Optional.of(new Account()));
        when(accountService.findAccountById(creationInput.getAccountTo())).thenReturn(Optional.of(new Account()));

        transactionService.createTransaction(creationInput);
    }

    @Test
    public void deleteTransactionById_noErrorIfFound() {
        String id = "id1";
        when(transactionRepository.delete(id)).thenReturn(true);

        transactionService.deleteTransactionById(id);
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteTransactionById_throwExceptionIfNotFound() {
        String id = "id1";
        when(transactionRepository.delete(id)).thenReturn(false);

        transactionService.deleteTransactionById(id);
    }

    private Transaction createTransaction(String id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        return transaction;
    }

    private TransactionCreationInput getFullCreationInput() {
        TransactionCreationInput creationInput = new TransactionCreationInput();
        creationInput.setAccountFrom("account1");
        creationInput.setAccountTo("account2");
        creationInput.setAmount(new BigDecimal(11111.20));
        creationInput.setCurrency("USD");
        creationInput.setCreatedBy("user1");
        return creationInput;
    }
}
