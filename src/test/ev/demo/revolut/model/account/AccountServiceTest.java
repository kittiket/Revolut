package ev.demo.revolut.model.account;

import ev.demo.revolut.db.exception.EntityNotFoundException;
import ev.demo.revolut.model.ValidationException;
import ev.demo.revolut.model.account.entity.Account;
import ev.demo.revolut.model.account.entity.AccountCreationInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Connection connection;

    @InjectMocks
    private AccountService accountService = new AccountService();


    @Test
    public void getAllAccounts_returnAll() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(createAccount("id1"));
        accounts.add(createAccount("id2"));

        when(accountRepository.findAll()).thenReturn(accounts);

        List<Account> accountsFromService = accountService.getAllAccounts();
        assertNotNull(accountsFromService);
        assertEquals(accountsFromService.size(), accounts.size());
        assertEquals(accountsFromService.get(0).getId(), accounts.get(0).getId());
        assertEquals(accountsFromService.get(1).getId(), accounts.get(1).getId());
    }

    @Test
    public void getAllAccounts_returnEmptyList() {
        when(accountRepository.findAll()).thenReturn(new ArrayList<>());

        List<Account> accountsFromService = accountService.getAllAccounts();
        assertNotNull(accountsFromService);
        assertTrue(accountsFromService.isEmpty());
    }

    @Test
    public void getAccountById_returnAccount() {
        String id = "id1";
        when(accountRepository.find(id)).thenReturn(Optional.of(createAccount(id)));

        Account accountFromService = accountService.getAccountById(id);
        assertNotNull(accountFromService);
        assertEquals(id, accountFromService.getId());
    }

    @Test(expected = EntityNotFoundException.class)
    public void getAccountById_throwExceptionIfNotFound() {
        String id = "id1";
        when(accountRepository.find(id)).thenReturn(Optional.empty());

        accountService.getAccountById(id);
    }

    @Test
    public void createAccount_returnAccount() {
        String id = "id1";

        AccountCreationInput creationInput = getFullCreationInput();

        when(accountRepository.insert(any())).thenAnswer(i -> {
            Account account = i.getArgument(0);
            account.setId(id);
            return account;
        });

        Account accountFromService = accountService.createAccount(creationInput);
        assertNotNull(accountFromService);
        assertEquals(id, accountFromService.getId());
        assertEquals(creationInput.getName(), accountFromService.getName());
        assertEquals(creationInput.getAmount(), accountFromService.getAmount());
        assertEquals(creationInput.getCurrency(), accountFromService.getCurrency());
        assertEquals(creationInput.getOwnerId(), accountFromService.getOwnerId());
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfCreationInputEmpty() {
        accountService.createAccount(null);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfCurrencyNull() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setCurrency(null);

        accountService.createAccount(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfCurrencyEmpty() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setCurrency("");

        accountService.createAccount(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfOwnerNull() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setOwnerId(null);

        accountService.createAccount(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfOwnerEmpty() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setOwnerId("");

        accountService.createAccount(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfAmountNull() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setAmount(null);

        accountService.createAccount(creationInput);
    }

    @Test (expected = ValidationException.class)
    public void createAccount_exceptionIfAmountNegative() {
        AccountCreationInput creationInput = getFullCreationInput();
        creationInput.setAmount(new BigDecimal(-100));

        accountService.createAccount(creationInput);
    }

    @Test
    public void deleteAccountById_noErrorIfFound() {
        String id = "id1";
        when(accountRepository.delete(id)).thenReturn(true);

        accountService.deleteAccountById(id);
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteAccountById_throwExceptionIfNotFound() {
        String id = "id1";
        when(accountRepository.delete(id)).thenReturn(false);

        accountService.deleteAccountById(id);
    }

    @Test(expected = ValidationException.class)
    public void transferAmount_throwExceptionIfAmountBigger(){
        Account account1 = createAccount("id1", 100);
        Account account2 = createAccount("id2", 200);
        when(accountRepository.findForUpdate(connection, account1.getId())).thenReturn(Optional.of(account1));
        when(accountRepository.findForUpdate(connection, account2.getId())).thenReturn(Optional.of(account2));

        accountService.transferAmount(connection, account1.getId(), account2.getId(), new BigDecimal(300), new BigDecimal(300));
    }

    @Test
    public void transferAmount_amountsChanged(){
        Account account1 = createAccount("id1", 100);
        Account account2 = createAccount("id2", 200);
        when(accountRepository.findForUpdate(connection, account1.getId())).thenReturn(Optional.of(account1));
        when(accountRepository.findForUpdate(connection, account2.getId())).thenReturn(Optional.of(account2));

        when(accountRepository.update(connection, account1)).thenAnswer(i -> {
            Account account1AfterUpdate = i.getArgument(1);
            assertEquals(new BigDecimal(80), account1AfterUpdate.getAmount());
            return account1AfterUpdate;
        });
        when(accountRepository.update(connection, account2)).thenAnswer(i -> {
            Account account2AfterUpdate = i.getArgument(1);
            assertEquals(new BigDecimal(250), account2AfterUpdate.getAmount());
            return account2AfterUpdate;
        });

        accountService.transferAmount(connection, account1.getId(), account2.getId(), new BigDecimal(20), new BigDecimal(50));
    }

    private Account createAccount(String id) {
        Account account = new Account();
        account.setId(id);
        return account;
    }

    private Account createAccount(String id, int amount) {
        Account account = new Account();
        account.setId(id);
        account.setAmount(new BigDecimal(amount));
        return account;
    }

    private AccountCreationInput getFullCreationInput() {
        AccountCreationInput creationInput = new AccountCreationInput();
        creationInput.setName("name1");
        creationInput.setAmount(new BigDecimal(11111.11));
        creationInput.setCurrency("USD");
        creationInput.setOwnerId("user1");
        return creationInput;
    }
}
