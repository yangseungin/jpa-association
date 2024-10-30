package persistence.sql.entity;

import database.DatabaseServer;
import database.H2;
import jdbc.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.ddl.CreateQueryBuilder;
import persistence.sql.ddl.DropQueryBuilder;
import persistence.sql.domain.Person;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityPersisterTest {
    private JdbcTemplate jdbcTemplate;
    private EntityPersister entityPersister;
    private EntityLoader entityLoader;
    private Person expectedPerson;


    @BeforeEach
    void init() throws SQLException {
        final DatabaseServer server = new H2();
        server.start();
        CreateQueryBuilder queryBuilder = new CreateQueryBuilder(Person.class);
        String tableQuery = queryBuilder.createTableQuery(Person.class);

        jdbcTemplate = new JdbcTemplate(server.getConnection());
        jdbcTemplate.execute(tableQuery);

        entityPersister = new EntityPersister(Person.class, server.getConnection());
        entityLoader = new EntityLoader(server.getConnection());

        expectedPerson = new Person("양승인", 20, "rhfpdk12@naver.com.com", 1);
        entityPersister.insert(expectedPerson);
    }


    @Test
    @DisplayName("EntityPersister update구현")
    void update() {
        Person changeEmailPerson = new Person(1L, "양승인", 20, "change@naver.com");
        entityPersister.update(changeEmailPerson);

        Person updatedPerson = entityLoader.loadEntity(Person.class, 1L);

        assertThat(changeEmailPerson.getEmail()).isEqualTo(updatedPerson.getEmail());
    }

    @Test
    @DisplayName("EntityPersister insert구현")
    void insert() {
        Person insertedPerson = entityLoader.loadEntity(Person.class, 1L);

        assertThat(insertedPerson.getEmail()).isEqualTo(expectedPerson.getEmail());

    }

    @Test
    @DisplayName("EntityPersister delete구현")
    void delete() {
        Person deletePerson = new Person(1L, "양승인", 20, "rhfpdk12@naver.com.com");
        entityPersister.delete(deletePerson);

        assertThatThrownBy(
                () -> entityLoader.loadEntity(Person.class, 1L)
        ).isInstanceOf(RuntimeException.class);
    }

    @AfterEach
    void afterEach() {
        DropQueryBuilder dropQueryBuilder = new DropQueryBuilder();
        String dropTableQuery = dropQueryBuilder.dropTableQuery(Person.class);
        jdbcTemplate.execute(dropTableQuery);
    }

}
