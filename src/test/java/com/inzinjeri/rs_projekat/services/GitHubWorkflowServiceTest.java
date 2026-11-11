package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.IzmenaFajlaRequest;
import com.inzinjeri.rs_projekat.exceptions.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubWorkflowServiceTest {
    private final RadRepository radovi = mock(RadRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final ZadatakRepository zadaci = mock(ZadatakRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final PredajePredmetRepository predaje = mock(PredajePredmetRepository.class);
    private final GitHubService github = mock(GitHubService.class);
    private final RokRepository rokovi = mock(RokRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2030-06-15T08:30:00Z"), ZoneId.of("Europe/Belgrade"));
    private final RadService radService = new RadService(radovi, studenti, zadaci, prijave, predaje, clock, github);
    private final ZadatakService zadatakService = new ZadatakService(zadaci, grupe, studenti, prijave,
            predaje, radovi, clock, github, rokovi);
    private final Authentication studentAuth = auth("pperic2126si@raf.rs", "STUDENT");
    private final Authentication profesorAuth = auth("prof@raf.rs", "PROFESOR");
    private final String source = "a".repeat(40);
    private final String submitted = "b".repeat(40);
    private final Student student = new Student();
    private final Zadatak zadatak = new Zadatak();
    private final Rad rad = new Rad();
    private final Grupa grupa = new Grupa();
    private final MockMultipartFile file = new MockMultipartFile("files", "A.java", "text/plain", new byte[]{0,1,2,-1});

    @BeforeEach
    void podaci() {
        Predmet predmet = new Predmet(); predmet.setId(4L); predmet.setNaziv("Programiranje");
        Rok rok = new Rok(); rok.setId(1L); rok.setNaziv("Novembar"); rok.setSkolskaGodina(2030);
        rok.setRepoPutanja("https://github.com/prof/novembar");
        Termin termin = new Termin(); termin.setId(2L); termin.setPredmet(predmet); termin.setRok(rok);
        termin.setDatum(LocalDate.of(2030,6,15)); termin.setVremePocetak(LocalTime.of(10,0)); termin.setVremeKraj(LocalTime.NOON);
        grupa.setId(3L); grupa.setNaziv("Grupa1"); grupa.setTermin(termin);
        zadatak.setId(5L); zadatak.setNaziv("Z1"); zadatak.setGrupa(grupa);
        zadatak.setGithubRepo("prof/novembar"); zadatak.setGithubFolder("Programiranje/Grupa1/zadatak"); zadatak.setGithubCommit(source);
        zadatak.setRepoPutanja("https://github.com/prof/novembar/tree/"+source+"/Programiranje/Grupa1/zadatak");
        student.setId(7L); student.setEmail(studentAuth.getName());
        rad.setId(8L); rad.setStudent(student); rad.setZadatak(zadatak); rad.setStatus(StatusRada.PREUZET);
        rad.setVremePreuzimanja(LocalDateTime.of(2030,6,15,10,15)); rad.setIzvorniCommit(source);
        rad.setGithubRepo("prof/novembar"); rad.setGithubFolder("Programiranje/Grupa1/pperic2126si");
        PredajePredmet veza = new PredajePredmet(); veza.setPredmet(predmet);
        when(predaje.getPredmetiZaPredavaca(profesorAuth.getName())).thenReturn(List.of(veza));
        when(grupe.findByIdZaIzmenu(3L)).thenReturn(Optional.of(grupa));
        when(rokovi.repoZaGrupuZaIzmenu(3L)).thenReturn(Optional.of("https://github.com/prof/novembar"));
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak));
        when(zadaci.findByIdZaIzmenu(5L)).thenReturn(Optional.of(zadatak));
        when(zadaci.pronadjiPoGrupi(3L)).thenReturn(Optional.of(zadatak));
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(student));
        when(prijave.existsByStudent_IdAndGrupa_Id(7L,3L)).thenReturn(true);
        when(radovi.findById(8L)).thenReturn(Optional.of(rad));
        when(radovi.findByIdZaIzmenu(8L)).thenReturn(Optional.of(rad));
        when(radovi.save(any())).thenAnswer(call -> { Rad r=call.getArgument(0); r.setId(8L); return r; });
        when(zadaci.save(any())).thenAnswer(call -> call.getArgument(0));
        when(github.publish(anyString(),anyString(),anyList(),any())).thenReturn(submitted);
    }

    @Test
    void predajaCuvaBajtoveStvarnuPutanjuICommitTekPosleUspesnogUpisa() {
        Rad odgovor = radService.predaj(8L,List.of(file),List.of("src/A.java"),studentAuth);
        assertEquals(StatusRada.PREDAT,odgovor.getStatus()); assertEquals(submitted,odgovor.getGithubCommit());
        assertEquals(source,odgovor.getIzvorniCommit()); assertNotNull(odgovor.getVremePredaje());
        assertTrue(odgovor.getRepoPutanjaResenja().contains("/tree/"+submitted+"/Programiranje/Grupa1/pperic2126si"));
        verify(github).publish(eq("prof/novembar"),eq("Programiranje/Grupa1/pperic2126si"),
                argThat(files -> files.size()==1 && java.util.Arrays.equals(new byte[]{0,1,2,-1},files.getFirst().content())),isNull());
    }

    @Test
    void githubGreskaNeOznacavaRadKaoPredat() {
        when(github.publish(anyString(),anyString(),anyList(),any())).thenThrow(new GitHubException(503,"GitHub nije dostupan"));
        assertThrows(GitHubException.class,()->radService.predaj(8L,List.of(file),List.of("A.java"),studentAuth));
        assertEquals(StatusRada.PREUZET,rad.getStatus()); assertNull(rad.getGithubCommit()); assertNull(rad.getVremePredaje());
        verify(radovi,never()).save(any());
    }

    @Test
    void tudjiStudentINeprijavljenStudentSeOdbijajuPreGitHubPoziva() {
        assertThrows(ForbiddenException.class,()->radService.predaj(8L,List.of(file),List.of("A.java"),auth("tudji@raf.rs","STUDENT")));
        when(prijave.existsByStudent_IdAndGrupa_Id(7L,3L)).thenReturn(false);
        assertThrows(ForbiddenException.class,()->radService.zapocni(5L,studentAuth));
        verifyNoInteractions(github);
    }

    @Test
    void ponovniPocetakVracaIstiRadICommitBezResetovanjaVremena() {
        when(radovi.findByStudentIZadatakZaIzmenu(7L,5L)).thenReturn(Optional.of(rad));
        var odgovor = radService.zapocni(5L,studentAuth);
        assertEquals(8L,odgovor.rad().getId()); assertEquals(source,odgovor.izvorniCommit());
        assertEquals(LocalDateTime.of(2030,6,15,10,15),rad.getVremePreuzimanja());
        verify(radovi,never()).save(any()); verifyNoInteractions(github);
    }

    @Test
    void ponovniPocetakPosleRokaZaPredajuNeMenjaRad() {
        when(radovi.findByStudentIZadatakZaIzmenu(7L,5L)).thenReturn(Optional.of(rad));
        Clock posleRoka = Clock.fixed(Instant.parse("2030-06-15T10:05:01Z"), ZoneId.of("Europe/Belgrade"));
        RadService service = new RadService(radovi, studenti, zadaci, prijave, predaje, posleRoka, github);

        assertThrows(ConflictException.class, () -> service.zapocni(5L, studentAuth));

        assertEquals(StatusRada.PREUZET, rad.getStatus());
        assertEquals(LocalDateTime.of(2030,6,15,10,15), rad.getVremePreuzimanja());
        assertEquals(source, rad.getIzvorniCommit());
        assertNull(rad.getGithubCommit());
        assertNull(rad.getVremePredaje());
        verify(radovi, never()).save(any());
        verifyNoInteractions(github);
    }

    @ParameterizedTest
    @EnumSource(value = StatusRada.class, names = {"PREDAT", "OCENJEN"})
    void ponovniPocetakPredatogIliOcenjenogRadaNeMenjaPredaju(StatusRada status) {
        rad.setStatus(status);
        rad.setGithubCommit(submitted);
        rad.setVremePredaje(LocalDateTime.of(2030,6,15,10,20));
        when(radovi.findByStudentIZadatakZaIzmenu(7L,5L)).thenReturn(Optional.of(rad));

        assertThrows(ConflictException.class, () -> radService.zapocni(5L, studentAuth));

        assertEquals(status, rad.getStatus());
        assertEquals(LocalDateTime.of(2030,6,15,10,15), rad.getVremePreuzimanja());
        assertEquals(LocalDateTime.of(2030,6,15,10,20), rad.getVremePredaje());
        assertEquals(source, rad.getIzvorniCommit());
        assertEquals(submitted, rad.getGithubCommit());
        verify(radovi, never()).save(any());
        verifyNoInteractions(github);
    }

    @Test
    void prviPocetakCuvaIzvorniCommitIFolderObjavljenogZadatka() {
        grupa.setNaziv("Preimenovana grupa");
        grupa.getTermin().getPredmet().setNaziv("Preimenovan predmet");
        when(radovi.findByStudentIZadatakZaIzmenu(7L,5L)).thenReturn(Optional.empty());

        var odgovor = radService.zapocni(5L, studentAuth);

        assertEquals(8L, odgovor.rad().getId());
        assertEquals(StatusRada.PREUZET, odgovor.rad().getStatus());
        assertEquals(source, odgovor.izvorniCommit());
        assertEquals(source, odgovor.rad().getIzvorniCommit());
        assertEquals("pperic2126si", odgovor.nazivFoldera());
        assertEquals(LocalDateTime.of(2030,6,15,10,30), odgovor.rad().getVremePreuzimanja());
        var sacuvan = org.mockito.ArgumentCaptor.forClass(Rad.class);
        verify(radovi).save(sacuvan.capture());
        assertEquals("prof/novembar", sacuvan.getValue().getGithubRepo());
        assertEquals("Programiranje/Grupa1/pperic2126si", sacuvan.getValue().getGithubFolder());
        zadatak.setGithubCommit("f".repeat(40));
        assertEquals(source, sacuvan.getValue().getIzvorniCommit());
        assertNull(sacuvan.getValue().getGithubCommit());
        verifyNoInteractions(github);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stabloFajlIArhivaBirajuPredatiSnapshotIliRadnuVerziju(boolean radna) {
        rad.setStatus(StatusRada.PREDAT);
        rad.setGithubCommit(submitted);
        String working = "d".repeat(40);
        when(github.head("prof/novembar")).thenReturn(working);
        when(github.tree("prof/novembar", submitted, "Programiranje/Grupa1/pperic2126si"))
                .thenReturn(List.of(new GitHubService.TreeEntry("predato.java", "blob", "c".repeat(40), 7)));
        when(github.tree("prof/novembar", working, "Programiranje/Grupa1/pperic2126si"))
                .thenReturn(List.of(new GitHubService.TreeEntry("radno.java", "blob", "e".repeat(40), 5)));
        when(github.read("prof/novembar", submitted, "Programiranje/Grupa1/pperic2126si", "A.java"))
                .thenReturn(new GitHubService.FileContent("A.java", "c".repeat(40), submitted,
                        "predato".getBytes(StandardCharsets.UTF_8), false));
        when(github.read("prof/novembar", working, "Programiranje/Grupa1/pperic2126si", "A.java"))
                .thenReturn(new GitHubService.FileContent("A.java", "e".repeat(40), working,
                        "radno".getBytes(StandardCharsets.UTF_8), false));
        when(github.archive("prof/novembar", submitted, "Programiranje/Grupa1/pperic2126si"))
                .thenReturn(new byte[]{1, 2, 3});
        when(github.archive("prof/novembar", working, "Programiranje/Grupa1/pperic2126si"))
                .thenReturn(new byte[]{4, 5, 6});

        assertEquals(radna ? "radno.java" : "predato.java",
                radService.stablo(8L, radna, profesorAuth).getFirst().path());
        var content = radService.fajl(8L, "A.java", radna, profesorAuth);
        assertArrayEquals((radna ? "radno" : "predato").getBytes(StandardCharsets.UTF_8), content.content());
        assertEquals(radna ? working : submitted, content.commit());
        assertArrayEquals(radna ? new byte[]{4, 5, 6} : new byte[]{1, 2, 3},
                radService.arhiva(8L, radna, profesorAuth));
        assertEquals(submitted, rad.getGithubCommit());
        assertEquals(source, rad.getIzvorniCommit());
        verify(github, radna ? times(3) : never()).head(anyString());
        verify(radovi, never()).save(any());
    }

    @Test
    void objavaKoristiFolderGrupeICuvaSnapshot() {
        zadatakService.objavi("Z1",3L,List.of(file),List.of("src/A.java"),profesorAuth);
        assertEquals(submitted,zadatak.getGithubCommit());
        verify(github).publish(eq("prof/novembar"),eq("Programiranje/Grupa1/zadatak"),anyList(),eq(source));
    }

    @Test
    void objavaNeMozeDaZaobidjePredmetNiPrvoPreuzimanje() {
        assertThrows(ForbiddenException.class,()->zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),auth("tudji@raf.rs","PROFESOR")));
        when(radovi.findFirstByZadatak_Id(5L)).thenReturn(Optional.of(rad));
        assertThrows(ConflictException.class,()->zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth));
        verifyNoInteractions(github);
    }

    @Test
    void objavaProveravaTekuceRadovePosleCekanjaNaZakljucanZadatak() {
        when(radovi.findFirstByZadatak_Id(5L)).thenReturn(Optional.of(rad));
        assertThrows(ConflictException.class,()->zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth));
        verifyNoInteractions(github);
    }

    @Test
    void profesorMenjaRadnuVerzijuBezPromeneCommitaPredaje() {
        rad.setStatus(StatusRada.PREDAT); rad.setGithubCommit(submitted);
        IzmenaFajlaRequest request = new IzmenaFajlaRequest(); request.setPutanja("A.java");
        request.setPrethodniSha("c".repeat(40)); request.setTekst("izmena");
        when(github.edit(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn("d".repeat(40));
        assertEquals("d".repeat(40),radService.izmeniFajl(8L,request,profesorAuth));
        assertEquals(submitted,rad.getGithubCommit());
        assertThrows(ForbiddenException.class,()->radService.izmeniFajl(8L,request,studentAuth));
    }

    private static Authentication auth(String email,String role) {
        return new UsernamePasswordAuthenticationToken(email,"",List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }

    @Test
    void kolizijaRezervnogFolderaGrupeDobijaSledeciSlobodanNaziv() {
        zadatak.setGithubFolder(null); zadatak.setGithubCommit(null); zadatak.setGithubRepo(null);
        when(zadaci.pronadjiFolderZaIzmenu("prof/novembar","Novembar-2030/Programiranje/Grupa1/zadatak",5L)).thenReturn(List.of(new Zadatak()));
        when(zadaci.pronadjiFolderZaIzmenu("prof/novembar","Novembar-2030/Programiranje/Grupa1-3/zadatak",5L)).thenReturn(List.of(new Zadatak()));
        Zadatak odgovor = zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth);
        assertEquals("Novembar-2030/Programiranje/Grupa1-3-2/zadatak",odgovor.getGithubFolder());
    }

    @ParameterizedTest
    @CsvSource({"Januar,2031", "Februar,2031", "Mart,2031", "April,2031", "Maj,2031", "Jun,2031",
            "Jul,2031", "Avgust,2031", "Septembar,2031", "Oktobar,2031", "Novembar,2030", "Decembar,2030",
            "'  jUn 2  ',2031"})
    void novaObjavaOrganizujeFolderPoRokuIKalendarskojGodini(String nazivRoka, int godina) {
        when(zadaci.pronadjiPoGrupi(3L)).thenReturn(Optional.empty());
        grupa.getTermin().getRok().setNaziv(nazivRoka);
        String folder = nazivRoka.trim().replace(' ', '-') + "-" + godina + "/Programiranje/Grupa1/zadatak";

        Zadatak objavljen = zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth);

        assertSame(grupa, objavljen.getGrupa());
        assertEquals(folder, objavljen.getGithubFolder());
        assertEquals(submitted, objavljen.getGithubCommit());
        assertEquals("https://github.com/prof/novembar/tree/" + submitted + "/" + folder, objavljen.getRepoPutanja());
        verify(github).publish(eq("prof/novembar"), eq(folder), anyList(), isNull());
        verify(zadaci).pronadjiFolderZaIzmenu("prof/novembar", folder, -1L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Ispitni rok"})
    void neispravanNazivRokaOdbijaObjavuPreGitHubUpisa(String naziv) {
        zadatak.setGithubFolder(null);
        grupa.getTermin().getRok().setNaziv(naziv);

        assertThrows(ConflictException.class,
                () -> zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth));

        verifyNoInteractions(github);
        verify(zadaci, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nedostajuciRokIliSkolskaGodinaOdbijajuNovuObjavu(boolean nemaRoka) {
        zadatak.setGithubFolder(null);
        if (nemaRoka) grupa.getTermin().setRok(null);
        else grupa.getTermin().getRok().setSkolskaGodina(null);

        assertThrows(ConflictException.class,
                () -> zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth));

        verifyNoInteractions(github);
        verify(zadaci, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo", "folder", "commit"})
    void obaNacinaPocetkaOdbijajuZadatakBezObjavljenihGitHubPodataka(String nedostaje) {
        switch (nedostaje) {
            case "repo" -> zadatak.setGithubRepo(null);
            case "folder" -> zadatak.setGithubFolder(null);
            case "commit" -> zadatak.setGithubCommit(null);
        }

        assertThrows(ConflictException.class, () -> radService.preuzmi(5L, studentAuth));
        assertThrows(ConflictException.class, () -> radService.zapocni(5L, studentAuth));

        verify(radovi, never()).save(any());
        verifyNoInteractions(github);
    }

    @Test
    void zajednickaArhivaProsledjujeSveFiltereISortiraneSacuvaneVerzije() {
        rad.setStatus(StatusRada.PREDAT); rad.setGithubCommit(submitted);
        Rad ocenjen = new Rad(); ocenjen.setId(6L); ocenjen.setStatus(StatusRada.OCENJEN);
        ocenjen.setGithubRepo("prof/drugi-rok"); ocenjen.setGithubFolder("Jun-2031/Programiranje/Grupa2/ana");
        ocenjen.setGithubCommit(source);
        Rad preuzet = new Rad(); preuzet.setId(9L); preuzet.setStatus(StatusRada.PREUZET);
        when(radovi.findByPredmetIdAndFilters(4L, null, 3L, 2L, 5L)).thenReturn(List.of(rad, preuzet, ocenjen));
        List<GitHubService.ArchiveSource> izvori = List.of(
                new GitHubService.ArchiveSource(6L, ocenjen.getGithubRepo(), source, ocenjen.getGithubFolder()),
                new GitHubService.ArchiveSource(8L, rad.getGithubRepo(), submitted, rad.getGithubFolder()));
        byte[] zip = {1,2,3};
        when(github.archiveMany(izvori)).thenReturn(zip);

        assertArrayEquals(zip, radService.arhivaPredmeta(4L, null, 3L, 2L, 5L, profesorAuth));

        verify(radovi).findByPredmetIdAndFilters(4L, null, 3L, 2L, 5L);
        verify(github).archiveMany(izvori);
        verify(github, never()).head(anyString());
        verify(radovi, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusRada.class, names = {"PREDAT", "OCENJEN"})
    void arhivaPrihvataObaZavrsenaStatusa(StatusRada status) {
        rad.setStatus(status); rad.setGithubCommit(submitted);
        when(radovi.findByPredmetIdAndFilters(4L, status, null, null, null)).thenReturn(List.of(rad));
        when(github.archiveMany(anyList())).thenReturn(new byte[]{1});

        assertArrayEquals(new byte[]{1}, radService.arhivaPredmeta(4L, status, null, null, null, profesorAuth));
        verify(radovi).findByPredmetIdAndFilters(4L, status, null, null, null);
    }

    @Test
    void arhivaOdbijaTudjegProfesoraIPreuzeteRadovePreUpita() {
        assertThrows(ForbiddenException.class,
                () -> radService.arhivaPredmeta(4L, null, null, null, null, auth("tudji@raf.rs","PROFESOR")));
        assertThrows(BadRequestException.class,
                () -> radService.arhivaPredmeta(4L, StatusRada.PREUZET, null, null, null, profesorAuth));
        verifyNoInteractions(radovi, github);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nemaArhiveAkoFilteriNeVratiZavrseneRadove(boolean samoPreuzet) {
        when(radovi.findByPredmetIdAndFilters(4L, null, null, null, null))
                .thenReturn(samoPreuzet ? List.of(rad) : List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> radService.arhivaPredmeta(4L, null, null, null, null, profesorAuth));
        verifyNoInteractions(github);
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo", "folder", "commit"})
    void arhivaOdbijaRadBezSacuvaneGitHubVerzije(String nedostaje) {
        rad.setStatus(StatusRada.PREDAT); rad.setGithubCommit(submitted);
        switch (nedostaje) {
            case "repo" -> rad.setGithubRepo(null);
            case "folder" -> rad.setGithubFolder(null);
            case "commit" -> rad.setGithubCommit(null);
        }
        when(radovi.findByPredmetIdAndFilters(4L, null, null, null, null)).thenReturn(List.of(rad));

        assertThrows(ConflictException.class,
                () -> radService.arhivaPredmeta(4L, null, null, null, null, profesorAuth));
        verifyNoInteractions(github);
    }

    @Test
    void kolizijaRezervnogStudentskogFolderaDobijaSledeciSlobodanNaziv() {
        when(radovi.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1/pperic2126si")).thenReturn(List.of(new Rad()));
        when(radovi.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1/pperic2126si-7")).thenReturn(List.of(new Rad()));
        var odgovor = radService.zapocni(5L,studentAuth);
        assertEquals("pperic2126si-7-2",odgovor.nazivFoldera());
    }
}
