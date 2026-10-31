package com.inzinjeri.rs_projekat.model;

import com.inzinjeri.rs_projekat.utils.EntityMappers;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Types;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatusRadaTest {
    @Test
    void hibernateMapiraEnumNaPostojecuTekstualnuKolonuBezPristupaBazi() {
        try (var registry = new StandardServiceRegistryBuilder()
                .applySetting(JdbcSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting(JdbcSettings.ALLOW_METADATA_ON_BOOT, false)
                .build()) {
            var metadata = new MetadataSources(registry)
                    .addAnnotatedClass(Rad.class).addAnnotatedClass(Student.class)
                    .addAnnotatedClass(Zadatak.class).addAnnotatedClass(Grupa.class)
                    .addAnnotatedClass(Termin.class).addAnnotatedClass(Predmet.class)
                    .addAnnotatedClass(Rok.class).addAnnotatedClass(Ucionica.class)
                    .buildMetadata();
            var property = metadata.getEntityBinding(Rad.class.getName()).getProperty("status");
            var type = (BasicType<?>) property.getType();

            assertTrue(type.getReturnedClass().isEnum());
            assertEquals(Types.VARCHAR, type.getJdbcType().getDefaultSqlTypeCode());
            for (String status : new String[] {"PREUZET", "PREDAT", "OCENJEN"}) {
                Object value = type.getJavaTypeDescriptor().fromString(status);
                assertEquals(status, ((Enum<?>) value).name());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PREUZET", "PREDAT", "OCENJEN"})
    void jsonZadrzavaPostojeceTekstualneStatuse(String status) {
        Rad rad = new Rad();
        rad.setStatus(StatusRada.valueOf(status));
        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(EntityMappers.toResponse(rad));

        assertEquals(status, mapper.readValue(json, Map.class).get("status"));
    }
}
