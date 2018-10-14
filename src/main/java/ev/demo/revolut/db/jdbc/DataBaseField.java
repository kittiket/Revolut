package ev.demo.revolut.db.jdbc;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class DataBaseField<T> {

    private String dbFieldName;
    private Function<T, Object> valueFromEntity;
    private BiConsumer<Object, T> valueToEntity;
    private boolean isIdField = false;

    public DataBaseField(String dbFieldName, Function<T, Object> valueFromEntity, BiConsumer<Object, T> valueToEntity) {
        this.dbFieldName = dbFieldName;
        this.valueFromEntity = valueFromEntity;
        this.valueToEntity = valueToEntity;
    }

    public DataBaseField(String dbFieldName, Function<T, Object> valueFromEntity, BiConsumer<Object, T> valueToEntity, boolean isIdField) {
        this.dbFieldName = dbFieldName;
        this.valueFromEntity = valueFromEntity;
        this.valueToEntity = valueToEntity;
        this.isIdField = isIdField;
    }

    public String getDbFieldName() {
        return dbFieldName;
    }

    Function<T, Object> getValueFromEntity() {
        return valueFromEntity;
    }

    BiConsumer<Object, T> getValueToEntity() {
        return valueToEntity;
    }

    boolean isIdField() {
        return isIdField;
    }
}
