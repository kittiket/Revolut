package ev.demo.revolut.db.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class ResultSetOneHandler<T> implements ResultSetHandler<T> {
    private List<DataBaseField<T>> dataBaseFields;
    private Class<T> clazz;

    ResultSetOneHandler(List<DataBaseField<T>> dataBaseFields, Class<T> clazz) {
        this.dataBaseFields = dataBaseFields;
        this.clazz = clazz;
    }

    @Override
    public T handle(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        T entity;
        try {
            entity = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (DataBaseField<T> dataBaseField : dataBaseFields) {
            dataBaseField.getValueToEntity().accept(resultSet.getObject(dataBaseField.getDbFieldName()), entity);
        }
        return entity;
    }
}
