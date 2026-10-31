package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.model.Profesor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParseUtilsTest {

    @Test
    void parseIndeks() {
        assertArrayEquals(new String[] {"SI", "24", "36"}, ParseUtils.parseIndeks("3624si"));
        assertArrayEquals(new String[] {"RI", "20", "105"}, ParseUtils.parseIndeks(" 10520RI "));
        assertArrayEquals(new String[] {"RN", "05", "1"}, ParseUtils.parseIndeks("105rn"));
        assertArrayEquals(new String[] {"RN", "24", "123456"}, ParseUtils.parseIndeks("12345624rn"));
        assertNull(ParseUtils.parseIndeks(null));
        assertNull(ParseUtils.parseIndeks(""));
        assertNull(ParseUtils.parseIndeks("rn1923"));
        assertNull(ParseUtils.parseIndeks("24si"));
        assertNull(ParseUtils.parseIndeks("123456724rn"));
        assertNull(ParseUtils.parseIndeks("36/24si"));
        assertNull(ParseUtils.parseIndeks("1923"));
        assertNull(ParseUtils.parseIndeks("rn1"));
        assertNull(ParseUtils.parseIndeks("1/2023"));
    }

    @Test
    void parseEmail() {
        assertArrayEquals(new String[] {"RN", "20", "12"}, ParseUtils.parseEmail("ppetrovic1220rn@raf.rs"));
        assertNull(ParseUtils.parseEmail(null));
        assertNull(ParseUtils.parseEmail("ppetrovic1220rn@gmail.com"));
        assertNull(ParseUtils.parseEmail("student1@example.com"));
        assertNull(ParseUtils.parseEmail("ppetrovic@raf.rs"));
    }

    @Test
    void profesorEmail() {
        assertEquals("ppetrovic@raf.rs", ParseUtils.profesorEmail("Petar", "Petrović"));
        assertEquals("djpetrovic@raf.rs", ParseUtils.profesorEmail("Đorđe", "Petrović"));
        assertEquals("djpetrovic@raf.rs", ParseUtils.profesorEmail("Djordje", "Petrovic"));
        assertEquals("njpetrovic@raf.rs", ParseUtils.profesorEmail("Njegoš", "Petrović"));
        assertEquals("ljpetrovic@raf.rs", ParseUtils.profesorEmail("Ljiljana", "Petrović"));
        assertEquals("dzpetrovic@raf.rs", ParseUtils.profesorEmail("Džemal", "Petrović"));
        assertEquals("cdjokic@raf.rs", ParseUtils.profesorEmail("Čedomir", "Đokić"));
        assertEquals("szivkovic@raf.rs", ParseUtils.profesorEmail(" Šaban ", "Živković"));
        assertEquals("dpetrovic@raf.rs", ParseUtils.profesorEmail("Dragan", "Petrović"));
        assertNull(ParseUtils.profesorEmail(null, "Petrović"));
        assertNull(ParseUtils.profesorEmail("Petar", ""));
    }

    @Test
    void profesorEmailProduzavaImeDokNeNadjeSlobodnuAdresu() {
        Set<String> zauzeti = new HashSet<>();
        assertEquals("mpetrovic@raf.rs", dodajProfesora("Marija", "Petrovic", zauzeti));
        assertEquals("mapetrovic@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("marpetrovic@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("maripetrovic@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("marinpetrovic@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("marinapetrovic@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("marinapetrovic1@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
        assertEquals("marinapetrovic2@raf.rs", dodajProfesora("Marina", "Petrovic", zauzeti));
    }

    @Test
    void profesorEmailCuvaNormalizacijuIDvoslovneInicijale() {
        Set<String> zauzeti = new HashSet<>();
        assertEquals("djpetrovic@raf.rs", dodajProfesora(" Đorđe ", "Petrović", zauzeti));
        assertEquals("djopetrovic@raf.rs", dodajProfesora("Djordje", "Petrovic", zauzeti));
        assertEquals("apetrovic@raf.rs", dodajProfesora("A", "Petrovic", zauzeti));
        assertEquals("apetrovic1@raf.rs", dodajProfesora("A", "Petrovic", zauzeti));
        assertNull(ParseUtils.profesorEmail(null, "Petrovic", zauzeti::contains));
        assertNull(ParseUtils.profesorEmail("Marina", "123", zauzeti::contains));
    }

    @Test
    void pronadjiProfesoraKoristiSacuvaniEmailPosleKolizije() {
        Profesor marija = profesor("Marija", "Petrovic");
        marija.setEmail("mpetrovic@raf.rs");
        Profesor marina = profesor("Marina", "Petrovic");
        marina.setEmail("mapetrovic@raf.rs");
        Profesor drugaMarina = profesor("Marina", "Petrovic");
        drugaMarina.setEmail("marinapetrovic1@raf.rs");
        List<Profesor> baza = List.of(marija, marina, drugaMarina);

        assertSame(marija, ParseUtils.pronadjiProfesora("mpetrovic@raf.rs", baza));
        assertSame(marina, ParseUtils.pronadjiProfesora(" MAPetrovic@raf.rs ", baza));
        assertSame(drugaMarina, ParseUtils.pronadjiProfesora("marinapetrovic1@raf.rs", baza));
    }

    private static String dodajProfesora(String ime, String prezime, Set<String> zauzeti) {
        String email = ParseUtils.profesorEmail(ime, prezime, zauzeti::contains);
        zauzeti.add(email);
        return email;
    }

    @Test
    void studentEmail() {
        assertEquals("mnikolic1223rn@raf.rs", ParseUtils.studentEmail("Marko", "Nikolić", 12, 2023, "RN"));
        assertEquals("ljpopovic505ri@raf.rs", ParseUtils.studentEmail("Ljubica", "Popović", 5, 2005, "RI"));
        assertNull(ParseUtils.studentEmail("Marko", "Nikolić", 12, 2023, null));
        // parseEmail mora da vrati ono od cega je email napravljen
        assertArrayEquals(new String[] {"RN", "23", "12"},
                ParseUtils.parseEmail(ParseUtils.studentEmail("Đorđe", "Jovanović", 12, 2023, "RN")));
    }

    @Test
    void pronadjiProfesora() {
        Profesor djordje = profesor("Đorđe", "Petrović");
        Profesor dragan = profesor("Dragan", "Jpetrović"); // d + jpetrovic, ne sme da se pomesa sa dj + petrovic
        Profesor ljiljana = profesor("Ljiljana", "Petrović");
        List<Profesor> baza = List.of(djordje, ljiljana, profesor("Petar", "Petrović"));

        assertSame(djordje, ParseUtils.pronadjiProfesora("djpetrovic@raf.rs", baza));
        assertSame(ljiljana, ParseUtils.pronadjiProfesora(" LJPetrovic@raf.rs ", baza));
        assertSame(dragan, ParseUtils.pronadjiProfesora("djpetrovic@raf.rs", List.of(dragan)));
        assertNull(ParseUtils.pronadjiProfesora("djpetrovic@raf.rs", List.of(djordje, dragan))); // dvosmisleno
        assertNull(ParseUtils.pronadjiProfesora("mmarkovic@raf.rs", baza));
        assertNull(ParseUtils.pronadjiProfesora("ppetrovic1220rn@raf.rs", baza));
        assertNull(ParseUtils.pronadjiProfesora(null, baza));
    }

    private static Profesor profesor(String ime, String prezime) {
        Profesor p = new Profesor();
        p.setIme(ime);
        p.setPrezime(prezime);
        p.setEmail(ParseUtils.profesorEmail(ime, prezime));
        return p;
    }
}
