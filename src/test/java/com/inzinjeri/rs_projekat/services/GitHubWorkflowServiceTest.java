package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.IzmenaFajlaRequest;
import com.inzinjeri.rs_projekat.exceptions.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.*;
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
        Rok rok = new Rok(); rok.setId(1L); rok.setRepoPutanja("https://github.com/prof/novembar");
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
        when(zadaci.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1/zadatak",5L)).thenReturn(List.of(new Zadatak()));
        when(zadaci.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1-3/zadatak",5L)).thenReturn(List.of(new Zadatak()));
        Zadatak odgovor = zadatakService.objavi("Z1",3L,List.of(file),List.of("A.java"),profesorAuth);
        assertEquals("Programiranje/Grupa1-3-2/zadatak",odgovor.getGithubFolder());
    }

    @Test
    void kolizijaRezervnogStudentskogFolderaDobijaSledeciSlobodanNaziv() {
        when(radovi.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1/pperic2126si")).thenReturn(List.of(new Rad()));
        when(radovi.pronadjiFolderZaIzmenu("prof/novembar","Programiranje/Grupa1/pperic2126si-7")).thenReturn(List.of(new Rad()));
        var odgovor = radService.zapocni(5L,studentAuth);
        assertEquals("pperic2126si-7-2",odgovor.nazivFoldera());
    }
}
