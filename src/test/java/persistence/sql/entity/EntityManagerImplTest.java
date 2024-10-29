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
import persistence.sql.entity.EntityManager;
import persistence.sql.entity.EntityManagerImpl;
import persistence.sql.entity.EntityPersister;
import persistence.sql.entity.PersistenceContext;
import persistence.sql.entity.PersistenceContextImpl;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityManagerImplTest {

    private JdbcTemplate jdbcTemplate;
    private EntityManager entityManager;
    private EntityPersister entityPersister;
    private PersistenceContext persistenceContext;


    @BeforeEach
    void init() throws SQLException {
        final DatabaseServer server = new H2();
        server.start();
        CreateQueryBuilder queryBuilder = new CreateQueryBuilder(Person.class);
        String tableQuery = queryBuilder.createTableQuery(Person.class);
        Person person = new Person("yang", 23, "rhfp@naver.com", 3);

        jdbcTemplate = new JdbcTemplate(server.getConnection());
        jdbcTemplate.execute(tableQuery);

        entityPersister = new EntityPersister(Person.class, server.getConnection());
        persistenceContext = new PersistenceContextImpl();

        entityManager = new EntityManagerImpl(entityPersister, persistenceContext, server.getConnection());
        entityManager.persist(person);
    }

    @AfterEach
    void afterEach() {
        DropQueryBuilder dropQueryBuilder = new DropQueryBuilder();
        String dropTableQuery = dropQueryBuilder.dropTableQuery(Person.class);
        jdbcTemplate.execute(dropTableQuery);
    }

    @Test
    @DisplayName("EntityManager의 find구현")
    void entityManager_find() {
        Person expectPerson = new Person(1L, "yang", 23, "rhfp@naver.com");

        Person resultPerson = entityManager.find(Person.class, 1L);

        assertAll(
                () -> assertThat(expectPerson.getAge()).isEqualTo(resultPerson.getAge()),
                () -> assertThat(expectPerson.getEmail()).isEqualTo(resultPerson.getEmail()),
                () -> assertThat(expectPerson.getId()).isEqualTo(resultPerson.getId()),
                () -> assertThat(expectPerson.getName()).isEqualTo(resultPerson.getName())
        );
    }

    @Test
    @DisplayName("EntityManager의 persist구현")
    void entityManager_persist() throws SQLException {
        Person expectPerson = new Person("yang2", 25, "rhfpdk92@naver.com", 3);

        Person findPerson = (Person) entityManager.persist(expectPerson);

        assertAll(
                () -> assertThat(expectPerson.getAge()).isEqualTo(findPerson.getAge()),
                () -> assertThat(expectPerson.getEmail()).isEqualTo(findPerson.getEmail()),
                () -> assertThat(expectPerson.getId()).isEqualTo(findPerson.getId()),
                () -> assertThat(expectPerson.getName()).isEqualTo(findPerson.getName())
        );
    }


    @Test
    @DisplayName("EntityManager의 persist시 idValue가 null인 경우 id값이 자동으로 생성된다")
    void entityManager_persist_with_null_id() throws SQLException {
        long expectedId = 2L;
        Person expectPerson = new Person(null, "yang2", 25, "rhfpdk92@naver.com");

        Person resultPerson = (Person) entityManager.persist(expectPerson);

        assertThat(resultPerson.getId()).isEqualTo(expectedId);
    }


    @Test
    @DisplayName("EntityManager의 remove구현")
    void entityManager_remove() throws SQLException {
        Person expectPerson = new Person(2L, "yang2", 25, "rhfpdk92@naver.com");

        entityManager.persist(expectPerson);
        entityManager.remove(expectPerson);

        assertThrows(RuntimeException.class, () -> entityManager.find(Person.class, 2L));
    }

    //    @Test
    @DisplayName("EntityManager의 update구현")
    void entityManager_update() throws SQLException {
        Person person = new Person(2L, "yang2", 25, "rhfpdk92@naver.com");
        Person updatePerson = new Person(2L, "yang3", 233, "rhfpdk92@gmail.com");

        entityManager.persist(person);
        entityManager.update(updatePerson);
        Person findPerson = entityManager.find(Person.class, 2L);

        assertAll(
                () -> assertThat(findPerson.getAge()).isEqualTo(updatePerson.getAge()),
                () -> assertThat(findPerson.getEmail()).isEqualTo(updatePerson.getEmail()),
                () -> assertThat(findPerson.getId()).isEqualTo(updatePerson.getId()),
                () -> assertThat(findPerson.getName()).isEqualTo(updatePerson.getName())
        );

    }
}
