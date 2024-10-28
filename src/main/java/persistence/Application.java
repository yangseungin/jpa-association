package persistence;

import database.DatabaseServer;
import database.H2;
import domain.Order;
import domain.OrderItem;
import jdbc.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.entity.EntityManager;
import persistence.entity.EntityManagerImpl;
import persistence.entity.EntityRowMapper;
import persistence.entity.PersistenceContextImpl;
import persistence.sql.H2Dialect;
import domain.Person;
import persistence.sql.ddl.query.CreateTableQueryBuilder;
import persistence.sql.ddl.query.DropQueryBuilder;
import persistence.sql.definition.TableDefinition;
import persistence.sql.dml.query.SelectAllQueryBuilder;

import java.util.List;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting application...");
        try {
            final DatabaseServer server = new H2();
            final Class<?> testClass = Person.class;
            server.start();

            final JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
            final EntityManager em = new EntityManagerImpl(jdbcTemplate, new PersistenceContextImpl());

            CreateTableQueryBuilder createOrder = new CreateTableQueryBuilder(new H2Dialect(), Order.class, null);
            CreateTableQueryBuilder createOrderItem = new CreateTableQueryBuilder(new H2Dialect(), OrderItem.class, Order.class);
            jdbcTemplate.execute(createOrder.build());
            jdbcTemplate.execute(createOrderItem.build());

            Order order = new Order("123");
            OrderItem orderItem = new OrderItem("item1", 1);
            OrderItem orderItem2 = new OrderItem("item2", 2);
            order.getOrderItems().add(orderItem);
            order.getOrderItems().add(orderItem2);

            em.persist(order);
            em.clear();

            em.find(Order.class, 1L);
            server.stop();
        } catch (Exception e) {
            logger.error("Error occurred", e);
        } finally {
            logger.info("Application finished");
        }
    }

    private static void drop(JdbcTemplate jdbcTemplate) {
        DropQueryBuilder dropQuery = new DropQueryBuilder(Person.class);
        String build = dropQuery.build();
        logger.info("Drop query: {}", build);
        jdbcTemplate.execute(build);
    }

    private static void selectAll(JdbcTemplate jdbcTemplate, Class<?> testClass) {
        String query = new SelectAllQueryBuilder().build(testClass);
        List<Person> people = jdbcTemplate.query(query, new EntityRowMapper<>(Person.class));

        for (Person person : people) {
            logger.info("Person: {}", person);
        }
    }

    private static void select(EntityManager em, Object id) {
        Person person = em.find(Person.class, id);
        logger.info("Person: {}", person);
    }

    private static void insert(EntityManager em, Person person) {
        em.persist(person);
        logger.info("Data inserted successfully!");
    }

    private static void update(EntityManager em, Person person) {
        em.merge(person);
        logger.info("Data updated successfully!");
    }

    private static void remove(EntityManager em, Person person) {
        em.remove(person);
        logger.info("Data deleted successfully!");
    }
}
