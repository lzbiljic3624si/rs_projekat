package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.model.Profesor;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseUtils {

    // rn1923 -> smer, dve cifre godine, broj
    private static final Pattern INDEKS_PATTERN = Pattern.compile("^([A-Za-z]+)(\\d{2})(\\d{1,6})$");
    // ppetrovic1220rn@raf.rs -> ime, broj, dve cifre godine, smer
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z]+(\\d{1,6})(\\d{2})([A-Za-z]+)@raf\\.rs$");
    // dvoslovni pocetak imena koji se u email-u pise sa dva slova (Djordje, Dzemal, Ljiljana, Njegos)
    private static final List<String> DVOSLOVNI_INICIJALI = List.of("dj", "dz", "lj", "nj");

    private ParseUtils() {
    }

    /*
     * dobija indeks oblika rn1923 i vraca niz stringova [RN, 19, 23]
     * ili null ako string nije u ispravnom obliku
     */
    public static String[] parseIndeks(String indeksShort) {
        if (indeksShort == null) return null;
        Matcher m = INDEKS_PATTERN.matcher(indeksShort.trim());
        if (!m.matches()) return null;
        return new String[] {m.group(1).toUpperCase(), m.group(2), m.group(3)};
    }

    /*
     * dobija email studenta na primer ppetrovic1220rn@raf.rs, a vraca indeks u obliku [RN, 20, 12]
     * ili null ako email nije u ispravnom obliku
     */
    public static String[] parseEmail(String studEmail) {
        if (studEmail == null) return null;
        Matcher m = EMAIL_PATTERN.matcher(studEmail.trim());
        if (!m.matches()) return null;
        return new String[] {m.group(3).toUpperCase(), m.group(2), m.group(1)};
    }

    /*
     * pravi email koji profesor treba da ima: inicijal imena + prezime @raf.rs
     * Petar Petrović -> ppetrovic@raf.rs, Đorđe Petrović -> djpetrovic@raf.rs,
     * Njegoš Petrović -> njpetrovic@raf.rs, Ljiljana Petrović -> ljpetrovic@raf.rs
     * ili null ako ime ili prezime fali
     */
    public static String profesorEmail(String ime, String prezime) {
        String osnova = inicijalIPrezime(ime, prezime);
        return osnova == null ? null : osnova + "@raf.rs";
    }

    /*
     * pravi studentski email: inicijal + prezime + broj + dve cifre godine + smer @raf.rs
     * Marko Nikolić, 12, 2023, RN -> mnikolic1223rn@raf.rs (obrnuto od parseEmail)
     * ili null ako nesto fali
     */
    public static String studentEmail(String ime, String prezime, int broj, int godUpisa, String smer) {
        String osnova = inicijalIPrezime(ime, prezime);
        String s = latinica(smer);
        if (osnova == null || s.isEmpty()) return null;
        return osnova + broj + String.format("%02d", godUpisa % 100) + s + "@raf.rs";
    }

    // "Đorđe", "Petrović" -> "djpetrovic"; "Petar", "Petrović" -> "ppetrovic"
    private static String inicijalIPrezime(String ime, String prezime) {
        String i = latinica(ime);
        String p = latinica(prezime);
        if (i.isEmpty() || p.isEmpty()) return null;
        String inicijal = DVOSLOVNI_INICIJALI.contains(i.substring(0, Math.min(2, i.length())))
                ? i.substring(0, 2)
                : i.substring(0, 1);
        return inicijal + p;
    }

    /*
     * dobija email profesora (npr. djpetrovic@raf.rs) i listu profesora iz baze,
     * a vraca profesora sa punim imenom (Đorđe Petrović).
     * Iz samog email-a ne moze da se zna da li je djpetrovic = "dj" + "petrovic" ili "d" + "jpetrovic",
     * zato se za svakog profesora napravi ocekivani email i uporedi.
     * Vraca null ako nema poklapanja ili ako se poklapa vise profesora (npr. Petar i Pavle Petrović).
     */
    // ponytail: prolazi kroz sve profesore (O(n)), dovoljno za par stotina; za vise filtrirati upitom po prezimenu
    public static Profesor pronadjiProfesora(String profEmail, Collection<Profesor> profesori) {
        if (profEmail == null || profesori == null) return null;
        String email = profEmail.trim().toLowerCase();
        List<Profesor> pogodci = profesori.stream()
                .filter(p -> email.equals(profesorEmail(p.getIme(), p.getPrezime())))
                .toList();
        return pogodci.size() == 1 ? pogodci.get(0) : null;
    }

    // "Đorđe" -> "djordje", "Petrović" -> "petrovic", "Džemal" -> "dzemal"; sve sto nije slovo a-z se brise
    private static String latinica(String s) {
        if (s == null) return "";
        String bezDj = s.trim().toLowerCase().replace("đ", "dj");
        return Normalizer.normalize(bezDj, Normalizer.Form.NFD) // č -> c + kvacica
                .replaceAll("\\p{M}", "")                         // skida kvacice
                .replaceAll("[^a-z]", "");
    }
}
