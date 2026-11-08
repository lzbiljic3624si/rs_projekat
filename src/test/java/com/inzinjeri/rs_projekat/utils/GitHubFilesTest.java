package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GitHubFilesTest {
    private static final int MIB = 1024 * 1024;

    @Test
    void uploadCuvaRelativnePutanjeBinarneBajtoveIPrazanFajl() {
        byte[] binary = {0, 1, (byte) 0xff, 42};
        var files = GitHubFiles.upload(List.of(file(binary), file(new byte[0])),
                List.of("src/slika.bin", "README.md"));

        assertEquals("src/slika.bin", files.getFirst().path());
        assertArrayEquals(binary, files.getFirst().content());
        assertEquals("README.md", files.get(1).path());
        assertArrayEquals(new byte[0], files.get(1).content());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/a", "../a", "a/../b", "a/./b", "a//b", "a/", "C:/a", "a\\b",
            ".git/config", "a/.GiT/config", "a\n.txt", "a\u0000.txt"})
    void putanjaOdbijaIzlazakIzFolderaINedozvoljeneSegmente(String path) {
        assertThrows(BadRequestException.class, () -> GitHubFiles.path(path));
    }

    @Test
    void putanjaNeMenjaValidanNazivIOgranicavaDuzinu() {
        assertEquals("src/moj fajl čćž.txt", GitHubFiles.path("src/moj fajl čćž.txt"));
        assertThrows(BadRequestException.class, () -> GitHubFiles.path(null));
        assertThrows(BadRequestException.class, () -> GitHubFiles.path("a".repeat(501)));
    }

    @Test
    void uploadOdbijaNedostajuceFajloveDuplikateIKonfliktFajlaIDirektorijuma() {
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(List.of(), List.of()));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(null, List.of("a")));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(List.of(file(new byte[0])), null));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(List.of(file(new byte[0])), List.of()));
        for (List<String> paths : List.of(List.of("a", "a"), List.of("a", "a/b"), List.of("a/b", "a"))) {
            assertThrows(BadRequestException.class, () -> GitHubFiles.upload(
                    List.of(file(new byte[0]), file(new byte[0])), paths));
        }
    }

    @Test
    void uploadNeDozvoljavaRazlikeSamoUVeliciniSlovaNiWindowsKonfliktFoldera() {
        for (List<String> paths : List.of(List.of("A.java", "a.java"),
                List.of("Src", "src/Main.java"), List.of("SRC/Main.java", "src"))) {
            assertThrows(BadRequestException.class, () -> GitHubFiles.upload(
                    List.of(file(new byte[0]), file(new byte[0])), paths));
        }
        var files = GitHubFiles.upload(List.of(file(new byte[0])), List.of("Src/Main.java"));
        assertEquals("Src/Main.java", files.getFirst().path());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.", "a ", "src./a", "src /a", "CON", "con.txt", "src/NUL.java",
            "prn", "AUX.data", "COM1.txt", "com9", "LPT1", "lpt9.log", "NUL .txt",
            "a?.txt", "a*.txt", "a\".txt", "a<.txt", "a>.txt", "a|.txt"})
    void putanjaOdbijaWindowsNaziveKojiSeMenjajuIliOznacavajuUredjaj(String path) {
        assertThrows(BadRequestException.class, () -> GitHubFiles.path(path));
    }

    @Test
    void uploadOgranicavaBrojPojedinacnuIUkupnuVelicinu() {
        List<MultipartFile> tooMany = Collections.nCopies(201, file(new byte[0]));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(tooMany,
                IntStream.range(0, 201).mapToObj(i -> "f" + i).toList()));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(
                List.of(file(new byte[2 * MIB + 1])), List.of("large.bin")));
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(
                Collections.nCopies(6, file(new byte[2 * MIB])),
                IntStream.range(0, 6).mapToObj(i -> "f" + i).toList()));
    }

    @Test
    void uploadCitaOgranicenStreamINeVerujePrijavljenojVelicini() {
        MockMultipartFile lying = new MockMultipartFile("files", "ignored", null, new byte[2 * MIB + 1]) {
            @Override public long getSize() { return 0; }
        };
        assertThrows(BadRequestException.class, () -> GitHubFiles.upload(List.of(lying), List.of("large.bin")));
        MockMultipartFile broken = new MockMultipartFile("files", new byte[0]) {
            @Override public InputStream getInputStream() throws IOException { throw new IOException("private server path"); }
        };
        var error = assertThrows(BadRequestException.class,
                () -> GitHubFiles.upload(List.of(broken), List.of("broken")));
        assertFalse(error.getMessage().contains("private server path"));
    }

    @Test
    void nazivFolderaJeStabilanBezbedanIOgranicen() {
        assertEquals("Racunarske-mreze", GitHubFiles.segment("Računarske mreže", "predmet-7"));
        assertEquals("pperic2126si", GitHubFiles.segment("pperic2126si", "student-7"));
        assertEquals("predmet-7", GitHubFiles.segment("... / ", "predmet-7"));
        assertEquals("student-7", GitHubFiles.segment(null, "student-7"));
        assertTrue(GitHubFiles.segment("a".repeat(300), "predmet-7").length() <= 80);
        assertThrows(BadRequestException.class, () -> GitHubFiles.segment(".git", "predmet-7"));
    }

    @Test
    void repoUrlSeNormalizujeBezProizvoljnogServerskogOdredista() {
        assertEquals("pera/ispit", GitHubFiles.repository("https://github.com/Pera/Ispit"));
        assertEquals("pera/ispit", GitHubFiles.repository("https://github.com/Pera/Ispit.git/"));
        for (String url : List.of("http://github.com/pera/ispit", "https://example.com/pera/ispit",
                "https://github.com.evil.test/pera/ispit", "https://pera@github.com/pera/ispit",
                "https://github.com:444/pera/ispit", "https://github.com/pera/ispit?x=1",
                "https://github.com/pera/ispit#main", "https://github.com/pera/../ispit",
                "https://github.com/pera/ispit/tree/main", "https://github.com/pera%2Frepo/ispit")) {
            assertThrows(BadRequestException.class, () -> GitHubFiles.repository(url), url);
        }
    }

    @Test
    void folderUrlKodiraPutanjuBezPromeneHosta() {
        assertEquals("https://github.com/pera/ispit/tree/abc123/Predmet/Grupa%201/a%23b",
                GitHubFiles.folderUrl("pera/ispit", "abc123", "Predmet/Grupa 1/a#b"));
        assertThrows(BadRequestException.class,
                () -> GitHubFiles.folderUrl("https://evil.test/repo", "abc123", "Predmet"));
    }

    @Test
    void samoIspravanUtf8BezBinarnihKontrolaJeTekst() {
        assertTrue(GitHubFiles.isText("čćž\n\ttekst\r\n".getBytes(StandardCharsets.UTF_8)));
        assertTrue(GitHubFiles.isText(new byte[0]));
        assertFalse(GitHubFiles.isText(new byte[] {0}));
        assertFalse(GitHubFiles.isText(new byte[] {7}));
        assertFalse(GitHubFiles.isText(new byte[] {(byte) 0xc3, 0x28}));
    }

    private MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("files", "untrusted-original-name", "application/octet-stream", bytes);
    }
}
