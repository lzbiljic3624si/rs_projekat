package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.model.Profesor;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseUtils {

    // 3624si -> broj, dve cifre godine, smer
    private static final Pattern INDEKS_PATTERN = Pattern.compile("^(\\d{1,6})(\\d{2})([A-Za-z]+)$");
    // ppetrovic1220rn@raf.rs -> ime, broj, dve cifre godine, smer
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z]+(\\d{1,6})(\\d{2})([A-Za-z]+)@raf\\.rs$");
    // dvoslovni pocetak imena koji se u email-u pise sa dva slova (Djordje, Dzemal, Ljiljana, Njegos)
    private static final List<String> DVOSLOVNI_INICIJALI = List.of("dj", "dz", "lj", "nj");

    private ParseUtils() {
    }

    /*
     * dobija indeks oblika 3624si i vraca niz stringova [SI, 24, 36]
     * ili null ako string nije u ispravnom obliku
     */
    public static String[] parseIndeks(String indeksShort) {
        if (indeksShort == null) return null;
        Matcher m = INDEKS_PATTERN.matcher(indeksShort.trim());
        if (!m.matches()) return null;
        return new String[] {m.group(3).toUpperCase(), m.group(2), m.group(1)};
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

    public static String profesorEmail(String ime, String prezime, Predicate<String> emailZauzet) {
        String osnova = inicijalIPrezime(ime, prezime);
        if (osnova == null) return null;
        String i = latinica(ime);
        String p = latinica(prezime);
        for (int duzina = osnova.length() - p.length(); duzina <= i.length(); duzina++) {
            String email = i.substring(0, duzina) + p + "@raf.rs";
            if (!emailZauzet.test(email)) return email;
        }
        for (int broj = 1; ; broj++) {
            String email = i + p + broj + "@raf.rs";
            if (!emailZauzet.test(email)) return email;
        }
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

    // Poredi sacuvane adrese, jer zbog kolizija email ne mora da bude inicijal + prezime.
    // ponytail: prolazi kroz sve profesore (O(n)), dovoljno za par stotina; za vise filtrirati upitom po prezimenu
    public static Profesor pronadjiProfesora(String profEmail, Collection<Profesor> profesori) {
        if (profEmail == null || profesori == null) return null;
        String email = profEmail.trim();
        List<Profesor> pogodci = profesori.stream()
                .filter(p -> email.equalsIgnoreCase(p.getEmail()))
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
