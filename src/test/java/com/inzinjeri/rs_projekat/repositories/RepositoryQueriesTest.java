package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RepositoryQueriesTest {
    @Test
    void hibernateProveravaNoveUpiteBezPovezivanjaNaBazu() throws Exception {
        try (var registry = new StandardServiceRegistryBuilder()
                .applySetting(JdbcSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting(JdbcSettings.ALLOW_METADATA_ON_BOOT, false).build()) {
            var metadata = new MetadataSources(registry)
                    .addAnnotatedClass(Rad.class).addAnnotatedClass(Student.class)
                    .addAnnotatedClass(Zadatak.class).addAnnotatedClass(Grupa.class)
                    .addAnnotatedClass(Termin.class).addAnnotatedClass(Predmet.class)
                    .addAnnotatedClass(Rok.class).addAnnotatedClass(Ucionica.class).buildMetadata();
            try (var factory = (SessionFactoryImplementor) metadata.buildSessionFactory()) {
                var translator = factory.getQueryEngine().getHqlTranslator();
                String radovi = RadRepository.class.getMethod("findByPredmetIdAndStatus", Long.class, StatusRada.class)
                        .getAnnotation(Query.class).value();
                String preklapanje = TerminRepository.class.getMethod("postojiPreklapanjeOsim", Long.class,
                        Long.class, LocalDate.class, LocalTime.class, LocalTime.class).getAnnotation(Query.class).value();
                String grupa = GrupaRepository.class.getMethod("findByIdZaIzmenu", Long.class).getAnnotation(Query.class).value();
                String zadatak = ZadatakRepository.class.getMethod("findByIdZaIzmenu", Long.class).getAnnotation(Query.class).value();
                String rad = RadRepository.class.getMethod("findByIdZaIzmenu", Long.class).getAnnotation(Query.class).value();
                assertDoesNotThrow(() -> translator.translate(radovi, Rad.class));
                assertDoesNotThrow(() -> translator.translate(preklapanje, Boolean.class));
                assertDoesNotThrow(() -> translator.translate(grupa, Grupa.class));
                assertDoesNotThrow(() -> translator.translate(zadatak, Zadatak.class));
                assertDoesNotThrow(() -> translator.translate(rad, Rad.class));
            }
        }
    }
}
