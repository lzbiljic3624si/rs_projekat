# Unos i provera podataka preko API-ja

Program se proverava JSON zahtevima, npr. iz Postman-a ili IntelliJ HTTP Client-a. Forma nije potrebna.

Podrazumevana adresa pokrenutog programa je `http://localhost:6768`. Svaki zahtev sa JSON telom salje `Content-Type: application/json`.

## Prvo prijavljivanje

Seeder pri inicijalnom pokretanju pravi profesorski nalog `djpetrovic@raf.rs`, sa lozinkom `lozinka123`.

`POST /api/auth/login`

```json
{"email":"djpetrovic@raf.rs","lozinka":"lozinka123"}
```

Odgovor sadrzi `token`, `istice` i `uloga`. Za svaki zasticeni zahtev postaviti zaglavlje:

```http
Authorization: Bearer OVDE_IDE_TOKEN_IZ_LOGIN_ODGOVORA
```

Kreiranje naloga i dodela profesora predmetu zahtevaju profesorski token. Novi profesor moze odmah da se prijavi i koristi ista prava. Student ne moze da kreira naloge niti da sebi dodeli profesorsku ulogu.

## Pokrivenost modela

| Model | Kako se kreira/popunjava |
|---|---|
| Profesor | `POST /api/nalozi/profesori` |
| Student | `POST /api/nalozi/studenti` |
| Korisnik | Automatski u istoj transakciji sa Student/Profesor profilom |
| Predmet | `POST /api/predmet/add` |
| PredajePredmet | `POST /api/predaje-predmet` |
| Rok | `POST /api/raspored/rokovi` |
| Ucionica | `POST /api/raspored/ucionice` |
| Termin | `POST /api/raspored/termini` |
| Grupa | `POST /api/raspored/grupe` |
| Zadatak | `POST /api/zadaci` |
| PrijavljenStudent | Student: `POST /api/prijave/grupa/{grupaId}` |
| Rad | Student preuzima zadatak; zatim predaje resenje, a profesor ocenjuje |

`Uloga` je enum sa vrednostima `STUDENT` i `PROFESOR`; dodeljuje se prema vrsti kreiranog naloga. ID-eve generise baza. Lozinke se cuvaju kao hash i ne vracaju se u profilima.

## Novi nalozi

### Profesor

`POST /api/nalozi/profesori` — profesorski token

```json
{"ime":"Marija","prezime":"Petrovic"}
```

Odgovor je `201 Created` sa `id`, `ime`, `prezime`, `email`. Ako je osnovna adresa slobodna, mejl je `mpetrovic@raf.rs`. Za sledecu osobu Marina Petrovic, dobija se `mapetrovic@raf.rs`, zatim se po potrebi dodaju naredna slova imena. Kada je celo ime zauzeto, dodaju se brojevi: `marinapetrovic1@raf.rs`, `marinapetrovic2@raf.rs` itd. Porede se adrese svih korisnickih naloga i oba tipa profila, bez razlike u velicini slova.

### Student

`POST /api/nalozi/studenti` — profesorski token

```json
{"ime":"Ana","prezime":"Ilic","smer":"RN","brIndeksa":"100/2026","godUpisa":2026}
```

Odgovor je `201 Created` sa `id`, `ime`, `prezime`, `smer`, `brIndeksa`, `godUpisa`, `email`. Za ovaj primer generisani mejl je `ailic10026rn@raf.rs`.

Indeks se salje u obliku `broj/godina`, kao u Seeder-u. Broj ima najvise sest cifara i mora biti veci od nule; godina se mora slagati sa `godUpisa`. Smer se cuva velikim slovima. Vec postojeci indeks ili studentski mejl se odbija, umesto kreiranja drugog naloga.

Oba tipa novih naloga imaju pocetnu lozinku **`lozinka123`**, kao demonstracioni nalozi. Mejl i lozinku ne treba slati u zahtevima za kreiranje; odredjuje ih server. Zajednicka lozinka je namenjena demonstraciji, ne produkciji.

Ime i prezime imaju najvise 255 znakova, a generisani mejl najvise 254. Ako nije moguce napraviti mejl iz unetih imena ili bi bio predugacak, API vraca `400` bez upisa naloga.

## Predmet i dodela profesora

`POST /api/predmet/add` — profesorski token

```json
{"naziv":"API demonstracija","jezik":"Java","uslov":0}
```

`uslov` je opcioni tekst. Izostavljeno polje, `null`, prazan tekst, `0` i `"0"` znace da predmet nema uslov; cuva se `null`. Ako uslov postoji, salje se tekst, npr. `"Objektno orijentisano programiranje"`.

`POST /api/predaje-predmet` — profesorski token

```json
{"nazivPredmeta":"API demonstracija","emailProfesora":"mpetrovic@raf.rs"}
```

Prijavljeni profesor moze da dodeli sebe ili drugog postojeceg profesora. Koristiti mejl iz odgovora kreiranja, koji zbog kolizija moze biti drugaciji od osnovnog oblika. Odgovor je `201 Created` sa `id`, `profesor` i `predmet`. Ponovljena ista veza vraca `409`.

## Raspored i zadatak

U narednim primerima `6` predstavlja primer ID-a vracenog prethodnim zahtevom; zameniti ga stvarnim ID-em odgovarajuceg predmeta, roka, ucionice, termina, grupe ili zadatka. Za termine, grupe i zadatke koristiti token profesora dodeljenog tom predmetu. Datum termina mora biti danasnji ili buduci.

