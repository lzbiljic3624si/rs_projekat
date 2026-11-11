package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.IzmenaFajlaRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.sun.net.httpserver.HttpServer;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:github_workflow;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RadService.class, ZadatakService.class, GitHubWorkflowIntegrationTest.TestConfig.class})
class GitHubWorkflowIntegrationTest {
    private static final String REPO = "owner/rok";
    private static final String STARTER = "Jun-2031/Programiranje/Grupa1/zadatak";
    private static final String SOLUTION = "Jun-2031/Programiranje/Grupa1/pera";
    private static final Snapshot EMPTY = new Snapshot(sha("0"), sha("a"), null, null, null, null, null);
    private static final Snapshot SOURCE = new Snapshot(sha("1"), sha("b"), sha("04"), sha("07"), sha("10"), sha("20"), "Pocetni zadatak");
    private static final Snapshot SUBMITTED = new Snapshot(sha("2"), sha("c"), sha("05"), sha("08"), sha("11"), sha("21"), "Studentsko resenje");
    private static final Snapshot EDITED = new Snapshot(sha("3"), sha("d"), sha("06"), sha("09"), sha("12"), sha("22"), "Profesorska izmena");

    @Autowired EntityManager em;
    @Autowired ZadatakService zadaci;
    @Autowired RadService radovi;
    @Autowired ZadatakRepository zadatakRepository;
    @Autowired RadRepository radRepository;
    @Autowired FakeGitHub fake;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean @Primary Clock workflowClock() {
            return Clock.fixed(Instant.parse("2030-06-15T08:15:00Z"), ZoneId.of("Europe/Belgrade"));
        }
        @Bean(destroyMethod = "close") FakeGitHub fakeGitHub() throws IOException { return new FakeGitHub(); }
        @Bean GitHubService githubService(FakeGitHub fake) {
            return new GitHubService("owner", "integration-test-token",
                    RestClient.builder().baseUrl("http://127.0.0.1:" + fake.server.getAddress().getPort()).build());
        }
    }

    @Test
    void objavaPreuzimanjePredajaIProfesorskaIzmenaCuvaObaOriginalnaSnapshota() throws Exception {
        Grupa grupa = pripremiGrupuIPrijavu();
        Authentication profesor = auth("prof@raf.rs", "PROFESOR");
        Authentication student = auth("pera@raf.rs", "STUDENT");

        publication(EMPTY, SOURCE);
        Long zadatakId = zadaci.objavi("Prvi zadatak", grupa.getId(), List.of(upload(SOURCE.text())), List.of("A.txt"), profesor).getId();
        em.flush(); em.clear();
        assertEquals(SOURCE.commit(), zadatakRepository.findById(zadatakId).orElseThrow().getGithubCommit());

        RadService.PocetakRada pocetak = radovi.zapocni(zadatakId, student);
        Long radId = pocetak.rad().getId();
        assertNotNull(radId);
        assertEquals(SOURCE.commit(), pocetak.izvorniCommit());
        assertEquals("pera", pocetak.nazivFoldera());
        read(SOURCE);
        assertArrayEquals(bytes(SOURCE.text()), zadaci.fajl(zadatakId, "A.txt", student).content());

        publication(SOURCE, SUBMITTED);
        radovi.predaj(radId, List.of(upload(SUBMITTED.text())), List.of("A.txt"), student);
        em.flush(); em.clear();
        Rad predat = radRepository.findById(radId).orElseThrow();
        assertEquals(StatusRada.PREDAT, predat.getStatus());
        assertEquals(SUBMITTED.commit(), predat.getGithubCommit());
        assertEquals(SOURCE.commit(), predat.getIzvorniCommit());
        assertEquals(SOLUTION, predat.getGithubFolder());
        assertEquals("https://github.com/owner/rok/tree/" + SUBMITTED.commit() + "/" + SOLUTION, predat.getRepoPutanjaResenja());

        read(SUBMITTED);
        GitHubService.FileContent zaIzmenu = radovi.fajl(radId, "A.txt", false, profesor);
        assertArrayEquals(bytes(SUBMITTED.text()), zaIzmenu.content());
        IzmenaFajlaRequest request = new IzmenaFajlaRequest();
        request.setPutanja("A.txt"); request.setPrethodniSha(zaIzmenu.sha()); request.setTekst(EDITED.text());
        edit();
        assertEquals(EDITED.commit(), radovi.izmeniFajl(radId, request, profesor));
        em.flush(); em.clear();

        read(SUBMITTED);
        assertArrayEquals(bytes(SUBMITTED.text()), radovi.fajl(radId, "A.txt", false, profesor).content());
        repo(); head(EDITED.commit()); read(EDITED);
        GitHubService.FileContent radna = radovi.fajl(radId, "A.txt", true, profesor);
        assertArrayEquals(bytes(EDITED.text()), radna.content());
        assertEquals(EDITED.commit(), radna.commit());

        read(SOURCE);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zadaci.arhiva(zadatakId, profesor)))) {
            assertEquals("A.txt", Objects.requireNonNull(zip.getNextEntry()).getName());
            assertArrayEquals(bytes(SOURCE.text()), zip.readAllBytes());
            assertNull(zip.getNextEntry());
        }
        repo(); commit(SUBMITTED); folderPath(SUBMITTED); files(SUBMITTED); blob(SUBMITTED);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(
                radovi.arhivaPredmeta(grupa.getTermin().getPredmet().getId(), StatusRada.PREDAT,
                        grupa.getId(), grupa.getTermin().getId(), zadatakId, profesor)))) {
            assertEquals(SOLUTION + "/A.txt", Objects.requireNonNull(zip.getNextEntry()).getName());
            assertArrayEquals(bytes(SUBMITTED.text()), zip.readAllBytes());
            assertNull(zip.getNextEntry());
        }
        Rad sacuvan = radRepository.findById(radId).orElseThrow();
        assertEquals(SUBMITTED.commit(), sacuvan.getGithubCommit());
        assertEquals(SOURCE.commit(), sacuvan.getIzvorniCommit());
        assertEquals(SOURCE.commit(), zadatakRepository.findById(zadatakId).orElseThrow().getGithubCommit());
        assertEquals(List.of(SOURCE.text(), SUBMITTED.text(), EDITED.text()), fake.calls.stream()
                .filter(c -> c.method().equals("POST") && c.path().endsWith("/git/blobs"))
                .map(c -> new String(Base64.getDecoder().decode(fake.mapper.readTree(c.body()).get("content").asString()), StandardCharsets.UTF_8))
                .toList());
        assertEquals(List.of(STARTER, SOLUTION, SOLUTION), fake.calls.stream()
                .filter(c -> c.method().equals("POST") && c.path().endsWith("/git/trees"))
                .map(c -> fake.mapper.readTree(c.body())).filter(body -> body.has("base_tree")
                        && !body.get("base_tree").asString().equals(SUBMITTED.folder()))
                .map(body -> body.get("tree").get(0).get("path").asString()).toList());
        assertTrue(fake.calls.stream().filter(c -> c.method().equals("PATCH"))
                .allMatch(c -> !fake.mapper.readTree(c.body()).get("force").asBoolean()));
        assertNull(fake.failure);
        assertTrue(fake.replies.isEmpty(), "Nisu izvrseni svi ocekivani GitHub zahtevi");
    }

    private Grupa pripremiGrupuIPrijavu() {
        Predmet predmet = new Predmet(); predmet.setNaziv("Programiranje"); predmet.setJezik("sr"); em.persist(predmet);
        Profesor profesor = new Profesor(); profesor.setIme("Pera"); profesor.setPrezime("Peric");
        profesor.setEmail("prof@raf.rs"); profesor.setPasswordHash("test-hash"); em.persist(profesor);
        PredajePredmet veza = new PredajePredmet(); veza.setProfesor(profesor); veza.setPredmet(predmet); em.persist(veza);
        Rok rok = new Rok(); rok.setNaziv("Jun"); rok.setSkolskaGodina(2030); rok.setRepoPutanja("https://github.com/" + REPO); em.persist(rok);
        Termin termin = new Termin(); termin.setPredmet(predmet); termin.setRok(rok); termin.setDatum(LocalDate.of(2030, 6, 15));
        termin.setVremePocetak(LocalTime.of(10, 0)); termin.setVremeKraj(LocalTime.NOON); em.persist(termin);
        Grupa grupa = new Grupa(); grupa.setNaziv("Grupa1"); grupa.setTermin(termin); em.persist(grupa);
        Student student = new Student(); student.setIme("Pera"); student.setPrezime("Peric"); student.setEmail("pera@raf.rs");
        student.setPasswordHash("test-hash"); student.setBrIndeksa("2021/001"); student.setSmer("SI"); student.setGodUpisa(2021); em.persist(student);
        PrijavljenStudent prijava = new PrijavljenStudent(); prijava.setStudent(student); prijava.setGrupa(grupa); em.persist(prijava);
        return grupa;
    }

    private void publication(Snapshot previous, Snapshot next) {
        repo(); head(previous.commit()); commit(previous);
        if (previous == EMPTY) tree(previous.root(), List.of()); else folderPath(previous);
        fake.expect("POST", "/git/blobs", Map.of("sha", next.blob()));
        fake.expect("POST", "/git/trees", Map.of("sha", next.folder()));
        finishWrite(next);
    }

    private void edit() {
        repo(); head(SUBMITTED.commit()); commit(SUBMITTED); folderPath(SUBMITTED); files(SUBMITTED); blob(SUBMITTED);
        fake.expect("POST", "/git/blobs", Map.of("sha", EDITED.blob()));
        fake.expect("POST", "/git/trees", Map.of("sha", EDITED.folder()));
        finishWrite(EDITED);
    }

    private void finishWrite(Snapshot next) {
        fake.expect("POST", "/git/trees", Map.of("sha", next.root()));
        fake.expect("POST", "/git/commits", Map.of("sha", next.commit()));
        fake.expect("PATCH", "/git/refs/heads/main", Map.of());
    }

    private void read(Snapshot snapshot) { repo(); commit(snapshot); folderPath(snapshot); files(snapshot); blob(snapshot); }
    private void repo() { fake.expect("GET", "", Map.of("private", true, "default_branch", "main")); }
    private void head(String sha) { fake.expect("GET", "/git/ref/heads/main", Map.of("object", Map.of("sha", sha))); }
    private void commit(Snapshot s) { fake.expect("GET", "/git/commits/" + s.commit(), Map.of("tree", Map.of("sha", s.root()))); }
    private void folderPath(Snapshot s) {
        tree(s.root(), List.of(directory("Jun-2031", sha("ee"))));
        tree(sha("ee"), List.of(directory("Programiranje", s.subject())));
        tree(s.subject(), List.of(directory("Grupa1", s.group())));
        List<Map<String, Object>> group = new ArrayList<>(List.of(directory("zadatak", SOURCE.folder())));
        if (s != SOURCE) group.add(directory("pera", s.folder()));
        tree(s.group(), group);
    }
    private void files(Snapshot s) {
        fake.expect("GET", "/git/trees/" + s.folder() + "?recursive=1", Map.of("truncated", false, "tree", List.of(
                Map.of("path", "A.txt", "type", "blob", "mode", "100644", "sha", s.blob(), "size", bytes(s.text()).length))));
    }
    private void blob(Snapshot s) {
        fake.expect("GET", "/git/blobs/" + s.blob(), Map.of("encoding", "base64", "size", bytes(s.text()).length,
                "content", Base64.getEncoder().encodeToString(bytes(s.text()))));
    }
    private void tree(String sha, List<Map<String, Object>> entries) { fake.expect("GET", "/git/trees/" + sha, Map.of("truncated", false, "tree", entries)); }
    private static Map<String, Object> directory(String name, String sha) { return Map.of("path", name, "type", "tree", "mode", "040000", "sha", sha); }
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static String sha(String seed) { return seed.repeat(40 / seed.length()); }
    private static MockMultipartFile upload(String text) { return new MockMultipartFile("files", "A.txt", "text/plain", bytes(text)); }
    private static Authentication auth(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    private record Snapshot(String commit, String root, String subject, String group, String folder, String blob, String text) {}

    static class FakeGitHub implements AutoCloseable {
        final JsonMapper mapper = JsonMapper.builder().build();
        final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
        final List<Call> calls = new CopyOnWriteArrayList<>();
        final HttpServer server;
        volatile String failure;

        FakeGitHub() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().toString();
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                calls.add(new Call(method, path, body));
                Reply reply = replies.poll();
                int status = 200;
                if (reply == null || !reply.method().equals(method) || !reply.path().equals(path)
                        || !"Bearer integration-test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    failure = "Neocekivan zahtev " + method + " " + path + "; ocekivano " + reply;
                    reply = new Reply(method, path, "{}"); status = 500;
                }
                byte[] response = bytes(reply.body());
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response); exchange.close();
            });
            server.start();
        }
        void expect(String method, String suffix, Object body) {
            replies.add(new Reply(method, "/repos/" + REPO + suffix, mapper.writeValueAsString(body)));
        }
        @Override public void close() { server.stop(0); }
    }
    private record Reply(String method, String path, String body) {}
    private record Call(String method, String path, String body) {}
}
