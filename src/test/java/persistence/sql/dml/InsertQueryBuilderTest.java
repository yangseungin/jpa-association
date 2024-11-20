package persistence.sql.dml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.Metadata;
import persistence.sql.domain.Order;
import persistence.sql.domain.OrderItem;
import persistence.sql.domain.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertQueryBuilderTest {

    @Test
    @DisplayName("Person 객체로 Insert Query 만들기")
    void insertQuery() {
        Person person = new Person("양승인", 33, "rhfpdk92@naver.com", 1);
        Metadata metadata = new Metadata(Person.class);
        InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder();
        String insertQuery = insertQueryBuilder.getInsertQuery(metadata.getEntityTable(), metadata.getEntityColumns(), person, null);

        assertEquals(insertQuery, "insert into users (nick_name, old, email) VALUES ('양승인', 33, 'rhfpdk92@naver.com')");
    }

    @Test
    @DisplayName("Order 객체로 Insert Query 만들기")
    void insertQuery2() {
        Order order = new Order(1L, "농작물_주문번호1");

        OrderItem orderItem1 = new OrderItem(1L, "감자", 3);
        OrderItem orderItem2 = new OrderItem(2L, "고구마", 1);

        order.getOrderItems().add(orderItem1);
        order.getOrderItems().add(orderItem2);

        Metadata metadata = new Metadata(OrderItem.class);
        InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder();

        String insertQuery = insertQueryBuilder.getInsertQuery(metadata.getEntityTable(), metadata.getEntityColumns(), orderItem1, order);

        String expectedQuery = "insert into order_items (product, quantity, order_id) VALUES ('감자', 3, 1)";

        assertEquals(expectedQuery, insertQuery);
    }


}
