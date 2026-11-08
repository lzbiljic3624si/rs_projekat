package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Komentar;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.repositories.KomentarRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KomentarServiceTest {
    private final KomentarRepository komentari = mock(KomentarRepository.class);
    private final RadRepository radovi = mock(RadRepository.class);
    private final PristupService pristup = mock(PristupService.class);
    private final KomentarService service = new KomentarService(komentari, radovi, pristup);
    private final Authentication profesor = auth("PROFESOR");
    private final Rad rad = new Rad();
    private final Komentar komentar = new Komentar();

    @BeforeEach
    void podaci() {
        rad.setId(9L);
        komentar.setId(12L); komentar.setRad(rad);
        komentar.setKomentar("Greska"); komentar.setPutanjaFajla("src/Main.java"); komentar.setBrojLinije(17);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(komentari.findById(12L)).thenReturn(Optional.of(komentar));
        when(pristup.profesorPredajePredmetRada(profesor, 9L)).thenReturn(true);
    }

    @Test
    void dodavanjeCuvaTekstFajlILinijuNaPravomRadu() {
        when(komentari.save(any())).thenAnswer(i -> i.getArgument(0));

        Komentar sacuvan = service.dodaj(9L, "  Proveri granicu  ", "  src/Main.java  ", 17, profesor);

        assertEquals("Proveri granicu", sacuvan.getKomentar());
        assertEquals("src/Main.java", sacuvan.getPutanjaFajla());
        assertEquals(17, sacuvan.getBrojLinije());
        assertSame(rad, sacuvan.getRad());
        verify(komentari).save(sacuvan);
    }

    @Test
    void prihvataMaksimalneDuzineIPrvuLiniju() {
        when(komentari.save(any())).thenAnswer(i -> i.getArgument(0));

        Komentar sacuvan = service.dodaj(9L, "x".repeat(2000), "p".repeat(500), 1, profesor);

        assertEquals(2000, sacuvan.getKomentar().length());
        assertEquals(500, sacuvan.getPutanjaFajla().length());
        assertEquals(1, sacuvan.getBrojLinije());
    }

    @ParameterizedTest
    @MethodSource("neispravniKomentari")
    void neispravniPodaciSeNeCuvaju(String tekst, String putanja, Integer linija) {
        assertThrows(BadRequestException.class, () -> service.dodaj(9L, tekst, putanja, linija, profesor));
        assertThrows(BadRequestException.class, () -> service.izmeni(12L, tekst, putanja, linija, profesor));
        assertEquals("Greska", komentar.getKomentar());
        assertEquals("src/Main.java", komentar.getPutanjaFajla());
        assertEquals(17, komentar.getBrojLinije());
        verify(komentari, never()).save(any());
    }

    static Stream<Arguments> neispravniKomentari() {
        return Stream.of(
                Arguments.of(null, "Main.java", 1),
                Arguments.of("", "Main.java", 1),
                Arguments.of(" \t\n", "Main.java", 1),
                Arguments.of("x".repeat(2001), "Main.java", 1),
                Arguments.of("Greska", null, 1),
                Arguments.of("Greska", "", 1),
                Arguments.of("Greska", " \t\n", 1),
                Arguments.of("Greska", "p".repeat(501), 1),
                Arguments.of("Greska", "Main.java", null),
                Arguments.of("Greska", "Main.java", 0),
                Arguments.of("Greska", "Main.java", -1));
    }

    @ParameterizedTest
    @MethodSource("bezPravaIzmene")
    void dodavanjeIzmenaIBrisanjeBezPravaNeMenjajuPodatke(Authentication auth) {
        assertThrows(ForbiddenException.class, () -> service.dodaj(9L, "Greska", "Main.java", 1, auth));
        assertThrows(ForbiddenException.class, () -> service.izmeni(12L, "Izmenjeno", "Helper.java", 5, auth));
        assertThrows(ForbiddenException.class, () -> service.obrisi(12L, auth));
        assertEquals("Greska", komentar.getKomentar());
        assertEquals("src/Main.java", komentar.getPutanjaFajla());
        assertEquals(17, komentar.getBrojLinije());
        verify(komentari, never()).save(any());
        verify(komentari, never()).delete(any(Komentar.class));
    }

    static Stream<Arguments> bezPravaIzmene() {
        return Stream.of(
                Arguments.of((Authentication) null),
                Arguments.of(auth("STUDENT")),
                Arguments.of(auth("DRUGA")),
                Arguments.of(UsernamePasswordAuthenticationToken.unauthenticated("profesor", "")),
                Arguments.of(new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))),
                Arguments.of(UsernamePasswordAuthenticationToken.authenticated("drugi.profesor", "",
                        List.of(new SimpleGrantedAuthority("ROLE_PROFESOR")))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vlasnikIProfesorMoguDaCitajuListu(boolean vlasnik) {
        Authentication auth = auth(vlasnik ? "STUDENT" : "PROFESOR");
        when(pristup.studentJeVlasnikRada(auth, 9L)).thenReturn(vlasnik);
        when(pristup.profesorPredajePredmetRada(auth, 9L)).thenReturn(!vlasnik);
        when(komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(9L)).thenReturn(List.of(komentar));

        assertEquals(List.of(komentar), service.zaRad(9L, auth));
    }

    @Test
    void radBezKomentaraVracaPraznuListu() {
        assertTrue(service.zaRad(9L, profesor).isEmpty());
    }

    @Test
    void tudjiKorisnikNeCitaListu() {
        assertThrows(ForbiddenException.class, () -> service.zaRad(9L, auth("STUDENT")));
        verify(komentari, never()).findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(anyLong());
    }

    @Test
    void nepostojeciRadNeDozvoljavaDodavanjeNiListanje() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.dodaj(999L, "Greska", "Main.java", 1, profesor));
        assertThrows(ResourceNotFoundException.class, () -> service.zaRad(999L, profesor));
        verify(komentari, never()).save(any());
    }

    @Test
    void profesorPredmetaBriseKomentar() {
        service.obrisi(12L, profesor);
        verify(komentari).delete(komentar);
    }

    @Test
    void nepostojeciKomentarNeMozeDaSeObrise() {
        assertThrows(ResourceNotFoundException.class, () -> service.obrisi(999L, profesor));
        verify(komentari, never()).delete(any(Komentar.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vlasnikIProfesorMoguDaCitajuPojedinacanKomentar(boolean vlasnik) {
        Authentication auth = auth(vlasnik ? "STUDENT" : "PROFESOR");
        when(pristup.studentJeVlasnikRada(auth, 9L)).thenReturn(vlasnik);
        when(pristup.profesorPredajePredmetRada(auth, 9L)).thenReturn(!vlasnik);

        assertSame(komentar, service.findById(12L, auth));
    }

    @Test
    void tudjiKorisnikNeCitaPojedinacanKomentar() {
        assertThrows(ForbiddenException.class, () -> service.findById(12L, auth("STUDENT")));
    }

    @Test
    void nepostojeciKomentarVracaGreskuPriCitanju() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(999L, profesor));
    }

    @Test
    void profesorMenjaSadrzajPostojecegKomentaraBezPromeneIdIRada() {
        when(komentari.save(any())).thenAnswer(i -> i.getArgument(0));

        Komentar izmenjen = service.izmeni(12L, "  Ispravljen opis  ", "  src/Helper.java  ", 5, profesor);

        assertEquals(12L, izmenjen.getId());
        assertSame(rad, izmenjen.getRad());
        assertEquals("Ispravljen opis", izmenjen.getKomentar());
        assertEquals("src/Helper.java", izmenjen.getPutanjaFajla());
        assertEquals(5, izmenjen.getBrojLinije());
        verify(komentari).save(komentar);
    }

    @Test
    void izmenaPrihvataMaksimalneDuzineIPrvuLiniju() {
        when(komentari.save(any())).thenAnswer(i -> i.getArgument(0));

        Komentar izmenjen = service.izmeni(12L, "x".repeat(2000), "p".repeat(500), 1, profesor);

        assertEquals("x".repeat(2000), izmenjen.getKomentar());
        assertEquals("p".repeat(500), izmenjen.getPutanjaFajla());
        assertEquals(1, izmenjen.getBrojLinije());
    }

    @Test
    void nepostojeciKomentarNeMozeDaSeIzmeni() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.izmeni(999L, "Ispravljen opis", "Main.java", 1, profesor));
        verify(komentari, never()).save(any());
    }

    private static Authentication auth(String uloga) {
        return UsernamePasswordAuthenticationToken.authenticated("korisnik", "",
                List.of(new SimpleGrantedAuthority("ROLE_" + uloga)));
    }
}
