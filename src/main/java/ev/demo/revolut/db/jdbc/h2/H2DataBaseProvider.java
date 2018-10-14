package ev.demo.revolut.db.jdbc.h2;

import ev.demo.revolut.db.exception.DataBaseRuntimeException;
import ev.demo.revolut.db.jdbc.JdbcRepository;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2DataBaseProvider {

    private static Logger logger = Logger.getLogger(JdbcRepository.class);


    public Connection getConnection() {
        try {
            return DriverManager.getConnection ("jdbc:h2:mem:revolut;IFEXISTS=TRUE;", "","");

        } catch (SQLException e) {
            logger.error("Failed to get DB connection!", e);
            throw new DataBaseRuntimeException(e);
        }
    }

    public static void init() {

        Connection connection = null;
        try {
            connection = DriverManager.getConnection ("jdbc:h2:mem:revolut;DB_CLOSE_DELAY=-1;", "","");
            QueryRunner queryRunner = new QueryRunner();

            String sqlQuery = "CREATE TABLE Account (" +
                    "id VARCHAR(255) NOT NULL PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "ownerId VARCHAR(255) NOT NULL," +
                    "amount DECIMAL(20, 2) NOT NULL," +
                    "currency VARCHAR(3) NOT NULL" +
                    "); ";

            sqlQuery += "CREATE TABLE Transaction (" +
                    "id VARCHAR(255) NOT NULL PRIMARY KEY," +
                    "accountFrom VARCHAR(255) NOT NULL," +
                    "accountTo VARCHAR(255) NOT NULL," +
                    "amount DECIMAL(20, 2) NOT NULL," +
                    "currency VARCHAR(3) NOT NULL," +
                    "status VARCHAR(255) NOT NULL," +
                    "error VARCHAR(1000)," +
                    "createdBy VARCHAR(255)," +
                    "createdAt TIMESTAMP NOT NULL," +
                    "expiredAt TIMESTAMP NOT NULL," +
                    "FOREIGN KEY (accountFrom) REFERENCES Account(id)," +
                    "FOREIGN KEY (accountTo) REFERENCES Account(id)" +
                    ");";

            queryRunner.update(connection, sqlQuery);

            logger.info("DB Schema created successfully!");
        } catch (Exception e){
            logger.error("Failed to init DB Schema!", e);
            throw new DataBaseRuntimeException("Failed to init DB Schema!", e);

        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                logger.error("Failed to close closeable: " + connection + "!", e);
            }
        }
    }
}
