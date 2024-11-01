package persistence.sql.dml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.Metadata;
import persistence.sql.domain.Order;
import persistence.sql.domain.OrderItem;
import persistence.sql.domain.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectQueryBuilderTest {


    @Test
    @DisplayName("Person 객체로 Select(findAll) Query 만들기")
    void findAllQuery() {
        Metadata metadata = new Metadata(Person.class);
        SelectQueryBuilder selectQueryBuilder = new SelectQueryBuilder();
        String findAllQuery = selectQueryBuilder.findAll(metadata.getEntityTable(), metadata.getEntityColumns());

        assertEquals(findAllQuery, "select users.id, users.nick_name, users.old, users.email FROM users");
    }

    @Test
    @DisplayName("Person 객체로 Select(findById) Query 만들기")
    void findByIdQuery() {
        Metadata metadata = new Metadata(Person.class);
        SelectQueryBuilder selectQueryBuilder = new SelectQueryBuilder();
        String findByIdQuery = selectQueryBuilder.findById(metadata.getEntityTable(), metadata.getEntityColumns(), 1L);

        assertEquals(findByIdQuery, "select users.id, users.nick_name, users.old, users.email FROM users where id = 1");
    }

    @Test
    @DisplayName("Person 객체로 Select(findById) Query 만들기")
    void findByStringIdQuery() {
        Metadata metadata = new Metadata(Person.class);
        SelectQueryBuilder selectQueryBuilder = new SelectQueryBuilder();
        String findByIdQuery = selectQueryBuilder.findById(metadata.getEntityTable(), metadata.getEntityColumns(), "yang");

        assertEquals(findByIdQuery, "select users.id, users.nick_name, users.old, users.email FROM users where id = 'yang'");
    }

    @Test
    @DisplayName("Person 객체로 Select(findById) Query 만들기")
    void findAllWithJoinQuery() {
        Metadata mainMetaData = new Metadata(Order.class);
        Metadata joinMetaData = new Metadata(OrderItem.class);

        SelectQueryBuilder selectQueryBuilder = new SelectQueryBuilder();
        String findByIdQuery = selectQueryBuilder.findAllWithJoin(mainMetaData.getEntityTable(), mainMetaData.getEntityColumns(),
                joinMetaData.getEntityTable(), joinMetaData.getEntityColumns());

        assertEquals(findByIdQuery, "SELECT orders.id, orders.orderNumber, orders.orderItems, order_items.id, order_items.product, order_items.quantity FROM orders LEFT JOIN order_items ON orders.order_id = order_items.id");
    }
}
