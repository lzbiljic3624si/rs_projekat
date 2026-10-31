# API kreiranje podataka — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Omoguciti proveru programa iskljucivo kroz API, sa kreiranjem svih 12 entiteta.

**Architecture:** Zadrzati postojece kontrolere, DTO mappere, repozitorijume i poslovne operacije. Dodati NalogController/NalogService za profile i povezane korisnike, i PredajePredmetController/PredajePredmetService za dodelu predmeta.

**Tech Stack:** Postojeci Spring Boot, Spring Security JWT, JPA, Jakarta Validation, JUnit i Mockito; bez novih zavisnosti.

**Spec:** Korisnicki zahtevi u ovoj sesiji: bez formi, oba tipa naloga preko API-ja, automatski Korisnik, pocetna lozinka `lozinka123`, profesor moze da dodeli bilo kog profesora predmetu, uslov predmeta nije obavezan i moze biti nula.

## Global Constraints

- Kreiranje naloga i dodela predmeta zahtevaju postojeci profesorski JWT; prvi login koristi Seeder profesora.
- POST `/api/nalozi/studenti` prima ime, prezime, smer, brIndeksa (`12/2023`) i godUpisa; vraca StudentResponse.
- POST `/api/nalozi/profesori` prima ime i prezime; vraca ProfesorResponse.
- Mejl i hash se generisu na serveru; profil i Korisnik se cuvaju u jednoj transakciji.
- Pocetna lozinka `lozinka123` sluzi demonstraciji. Hash i lozinka se ne vracaju u DTO odgovorima.
- POST `/api/predaje-predmet` koristi postojeci PredajePredmetRequest (nazivPredmeta, emailProfesora) i vraca PredajePredmetResponse.
- Bez izmena korisnikovog notes.txt i bez commit/push operacija u ovom zadatku.

## Review Focus

- Kolizije generisanih mejlova sa postojecim profilima i korisnicima, ukljucujuci razliku u velicini slova.
- Duplikat indeksa ili profesor–predmet veze vraca 409; nepostojeca veza vraca 404.
- Nepodudarni broj/godina indeksa i imena bez slova odbijaju se sa 400.
- Anonimni i studentski zahtevi ne kreiraju naloge niti dodeljuju predmete.
- Nijedan odgovor ne iznosi hash lozinke; oba tipa naloga imaju ispravan profil i ulogu za login.

### Task 1: Opcioni uslov predmeta

- [x] Napisati i pokrenuti RED testove PredmetServiceTest i ControllerHttpTest za izostavljen/null/prazan/0 uslov.
- [x] Ukloniti obaveznost iz PredmetRequest i normalizovati prazan/0 uslov na null u PredmetService.save.
- [x] Pokrenuti `mvn -o test`: 204 testa prosla.

### Task 2: Nalozi i dodela predmeta

- [x] Dodati NalogServiceTest i PredajePredmetServiceTest; RED pokazao nedostajucu funkcionalnost.
- [x] Implementirati gore navedene servise/kontrolere, prilagoditi StudentRequest/ProfesorRequest i dopuniti repozitorijume za provere duplikata.
- [x] Dodati KreiranjeApiHttpTest za 201/400/401/403, JWT pristup, DTO odgovore i opcioni uslov.
- [x] Pokrenuti ciljne testove, pa kompletan `mvn -o test`: 236 testova, bez gresaka.

### Task 3: Uputstvo i zavrsna provera

- [x] Napisati docs/API.md sa svim modelima, login-om, JSON primerima i kompletnim redosledom unosa/testiranja.
- [x] Pregledati kompletan diff i proveriti `git diff --check`; jedan nezavisan pregled novih izmena.
- [x] Prijaviti rezultat testova i ogranicenje da se demonstraciona lozinka ne koristi u produkciji.

## Zavrsna provera

- Nezavisan pregled nasao je jedan P2: predugacka imena/mejlovi nisu odbijeni na granici. Dodata @Size validacija i ogranicenje generisanog mejla; sest HTTP regresionih testova potvrdjeno RED, zatim GREEN.
- Studentski DTO dopunjen je smerom; HTTP provera potvrdila da nov profesor moze da se prijavi, svojim pravim JWT-om napravi studenta, a zatim i student moze da se prijavi.
- Zavrsno `mvn -o test`: 236/236; provera nije koristila stvarnu MySQL bazu.
