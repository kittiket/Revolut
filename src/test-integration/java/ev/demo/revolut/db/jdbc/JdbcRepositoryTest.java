package ev.demo.revolut.db.jdbc;

import ev.demo.revolut.db.exception.DataBaseRuntimeException;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class JdbcRepositoryTest {

    private TestDbRepository testDbRepository = new TestDbRepository();

    static {
        org.apache.log4j.BasicConfigurator.configure();

        try (Connection connection = DriverManager.getConnection ("jdbc:h2:mem:revolut;DB_CLOSE_DELAY=-1;", "","")){

            QueryRunner queryRunner = new QueryRunner();

            String sqlQuery = "CREATE TABLE Test (" +
                    "id VARCHAR(255) NOT NULL PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "amount DECIMAL(20, 2) NOT NULL," +
                    "time TIMESTAMP NOT NULL," +
                    "); ";

            queryRunner.update(connection, sqlQuery);

        } catch (Exception e){
            throw new DataBaseRuntimeException("Failed to init DB Schema!", e);
        }
    }

    @Test
    public void insert_returnEntity() {
        TestDbEntity testDbEntity = createTestDbEntity();

        TestDbEntity createdEntity = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity);
        assertNotNull(createdEntity.getId());
        assertEquals(testDbEntity.getName(), createdEntity.getName());
        assertEquals(testDbEntity.getAmount(), createdEntity.getAmount());
        assertEquals(testDbEntity.getTime(), createdEntity.getTime());
    }

    @Test
    public void selectById_selectInsertedEntity() {
        TestDbEntity testDbEntity = createTestDbEntity();

        TestDbEntity createdEntity = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity.getId());

        TestDbEntity selectedEntity = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectById(connection, createdEntity.getId()));
        assertNotNull(selectedEntity);
        assertEquals(createdEntity.getId(), selectedEntity.getId());
        assertEquals(testDbEntity.getName(), selectedEntity.getName());
        assertEquals(testDbEntity.getAmount(), selectedEntity.getAmount());
        assertEquals(testDbEntity.getTime(), selectedEntity.getTime());
    }

    @Test
    public void selectAll_selectInserted() {
        TestDbEntity testDbEntity = createTestDbEntity();

        TestDbEntity createdEntity1 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity1.getId());
        TestDbEntity createdEntity2 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity2.getId());
        TestDbEntity createdEntity3 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity3.getId());

        List<TestDbEntity> selectedEntities = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectAll(connection));
        assertNotNull(selectedEntities);
        assertTrue(selectedEntities.size() >= 3);
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity1.getId().equals(entity.getId())));
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity2.getId().equals(entity.getId())));
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity3.getId().equals(entity.getId())));
    }

    @Test
    public void selectList_findByOrParameters() {
        TestDbEntity testDbEntity1 = createTestDbEntity();

        TestDbEntity createdEntity1 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity1));
        assertNotNull(createdEntity1.getId());

        TestDbEntity testDbEntity2 = createTestDbEntity();
        testDbEntity2.setName("specialName2");

        TestDbEntity createdEntity2 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity2));
        assertNotNull(createdEntity2.getId());

        TestDbEntity testDbEntity3 = createTestDbEntity();
        testDbEntity3.setName("specialName3");
        TestDbEntity createdEntity3 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity3));
        assertNotNull(createdEntity3.getId());

        WhereItem whereItem = new WhereItem(TestDbRepository.NAME.getDbFieldName(), testDbEntity2.getName(), testDbEntity3.getName());

        List<TestDbEntity> selectedEntities = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectList(connection, whereItem));
        assertNotNull(selectedEntities);
        assertEquals(2, selectedEntities.size());
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity2.getId().equals(entity.getId())));
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity3.getId().equals(entity.getId())));
    }

    @Test
    public void selectList_findByAndParameters() {
        String name = "specialName";

        TestDbEntity testDbEntity1 = createTestDbEntity();
        testDbEntity1.setName(name);
        testDbEntity1.setAmount(new BigDecimal(100));

        TestDbEntity createdEntity1 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity1));
        assertNotNull(createdEntity1.getId());

        TestDbEntity testDbEntity2 = createTestDbEntity();
        testDbEntity2.setName(name);
        testDbEntity2.setAmount(new BigDecimal(200));

        TestDbEntity createdEntity2 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity2));
        assertNotNull(createdEntity2.getId());

        TestDbEntity testDbEntity3 = createTestDbEntity();
        testDbEntity3.setName(name);
        testDbEntity3.setAmount(new BigDecimal(200));

        TestDbEntity createdEntity3 = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity3));
        assertNotNull(createdEntity3.getId());

        WhereItem whereItemName = new WhereItem(TestDbRepository.NAME.getDbFieldName(), name);
        WhereItem whereItemAmount = new WhereItem(TestDbRepository.AMOUNT.getDbFieldName(), 200);

        List<TestDbEntity> selectedEntities = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectList(connection, whereItemName, whereItemAmount));
        assertNotNull(selectedEntities);
        assertEquals(2, selectedEntities.size());
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity2.getId().equals(entity.getId())));
        assertTrue(selectedEntities.stream().anyMatch(entity -> createdEntity3.getId().equals(entity.getId())));
    }

    @Test
    public void selectByIdForUpdate_newUpdateAppliedAfterPreviousSelectForUpdateCompleted() {
        String newName1 = "newName1";
        String newName2 = "newName2";

        TestDbEntity testDbEntity = createTestDbEntity();

        TestDbEntity createdEntity = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity.getId());

        testDbRepository.runInNewTransaction((Connection connection1) -> {
            TestDbEntity selectForUpdateEntity = testDbRepository.selectByIdForUpdate(connection1, createdEntity.getId());
            selectForUpdateEntity.setName(newName1);
            testDbRepository.save(connection1, selectForUpdateEntity);

            Thread anotherUpdate = new Thread(() -> {
                testDbRepository.runInNewConnection((Connection connection2) -> {
                    TestDbEntity sameEntity = testDbRepository.selectById(connection2, createdEntity.getId());
                    sameEntity.setName(newName2);
                    return testDbRepository.save(connection2, sameEntity);
                });
            });
            anotherUpdate.start();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TestDbEntity selectedInAnotherConnectionEntity = testDbRepository.runInNewConnection((Connection connection3) -> testDbRepository.selectById(connection3, createdEntity.getId()));
            TestDbEntity selectedInForUpdateConnectionEntity = testDbRepository.selectById(connection1, createdEntity.getId());
            assertEquals(createdEntity.getName(), selectedInAnotherConnectionEntity.getName());
            assertEquals(selectForUpdateEntity.getName(), selectedInForUpdateConnectionEntity.getName());
            return selectForUpdateEntity;
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TestDbEntity selectedAfterSecondUpdate = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectById(connection, createdEntity.getId()));
        assertEquals(newName2, selectedAfterSecondUpdate.getName());
    }

    @Test
    public void deleteById_deleteInsertedEntity() {
        TestDbEntity testDbEntity = createTestDbEntity();

        TestDbEntity createdEntity = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.insert(connection, testDbEntity));
        assertNotNull(createdEntity.getId());

        TestDbEntity selectedEntity = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectById(connection, createdEntity.getId()));
        assertNotNull(selectedEntity);

        Boolean wasDeleted = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.deleteById(connection, createdEntity.getId()));
        assertTrue(wasDeleted);

        TestDbEntity selectedAfterDeleteEntity = testDbRepository.runInNewConnection((Connection connection) -> testDbRepository.selectById(connection, createdEntity.getId()));
        assertNull(selectedAfterDeleteEntity);

        Boolean wasDeletedAfterDelete = testDbRepository.runInNewTransaction((Connection connection) -> testDbRepository.deleteById(connection, createdEntity.getId()));
        assertFalse(wasDeletedAfterDelete);
    }

    private TestDbEntity createTestDbEntity() {
        TestDbEntity testDbEntity = new TestDbEntity();
        testDbEntity.setName("name");
        testDbEntity.setAmount(new BigDecimal("100.10"));
        testDbEntity.setTime(Instant.now());
        return testDbEntity;
    }
}
