package ev.demo.revolut.db.jdbc;

import jersey.repackaged.com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class TestDbRepository extends JdbcRepository<TestDbEntity> {

    private static final String TABLE_NAME = "Test";

    static final DataBaseField<TestDbEntity> ID = new DataBaseField<>("id", TestDbEntity::getId, (dbValue, entity) -> entity.setId((String)dbValue), true);
    static final DataBaseField<TestDbEntity> NAME = new DataBaseField<>("name", TestDbEntity::getName, (dbValue, entity) -> entity.setName((String)dbValue));
    static final DataBaseField<TestDbEntity> AMOUNT = new DataBaseField<>("amount", TestDbEntity::getAmount, (dbValue, entity) -> entity.setAmount((BigDecimal)dbValue));
    static final DataBaseField<TestDbEntity> TIME = new DataBaseField<>("time", (entity) -> Timestamp.from(entity.getTime()), (dbValue, entity) -> entity.setTime(((Timestamp) dbValue).toInstant()));

    @SuppressWarnings("unchecked")
    private static List<DataBaseField<TestDbEntity>> fields = Collections.unmodifiableList(Lists.newArrayList(
            ID, NAME, AMOUNT, TIME
    ));

    @Override
    protected Class<TestDbEntity> getEntityClass() {
        return TestDbEntity.class;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected DataBaseField<TestDbEntity> getIdField() {
        return ID;
    }

    @Override
    protected List<DataBaseField<TestDbEntity>> getAllFields() {
        return fields;
    }
}
