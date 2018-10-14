package ev.demo.revolut.model.account;

import ev.demo.revolut.db.DataBaseRepository;
import ev.demo.revolut.db.jdbc.DataBaseField;
import ev.demo.revolut.db.jdbc.JdbcRepository;
import ev.demo.revolut.model.account.entity.Account;
import jersey.repackaged.com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

public class AccountRepository extends JdbcRepository<Account> implements DataBaseRepository<Account> {

    private static final String TABLE_NAME = "Account";

    private static final DataBaseField<Account> ID = new DataBaseField<>("id", Account::getId, (dbValue, entity) -> entity.setId((String)dbValue), true);
    private static final DataBaseField<Account> NAME = new DataBaseField<>("name", Account::getName, (dbValue, entity) -> entity.setName((String)dbValue));
    private static final DataBaseField<Account> OWNER_ID = new DataBaseField<>("ownerId", Account::getOwnerId, (dbValue, entity) -> entity.setOwnerId((String)dbValue));
    private static final DataBaseField<Account> AMOUNT = new DataBaseField<>("amount", Account::getAmount, (dbValue, entity) -> entity.setAmount((BigDecimal)dbValue));
    private static final DataBaseField<Account> CURRENCY = new DataBaseField<>("currency", Account::getCurrency, (dbValue, entity) -> entity.setCurrency((String)dbValue));

    @SuppressWarnings("unchecked")
    private static List<DataBaseField<Account>> fields = Collections.unmodifiableList(Lists.newArrayList(
            ID, NAME, OWNER_ID, AMOUNT, CURRENCY
    ));

    @Override
    protected Class<Account> getEntityClass() {
        return Account.class;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected DataBaseField<Account> getIdField() {
        return ID;
    }

    @Override
    protected List<DataBaseField<Account>> getAllFields() {
        return fields;
    }


    @Override
    public Optional<Account> find(String accountId){
        return Optional.ofNullable(runInNewConnection((Connection connection) -> selectById(connection, accountId)));
    }

    Optional<Account> findForUpdate(Connection connection, String id) {
        return Optional.ofNullable(selectByIdForUpdate(connection, id));
    }

    @Override
    public List<Account> findAll(){
        return runInNewConnection(this::selectAll);
    }

    @Override
    public Account insert(Account account) {
        return runInNewTransaction((Connection connection) -> insert(connection, account));
    }

    @Override
    public Account update(Account account) {
        return runInNewTransaction((Connection connection) -> save(connection, account));
    }

    Account update(Connection connection, Account account) {
        return save(connection, account);
    }

    @Override
    public boolean delete(String accountId) {
        return runInNewTransaction((Connection connection) -> deleteById(connection, accountId));
    }
}
