package persistence.sql.entity;

import database.DatabaseServer;
import database.H2;
import jdbc.JdbcTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.domain.Order;

import java.sql.SQLException;
import java.util.List;

class EntityLoaderTest {

    private JdbcTemplate jdbcTemplate;
    private EntityLoader entityLoader;

    @BeforeEach
    void init() throws SQLException {
        final DatabaseServer server = new H2();
        server.start();
        jdbcTemplate = new JdbcTemplate(server.getConnection());
        entityLoader = new EntityLoader(server.getConnection());
    }

    @Test
    @DisplayName("find 메서드는 주어진 클래스와 연관관계에 있는 엔티티까지 반환한다.")
    void find_containsOneToMany() {
        //given
        String orderCreateQuery = "CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, orderNumber VARCHAR(255));";
        String orderItemCreateQuery = "CREATE TABLE order_items (id BIGINT AUTO_INCREMENT PRIMARY KEY, product VARCHAR(255), quantity INT, order_id BIGINT);";
        jdbcTemplate.execute(orderCreateQuery);
        jdbcTemplate.execute(orderItemCreateQuery);
        jdbcTemplate.execute("INSERT INTO order_items (id, order_id, product, quantity) VALUES (1, 1,'감자', 5);");
        jdbcTemplate.execute("INSERT INTO order_items (id, order_id, product, quantity) VALUES (2, 1,'고구마', 6);");
        jdbcTemplate.execute("INSERT INTO order_items (id, order_id, product, quantity) VALUES (3, 1,'호박', 1);");
        jdbcTemplate.execute("INSERT INTO orders (id, orderNumber) VALUES (1, '농작물_주문번호1');");

        //when
        List<Order> orders = entityLoader.loadEntitiesWithJoin(Order.class);

        //then
        Assertions.assertThat(orders.get(0).getOrderItems()).hasSize(3);
        Assertions.assertThat(orders.get(0).getOrderItems().get(0).getProduct()).isEqualTo("감자");
    }

}
