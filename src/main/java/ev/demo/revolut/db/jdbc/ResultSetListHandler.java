package ev.demo.revolut.db.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class ResultSetListHandler<T> implements ResultSetHandler<List<T>> {
    private List<DataBaseField<T>> dataBaseFields;
    private Class<T> clazz;

    ResultSetListHandler(List<DataBaseField<T>> dataBaseFields, Class<T> clazz) {
        this.dataBaseFields = dataBaseFields;
        this.clazz = clazz;
    }

    @Override
    public List<T> handle(ResultSet resultSet) throws SQLException {
        List<T> resultList = new ArrayList<>();

        while (resultSet.next()) {
            resultList.add(handleOne(resultSet));
        }

        return resultList;
    }

    private T handleOne(ResultSet resultSet) throws SQLException {
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
