package ev.demo.revolut.db.jdbc;

import ev.demo.revolut.db.jdbc.h2.H2DataBaseProvider;
import ev.demo.revolut.db.exception.DataBaseRuntimeException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class JdbcRepository<T> {
    private static Logger logger = Logger.getLogger(JdbcRepository.class);
    
    private H2DataBaseProvider dataBaseProvider = new H2DataBaseProvider();

    protected abstract Class<T> getEntityClass();
    protected abstract String getTableName();
    protected abstract DataBaseField<T> getIdField();
    protected abstract List<DataBaseField<T>> getAllFields();

    protected <F> F runInNewConnection(Function<Connection, F> function) {
        try(Connection connection = dataBaseProvider.getConnection()) {

            return function.apply(connection);

        } catch (SQLException e) {
            throw new DataBaseRuntimeException(e);
        }
    }

    protected <F> F runInNewTransaction(Function<Connection, F> function) {
        try (Connection connection = dataBaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try{
                F result = function.apply(connection);
                connection.commit();

                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataBaseRuntimeException(e);
        }
    }

    public void runInNewTransaction(Consumer<Connection> consumer) {
        try (Connection connection = dataBaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try{
                consumer.accept(connection);
                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataBaseRuntimeException(e);
        }
    }

    protected List<T> selectAll(Connection connection) {
        return selectList(connection, "SELECT * FROM " + getTableName());
    }

    protected List<T> selectList(Connection connection, WhereItem... whereItems) {
        String sqlQuery = "SELECT * FROM " + getTableName() + generateWhere(whereItems);
        return selectList(connection, sqlQuery, getParameters(whereItems));
    }

    private List<T> selectList(Connection connection, String sqlQuery, Object... parameters) {
        logger.debug("Execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'");

        QueryRunner queryRunner = new QueryRunner();

        try {
            return queryRunner.query(connection, sqlQuery,  new ResultSetListHandler<>(getAllFields(), getEntityClass()), parameters);

        } catch (SQLException e) {
            logger.error("Failed to execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'!", e);
            throw new DataBaseRuntimeException(e);
        }
    }

    protected T selectById(Connection connection, Object id) {
        return selectOne(connection, "SELECT * FROM " + getTableName() + " WHERE id = ?", id);
    }

    protected T selectByIdForUpdate(Connection connection, Object id) {
        return selectOne(connection, "SELECT * FROM " + getTableName() + " WHERE id = ? FOR UPDATE", id);
    }

    private T selectOne(Connection connection, String sqlQuery, Object... parameters) {
        logger.debug("Execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'");

        QueryRunner queryRunner = new QueryRunner();

        try {
            return queryRunner.query(connection, sqlQuery, new ResultSetOneHandler<>(getAllFields(), getEntityClass()), parameters);

        } catch (SQLException e) {
            logger.error("Failed to execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'!", e);
            throw new DataBaseRuntimeException(e);
        }
    }

    protected T insert(Connection connection, T entity) {
        String newId = UUID.randomUUID().toString();

        List<String> fieldNames = getNoIdFieldsNames();
        List<Object> parameters = getNoIdFieldsValues(entity);

        fieldNames.add(getIdField().getDbFieldName());
        parameters.add(newId);

        String sqlQuery = "INSERT INTO " + getTableName() + " (" + listToString(fieldNames) + ") VALUES (" +  getQuestionMarks(parameters.size()) + ")";

        update(connection, sqlQuery, parameters.toArray());
        return selectById(connection, newId);
    }

    protected T save(Connection connection, T entity) {
        Object id = getIdField().getValueFromEntity().apply(entity);
        List<Object> parameters = getNoIdFieldsValues(entity);
        parameters.add(id);

        String sqlQuery = "UPDATE " + getTableName() + " SET ";
        sqlQuery += getNoIdFieldsNames().stream().map((field) -> field + " = ?").collect(Collectors.joining(", "));
        sqlQuery += " WHERE id = ?";

        update(connection, sqlQuery, parameters.toArray());
        return selectById(connection, id);
    }

    protected boolean deleteById(Connection connection, Object id) {
        String sqlQuery = "DELETE FROM " + getTableName() + " WHERE " + getIdField().getDbFieldName() + " = ?";
        return update(connection, sqlQuery, id) == 1;
    }

    private int update(Connection connection, String sqlQuery, Object... parameters) {
        logger.debug("Execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'");

        QueryRunner queryRunner = new QueryRunner();

        try {
            return queryRunner.update(connection, sqlQuery, parameters);

        } catch (SQLException e) {
            logger.error("Failed to execute sqlQuery '" + sqlQuery + "' with parameters '" + listToString(parameters) + "'!", e);
            throw new DataBaseRuntimeException(e);
        }
    }


    private String generateWhere(WhereItem... whereItems) {
        if (whereItems == null || whereItems.length == 0) {
            return "";
        }

        String whereSql = " WHERE ";
        whereSql += Arrays.stream(whereItems).map(whereItem -> {
            if (whereItem.getOrParameters().size() == 1) {
                return whereItem.getFieldName() + " = ?";
            } else {
                return whereItem.getFieldName() + " IN (" + getQuestionMarks(whereItem.getOrParameters().size()) + ")";
            }
        }).collect(Collectors.joining(" AND "));

        return whereSql;
    }

    private Object[] getParameters(WhereItem... whereItems) {
        if (whereItems == null || whereItems.length == 0) {
            return new Object[0];
        }

        List<Object> parameters = new ArrayList<>();
        for (WhereItem whereItem : whereItems) {
            parameters.addAll(whereItem.getOrParameters());
        }

        return parameters.toArray();
    }


    private List<String> getNoIdFieldsNames() {
        return getAllFields().stream().filter(field -> !field.isIdField()).map(DataBaseField::getDbFieldName).collect(Collectors.toList());
    }

    private List<Object> getNoIdFieldsValues(T entity) {
        return getAllFields().stream().filter(field -> !field.isIdField()).map(dbField -> dbField.getValueFromEntity().apply(entity)).collect(Collectors.toList());
    }

    private String listToString(Iterable values) {
        return StringUtils.join(values, ", ");
    }
    private String listToString(Object[] values) {
        return StringUtils.join(values, ", ");
    }

    private String getQuestionMarks(int size) {
        String[] questionMarks = new String[size];
        Arrays.fill(questionMarks, "?");
        return listToString(questionMarks);
    }
}