| Zahtev | JSON telo |
|---|---|
| `POST /api/raspored/rokovi` | `{"naziv":"API rok","skolskaGodina":2026}` |
| `POST /api/raspored/ucionice` | `{"naziv":"API Lab"}` |
| `POST /api/raspored/termini` | `{"datum":"2027-06-15","vremePocetak":"10:00","vremeKraja":"12:00","ucionicaId":6,"predmetId":6,"rokId":6}` |
| `POST /api/raspored/grupe` | `{"naziv":"API grupa","terminId":6}` |
| `POST /api/zadaci` | `{"naziv":"API zadatak","grupaId":6,"repoPutanja":"https://github.com/test/api-zadatak"}` |

Ovi zahtevi vracaju `201 Created` i ID novog zapisa. Sacekati odgovor svakog zahteva pre slanja povezanog zahteva. Grupa mora imati termin, a termin predmet, rok i ucionicu. Zadatak pripada grupi. Ucionica ne moze biti zauzeta u izabranom vremenu.

Postojece podatke moguce je procitati preko:

- `GET /api/predmet/all` ili `GET /api/predmet/{id}`
- `GET /api/raspored/rokovi`
- `GET /api/raspored/ucionice`
- `GET /api/raspored/termini?predmetId=6&rokId=6`
- `GET /api/raspored/grupe?terminId=6`

## Prijava studenta, preuzimanje, predaja i ocenjivanje rada

Prijaviti novog studenta pozivom `POST /api/auth/login`:

```json
{"email":"ailic10026rn@raf.rs","lozinka":"lozinka123"}
```

Koristiti **studentski token**:

1. `POST /api/prijave/grupa/6` — bez tela, kreira PrijavljenStudent i vraca ID.
2. `GET /api/zadaci/grupa/6` — vraca zadatak studentove grupe.
3. `POST /api/rad/6/preuzmi` — `6` je ID zadatka; kreira Rad sa statusom `PREUZET`, studentom i vremenom preuzimanja; vraca ID rada.
4. `POST /api/rad/6/predaj` — `6` je ID rada; telo:

```json
{"repoPutanjaResenja":"https://github.com/test/api-resenje"}
```

Predaja postavlja putanju resenja, vreme predaje i status `PREDAT`.

Zatim koristiti **token profesora koji predaje predmet**:

`POST /api/rad/6/oceni` — ID rada; telo:

```json
{"poeni":85}
```

Ocena popunjava poene i postavlja status `OCENJEN`. Ovim postupkom se popunjavaju sva polja rada kroz odgovarajuce poslovne operacije.

Status u Java modelu, odgovoru i parametrima repozitorijuma ima tip `StatusRada`, sa vrednostima `PREUZET`, `PREDAT` i `OCENJEN`. API i dalje vraca te nazive kao JSON tekst, a baza zadrzava postojecu `VARCHAR` kolonu. Status se postavlja kroz navedene poslovne operacije, bez slobodnog unosa statusa u zahtevima.

Za proveru rezultata dostupni su `GET /api/prijave/moje`, `GET /api/rad/moj` za studenta i `GET /api/prijave/grupa/6`, `GET /api/rad/predmet/6/predati` za profesora tog predmeta. Lista `predati` vraca radove u statusu `PREDAT`; ocenjeni rad se vidi u odgovoru ocenjivanja i studentskoj listi.

## HTTP odgovori

- `201` — kreiran novi zapis; login, citanje, predaja i ocenjivanje koriste `200`.
- `400` — nedostaju obavezna polja ili podaci nisu ispravni.
- `401` — nema ispravnog tokena ili login podaci nisu tacni.
- `403` — uloga ili veza sa predmetom/grupom ne daje trazeno pravo.
- `404` — ne postoji trazeni profesor, predmet ili drugi povezani zapis. Neke postojece provere vlasnistva odbijaju nedostupne ID-eve sa `403`.
- `409` — duplikat ili konflikt sa postojecim podacima/stanjima.

Ponovljeno slanje primera za iste jedinstvene podatke moze dati `409`; za novu proveru koristiti nove vrednosti ili prethodno vracene ID-eve.

## Izuzeci i telo odgovora o gresci

Servisi koriste poslovne izuzetke iz paketa `com.inzinjeri.rs_projekat.exceptions`, po obrascu profesorovog projekta. HTTP status odredjuje `GlobalExceptionHandler`:

| Izuzetak | HTTP status |
|---|---|
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |

Handler obradjuje i `DataIntegrityViolationException` kao 409, bez otkrivanja SQL-a i internih detalja baze. Originalni uzrok ostaje sacuvan u izuzecima koji nastanu pri upisu naloga i dodeli predmeta.

Poslovne i standardne Spring MVC greske vracaju `Content-Type: application/problem+json`, sa poljima `type`, `title`, `status`, `detail` i `instance`. Za neuspesnu `@Valid` validaciju dodaje se `errors`, sa greskom po polju. Na primer, kreiranje predmeta bez naziva i jezika vraca 400:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Neispravni podaci u zahtevu",
  "instance": "/api/predmet/add",
  "errors": {
    "naziv": "must not be blank",
    "jezik": "must not be blank"
  }
}
```

Tekst pojedinacne validacione poruke zavisi od jezika validacije. JWT filter i Spring Security zadrzavaju svoje odgovore za zahteve koji se odbiju pre izvrsavanja kontrolera.
