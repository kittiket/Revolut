package ev.demo.revolut.model.transaction;

import ev.demo.revolut.db.DataBaseRepository;
import ev.demo.revolut.db.jdbc.DataBaseField;
import ev.demo.revolut.db.jdbc.WhereItem;
import ev.demo.revolut.db.jdbc.JdbcRepository;
import ev.demo.revolut.model.transaction.entity.Transaction;
import ev.demo.revolut.model.transaction.entity.TransactionStatus;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;

public class TransactionRepository extends JdbcRepository<Transaction> implements DataBaseRepository<Transaction> {

    private static final String TABLE_NAME = "Transaction";

    private static final DataBaseField<Transaction> ID = new DataBaseField<>("id", Transaction::getId, (dbValue, entity) -> entity.setId((String)dbValue), true);
    private static final DataBaseField<Transaction> ACCOUNT_FROM = new DataBaseField<>("accountFrom", Transaction::getAccountFrom, (dbValue, entity) -> entity.setAccountFrom((String)dbValue));
    private static final DataBaseField<Transaction> ACCOUNT_TO = new DataBaseField<>("accountTo", Transaction::getAccountTo, (dbValue, entity) -> entity.setAccountTo((String)dbValue));
    private static final DataBaseField<Transaction> AMOUNT = new DataBaseField<>("amount", Transaction::getAmount, (dbValue, entity) -> entity.setAmount((BigDecimal)dbValue));
    private static final DataBaseField<Transaction> CURRENCY = new DataBaseField<>("currency", Transaction::getCurrency, (dbValue, entity) -> entity.setCurrency((String)dbValue));
    private static final DataBaseField<Transaction> STATUS = new DataBaseField<>("status", entity -> entity.getStatus().name(), (dbValue, entity) -> entity.setStatus(TransactionStatus.valueOf((String)dbValue)));
    private static final DataBaseField<Transaction> ERROR = new DataBaseField<>("error", entity -> StringUtils.left(entity.getError(), 1000), (dbValue, entity) -> entity.setError((String)dbValue));
    private static final DataBaseField<Transaction> CREATED_BY = new DataBaseField<>("createdBy", Transaction::getCreatedBy, (dbValue, entity) -> entity.setCreatedBy((String)dbValue));
    private static final DataBaseField<Transaction> CREATED_AT = new DataBaseField<>("createdAt", (entity) -> Timestamp.from(entity.getCreatedAt()), (dbValue, entity) -> entity.setCreatedAt(((Timestamp) dbValue).toInstant()));
    private static final DataBaseField<Transaction> EXPIRED_AT = new DataBaseField<>("expiredAt", (entity) -> Timestamp.from(entity.getExpiredAt()), (dbValue, entity) -> entity.setExpiredAt(((Timestamp) dbValue).toInstant()));

    @SuppressWarnings("unchecked")
    private static List<DataBaseField<Transaction>> fields = Collections.unmodifiableList(Lists.newArrayList(
            ID, ACCOUNT_FROM, ACCOUNT_TO, AMOUNT, CURRENCY, STATUS, ERROR, CREATED_BY, CREATED_AT, EXPIRED_AT
    ));


    @Override
    protected Class<Transaction> getEntityClass() {
        return Transaction.class;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected DataBaseField<Transaction> getIdField() {
        return ID;
    }

    @Override
    protected List<DataBaseField<Transaction>> getAllFields() {
        return fields;
    }

    @Override
    public Optional<Transaction> find(String transactionId) {
        return Optional.ofNullable(runInNewConnection((Connection connection) -> selectById(connection, transactionId)));
    }

    @Override
    public List<Transaction> findAll() {
        return runInNewConnection(this::selectAll);
    }

    List<Transaction> findAllNewOrInProgress() {
        WhereItem whereItem = new WhereItem(STATUS.getDbFieldName(), TransactionStatus.NEW.name(), TransactionStatus.IN_PROGRESS.name());
        return runInNewConnection((Connection connection) -> selectList(connection, whereItem));
    }

    @Override
    public Transaction insert(Transaction transaction) {
        return runInNewTransaction((Connection connection) -> insert(connection, transaction));
    }

    @Override
    public Transaction update(Transaction transaction) {
        return runInNewTransaction((Connection connection) -> save(connection, transaction));
    }

    void update(Connection connection, Transaction transaction) {
        save(connection, transaction);
    }


    Optional<Transaction> lockAndTryChange(String transactionId, Function<Transaction, Boolean> changeAction) {
        return runInNewTransaction((Connection connection) -> lockAndTryChange(connection, transactionId, changeAction));
    }

    private Optional<Transaction> lockAndTryChange(Connection connection, String transactionId, Function<Transaction, Boolean> changeAction) {
        Transaction transaction = selectByIdForUpdate(connection, transactionId);
        if (transaction != null) {
            boolean wasChanged = changeAction.apply(transaction);
            if (wasChanged) {
                return Optional.of(save(connection, transaction));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean delete(String transactionId) {
        return runInNewTransaction((Connection connection) -> deleteById(connection, transactionId));
    }
}
