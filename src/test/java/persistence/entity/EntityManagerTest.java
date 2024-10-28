package persistence.entity;

import database.DatabaseServer;
import database.H2;
import domain.Order;
import domain.OrderItem;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jdbc.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.H2Dialect;
import persistence.sql.ddl.query.CreateTableQueryBuilder;
import persistence.sql.ddl.query.DropQueryBuilder;
import persistence.sql.definition.TableDefinition;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityManagerTest {

    @Entity
    private static class EntityManagerTestEntityWithIdentityId {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        private Integer age;

        public EntityManagerTestEntityWithIdentityId() {
        }

        public EntityManagerTestEntityWithIdentityId(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public EntityManagerTestEntityWithIdentityId(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    private static DatabaseServer server;
    private static JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws SQLException {
        server = new H2();
        server.start();

        String query = new CreateTableQueryBuilder(new H2Dialect(), EntityManagerTestEntityWithIdentityId.class, null).build();
        String query2 = new CreateTableQueryBuilder(new H2Dialect(), Order.class, null).build();

        TableDefinition tableDefinition = new TableDefinition(Order.class);

        String query3 = new CreateTableQueryBuilder(new H2Dialect(), OrderItem.class, Order.class).build();

        jdbcTemplate = new JdbcTemplate(server.getConnection());
        jdbcTemplate.execute(query);
        jdbcTemplate.execute(query2);
        jdbcTemplate.execute(query3);
    }

    @AfterEach
    void tearDown() throws SQLException {
        String query = new DropQueryBuilder(EntityManagerTestEntityWithIdentityId.class).build();
        String query2 = new DropQueryBuilder(Order.class).build();
        String query3 = new DropQueryBuilder(OrderItem.class).build();

        jdbcTemplate = new JdbcTemplate(server.getConnection());
        jdbcTemplate.execute(query);
        jdbcTemplate.execute(query2);
        jdbcTemplate.execute(query3);
        server.stop();
    }

    @Test
    @DisplayName("Identity 전략을 사용하는 엔티티를 저장한다.")
    void testPersistWithIdentityId() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId(null, "john_doe", 30);
        entityManager.persist(entity);

        EntityManagerTestEntityWithIdentityId persistedEntity = entityManager.find(EntityManagerTestEntityWithIdentityId.class, 1L);
        assertAll(
                () -> assertThat(persistedEntity.id).isEqualTo(1L),
                () -> assertThat(persistedEntity.name).isEqualTo("john_doe"),
                () -> assertThat(persistedEntity.age).isEqualTo(30)
        );
    }

    @Test
    @DisplayName("Identity 전략을 사용하지만, id값이 있는 경우 에러가 발생한다.")
    void testPersistWithIdentityIdButId() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId(1L, "john_doe", 30);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> entityManager.persist(entity));
        assertThat(e.getMessage()).isEqualTo("No Entity Entry with id: 1");
    }

    @Test
    @DisplayName("같은 엔티티에대해 저장이 여러번 호출되면 예외가 발생하지 않는다.")
    void testPersistManyTimes() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId(null, "john_doe", 30);

        entityManager.persist(entity);
        entityManager.persist(entity);

        EntityManagerTestEntityWithIdentityId persistedEntity = entityManager.find(EntityManagerTestEntityWithIdentityId.class, 1L);
        assertAll(
                () -> assertThat(persistedEntity.id).isEqualTo(1L),
                () -> assertThat(persistedEntity.name).isEqualTo("john_doe"),
                () -> assertThat(persistedEntity.age).isEqualTo(30)
        );
    }

    @Test
    @DisplayName("EntityManager.update()를 통해 엔티티를 수정한다.")
    void testMerge() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId("john_doe", 30);
        entityManager.persist(entity);

        entity.name = "jane_doe";
        entity.age = 40;

        entityManager.merge(entity);

        EntityManagerTestEntityWithIdentityId updated = entityManager.find(EntityManagerTestEntityWithIdentityId.class, 1L);

        assertAll(
                () -> assertThat(updated.id).isEqualTo(1L),
                () -> assertThat(updated.name).isEqualTo("jane_doe"),
                () -> assertThat(updated.age).isEqualTo(40)
        );
    }

    @Test
    @DisplayName("관리되고 있지 않은 엔티티를 EntityManager.merge()를 호출 하면 예외가 발생한다.")
    void testMergeNotManagedEntity() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId(1L, "john_doe", 30);

        entity.name = "jane_doe";
        entity.age = 40;

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> entityManager.merge(entity));
        assertThat(e.getMessage()).isEqualTo("Can not find entity in persistence context: EntityManagerTestEntityWithIdentityId");
    }

    @Test
    @DisplayName("EntityManager.remove()를 통해 엔티티를 삭제한다.")
    void testRemove() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        EntityManagerTestEntityWithIdentityId entity = new EntityManagerTestEntityWithIdentityId(null, "john_doe", 30);
        entityManager.persist(entity);

        entityManager.remove(entity);

        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> entityManager.find(EntityManagerTestEntityWithIdentityId.class, 1L)
        );
        assertThat(e.getMessage()).isEqualTo("Entity is not managed: EntityManagerTestEntityWithIdentityId");
    }

    @Test
    @DisplayName("Entity Manager Select without Join")
    void testSelectWithoutJoin() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        Order order = new Order("order_number");
        entityManager.persist(order);

        Order persistedOrder = entityManager.find(Order.class, 1L);
        assertAll(
                () -> assertThat(persistedOrder.getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderNumber()).isEqualTo("order_number"),
                () -> assertThat(persistedOrder.getOrderItems()).isEmpty()
        );
    }

    @Test
    @DisplayName("Entity Manager Select with Join")
    void testSelectWithJoin() throws Exception {
        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        Order order = new Order("order_number");
        OrderItem orderItem1 = new OrderItem("product1", 1);
        OrderItem orderItem2 = new OrderItem("product2", 2);

        entityManager.persist(order);
        entityManager.persist(orderItem1);
        entityManager.persist(orderItem2);

        order.getOrderItems().add(orderItem1);
        order.getOrderItems().add(orderItem2);

        entityManager.merge(order);
        Order persistedOrder = entityManager.find(Order.class, 1L);

        assertAll(
                () -> assertThat(persistedOrder.getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderNumber()).isEqualTo("order_number"),
                () -> assertThat(persistedOrder.getOrderItems()).hasSize(2),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getProduct()).isEqualTo("product1"),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getQuantity()).isEqualTo(1),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getId()).isEqualTo(2L),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getProduct()).isEqualTo("product2"),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getQuantity()).isEqualTo(2)
        );
    }

    @Test
    @DisplayName("Insert 시 연관 테이블이 없으면 Insert되지 않는다.")
    void testInsertWithoutAssociationTable() throws SQLException {

        EntityManager entityManager = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        Order order = new Order("order_number");
        entityManager.persist(order);

        Order persistedOrder = entityManager.find(Order.class, 1L);
        assertAll(
                () -> assertThat(persistedOrder.getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderNumber()).isEqualTo("order_number"),
                () -> assertThat(persistedOrder.getOrderItems()).isEmpty()
        );
    }

    @Test
    @DisplayName("Insert 시 연관 테이블까지 Insert 되어야 한다.")
    void testInsertWithAssociationTable() throws SQLException {
        EntityManager em = new EntityManagerImpl(new JdbcTemplate(server.getConnection()), new PersistenceContextImpl());
        Order order = new Order("order_number");
        OrderItem orderItem1 = new OrderItem("product1", 1);
        OrderItem orderItem2 = new OrderItem("product2", 2);


        order.getOrderItems().add(orderItem1);
        order.getOrderItems().add(orderItem2);

        em.persist(order);
        em.clear();

//        final Connection connection = server.getConnection();
//        ResultSet resultSet = connection.prepareStatement("SELECT orders.id, orders.orderNumber, order_items.id, order_items.product, order_items.quantity FROM orders LEFT JOIN order_items ON order_items.order_id = orders.id;")
//                .executeQuery();
//        printAllRowsAndColumns(resultSet);


//        jdbcTemplate.execute("SELECT orders.id, orders.orderNumber, order_items.id, order_items.product, order_items.quantity FROM orders LEFT JOIN order_items ON order_items.order_id = orders.id;");
        Order persistedOrder = em.find(Order.class, 1L);
//
        assertAll(
                () -> assertThat(persistedOrder.getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderNumber()).isEqualTo("order_number"),
                () -> assertThat(persistedOrder.getOrderItems()).hasSize(2),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getId()).isEqualTo(1L),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getProduct()).isEqualTo("product1"),
                () -> assertThat(persistedOrder.getOrderItems().get(0).getQuantity()).isEqualTo(1),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getId()).isEqualTo(2L),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getProduct()).isEqualTo("product2"),
                () -> assertThat(persistedOrder.getOrderItems().get(1).getQuantity()).isEqualTo(2)
        );
    }

    public static void printAllRowsAndColumns(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        int rowIndex = 0;
        while (resultSet.next()) {
            System.out.println("Row " + (++rowIndex) + ":");
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                System.out.printf("  %s: %s%n", columnName, value);
            }
        }
    }

}
