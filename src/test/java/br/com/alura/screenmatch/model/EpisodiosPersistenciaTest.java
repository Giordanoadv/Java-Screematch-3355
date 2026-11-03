package br.com.alura.screenmatch.model;

import org.hibernate.cfg.Configuration;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EpisodiosPersistenciaTest {

    @Test
    void salvaEpisodiosComChaveEstrangeiraAoAtualizarSerieExistente() throws Exception {
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        assertEquals("true", properties.getProperty("spring.jpa.show-sql"));
        assertEquals("false", properties.getProperty("spring.jpa.properties.hibernate.format_sql"));

        var configuration = new Configuration()
                .setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy())
                .addAnnotatedClass(Serie.class)
                .addAnnotatedClass(Episodio.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:episodios;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", properties.getProperty("spring.jpa.show-sql"))
                .setProperty("hibernate.format_sql", properties.getProperty("spring.jpa.properties.hibernate.format_sql"));

        try (var factory = configuration.buildSessionFactory()) {
            var serie = new Serie();
            serie.setTitulo("Friends");
            serie.setTotalTemporadas(1);
            try (var session = factory.openSession()) {
                var transaction = session.beginTransaction();
                session.persist(serie);
                transaction.commit();
            }

            // Como no menu: a série já existe e está fora da sessão de consulta.
            Serie serieEncontrada;
            try (var session = factory.openSession()) {
                serieEncontrada = session.find(Serie.class, serie.getId());
            }
            var primeiro = new Episodio(1, new DadosEpisodio("Piloto", 1, "8.0", "1994-09-22"));
            var segundo = new Episodio(1, new DadosEpisodio("Segundo episódio", 2, "8.1", "1994-09-29"));
            serieEncontrada.setEpisodios(new ArrayList<>(List.of(primeiro, segundo)));

            try (var session = factory.openSession()) {
                var transaction = session.beginTransaction();
                session.merge(serieEncontrada);
                transaction.commit();
            }
            try (var session = factory.openSession()) {
                var episodios = session.createQuery("from Episodio order by numeroEpisodio", Episodio.class).getResultList();
                assertEquals(2, episodios.size());
                for (var episodio : episodios) {
                    assertNotNull(episodio.getId());
                    assertNotNull(episodio.getSerie());
                    assertEquals(serie.getId(), episodio.getSerie().getId());
                }
                assertEquals("Piloto", episodios.get(0).getTitulo());
                assertEquals("Segundo episódio", episodios.get(1).getTitulo());
            }
        }
    }
}
