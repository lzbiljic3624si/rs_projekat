package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.RokRequest;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:github_race;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ZadatakService.class, RasporedService.class, GitHubConcurrencyTest.TimeConfig.class})
class GitHubConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ZadatakService zadaci;
    @Autowired RasporedService raspored;
    @MockitoBean GitHubService github;

    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfig {
        @Bean @Primary Clock githubTestClock() { return Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneId.of("Europe/Belgrade")); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void promenaRepozitorijumaCekaObjavuIPotomSeOdbija() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        long[] ids = tx.execute(status -> {
            Predmet predmet = new Predmet(); predmet.setNaziv("Programiranje"); predmet.setJezik("sr"); em.persist(predmet);
            Profesor profesor = new Profesor(); profesor.setIme("Pera"); profesor.setPrezime("Peric");
            profesor.setEmail("prof@raf.rs"); profesor.setPasswordHash("hash"); em.persist(profesor);
            PredajePredmet veza = new PredajePredmet(); veza.setProfesor(profesor); veza.setPredmet(predmet); em.persist(veza);
            Rok rok = new Rok(); rok.setNaziv("Jun"); rok.setSkolskaGodina(2030); rok.setRepoPutanja("https://github.com/prof/rok"); em.persist(rok);
            Termin termin = new Termin(); termin.setPredmet(predmet); termin.setRok(rok);
            termin.setDatum(LocalDate.of(2030,6,15)); termin.setVremePocetak(LocalTime.of(10,0)); termin.setVremeKraj(LocalTime.NOON); em.persist(termin);
            Grupa grupa = new Grupa(); grupa.setNaziv("Grupa1"); grupa.setTermin(termin); em.persist(grupa);
            return new long[]{rok.getId(),grupa.getId()};
        });
        var auth = new UsernamePasswordAuthenticationToken("prof@raf.rs","",List.of(new SimpleGrantedAuthority("ROLE_PROFESOR")));
        CountDownLatch inGitHub = new CountDownLatch(1);
        CountDownLatch finishGitHub = new CountDownLatch(1);
        CountDownLatch changeStarted = new CountDownLatch(1);
        when(github.publish(anyString(),anyString(),anyList(),any())).thenAnswer(call -> {
            inGitHub.countDown();
            if (!finishGitHub.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Test nije oslobodio GitHub poziv");
            return "a".repeat(40);
        });
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Zadatak> publication = pool.submit(() -> tx.execute(status -> zadaci.objavi("Z1",ids[1],
                    List.of(new MockMultipartFile("files","A.txt","text/plain",new byte[]{1})),List.of("A.txt"),auth)));
            try {
                assertTrue(inGitHub.await(5,TimeUnit.SECONDS));
                Future<Boolean> change = pool.submit(() -> {
                    changeStarted.countDown();
                    RokRequest request = new RokRequest(); request.setNaziv("Jun"); request.setSkolskaGodina(2030);
                    request.setRepoPutanja("https://github.com/prof/drugi-rok");
                    try {
                        tx.execute(status -> raspored.izmeniRok(ids[0],request,auth));
                        return true;
                    } catch (ConflictException expected) {
                        return false;
                    }
                });
                assertTrue(changeStarted.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class,()->change.get(150,TimeUnit.MILLISECONDS));
                finishGitHub.countDown();
                assertEquals("prof/rok",publication.get(5,TimeUnit.SECONDS).getGithubRepo());
                assertFalse(change.get(5,TimeUnit.SECONDS));
                assertEquals("https://github.com/prof/rok",tx.execute(status -> raspored.rok(ids[0]).getRepoPutanja()));
            } finally {
                finishGitHub.countDown();
            }
        }
    }

}
