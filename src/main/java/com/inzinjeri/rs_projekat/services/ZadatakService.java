package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.Rok;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import com.inzinjeri.rs_projekat.utils.GitHubFiles;
import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ZadatakService {

    private final ZadatakRepository zadatakRepository;
    private final GrupaRepository grupaRepository;
    private final StudentRepository studentRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final RadRepository radRepository;
    private final Clock clock;
    private final GitHubService github;
    private final RokRepository rokRepository;

    @Transactional
    public Zadatak izmeni(Long id, ZadatakRequest request, Authentication auth) {
        Zadatak zadatak = zaProfesora(id, auth);
        Grupa grupa = zadatak.getGrupa();
        if (!grupa.getId().equals(request.getGrupaId())) {
            throw new ConflictException("Zadatak ne moze da promeni grupu");
        }
        if (radRepository.existsByZadatak_Id(id)) {
            throw new ConflictException("Zadatak je vec preuzet i ne moze da se menja");
        }
        if (zadatak.getGithubCommit() != null && !request.getRepoPutanja().equals(zadatak.getRepoPutanja())) {
            throw new ConflictException("Objavljeni GitHub zadatak menja se slanjem fajlova");
        }
        if (!request.getRepoPutanja().equals(zadatak.getRepoPutanja())
                && zadatakRepository.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ConflictException("Putanja zadatka vec postoji");
        }
        zadatak.setNaziv(request.getNaziv());
        zadatak.setRepoPutanja(request.getRepoPutanja());
        return zadatakRepository.save(zadatak);
    }

    @Transactional
    public void obrisi(Long id, Authentication auth) {
        Zadatak zadatak = zaProfesora(id, auth);
        if (radRepository.existsByZadatak_Id(id)) {
            throw new ConflictException("Zadatak ima radove");
        }
        zadatakRepository.delete(zadatak);
        zadatakRepository.flush();
    }

    private Zadatak zaProfesora(Long id, Authentication auth) {
        Zadatak zadatak = zadatakRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));
        Grupa grupa = zadatak.getGrupa();
        if (grupa == null) {
            throw new ConflictException("Zadatak nema grupu");
        }
        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }
        return zadatak;
    }

    @Transactional
    public ZadatakResponse zaGrupu(Long grupaId, Authentication auth) {
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));

        Long predmetId = predmetId(grupa);

        boolean studentGrupe = studentRepository.pronadjiPoEmailu(auth.getName())
                .map(student -> prijavljenStudentRepository
                        .existsByStudent_IdAndGrupa_Id(student.getId(), grupaId))
                .orElse(false);

        boolean profesorPredmeta = profesorPredajePredmet(auth, predmetId);
        if (!studentGrupe && !profesorPredmeta) {
            throw new ForbiddenException("Nemaš pristup zadatku ove grupe");
        }

        if (!profesorPredmeta) {
            VremeTermina.proveriPreuzimanje(grupa.getTermin(), LocalDateTime.now(clock));
        }

        Zadatak zadatak = zadatakRepository.pronadjiPoGrupi(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa nema zadatak"));

        return EntityMappers.toResponse(zadatak);
    }

    public Long dodaj(ZadatakRequest request, Authentication auth) {
        Grupa grupa = grupaRepository.findById(request.getGrupaId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));

        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }

        if (zadatakRepository.existsByGrupa_Id(grupa.getId())) {
            throw new ConflictException("Grupa već ima zadatak");
        }

        if (zadatakRepository.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ConflictException("Putanja zadatka već postoji");
        }

        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv(request.getNaziv());
        zadatak.setGrupa(grupa);
        zadatak.setRepoPutanja(request.getRepoPutanja());

        return zadatakRepository.save(zadatak).getId();
    }

    @Transactional
    public Zadatak objavi(String naziv, Long grupaId, List<MultipartFile> files, List<String> paths,
                         Authentication auth) {
        Grupa grupa = grupaRepository.findByIdZaIzmenu(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }
        if (naziv == null || naziv.isBlank() || naziv.length() > 255) {
            throw new BadRequestException("Naziv zadatka mora imati od 1 do 255 znakova");
        }
        var upload = GitHubFiles.upload(files, paths);
        var termin = grupa.getTermin();
        String repoUrl = rokRepository.repoZaGrupuZaIzmenu(grupaId)
                .orElseThrow(() -> new ConflictException("Prvo povezi privatni GitHub repozitorijum sa rokom"));
        String repo = GitHubFiles.repository(repoUrl);
        Zadatak zadatak = zadatakRepository.pronadjiPoGrupi(grupaId).orElse(null);
        if (zadatak != null) {
            zadatak = zadatakRepository.findByIdZaIzmenu(zadatak.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));
            if (radRepository.findFirstByZadatak_Id(zadatak.getId()).isPresent()) {
                throw new ConflictException("Zadatak je vec preuzet i ne moze da se menja");
            }
        } else {
            zadatak = new Zadatak();
            zadatak.setGrupa(grupa);
        }

        String folder = zadatak.getGithubFolder();

        if(folder == null) {
            // Dobijamo folder roka na osnovu njegovog naziva i godine.
            String folderRoka = folderRoka(termin.getRok());

            // Formiramo nazive predmeta i grupe.
            String predmet = GitHubFiles.segment(termin.getPredmet().getNaziv(), "predmet-" +predmetId(grupa));

            String imeGrupe = GitHubFiles.segment(grupa.getNaziv(), "grupa-" + grupaId);

            // Osnovna putanja do pocetnih fajlova zadatka.
            folder = folderRoka + "/" + predmet + "/" + imeGrupe + "/zadatak";

            // Proveravamo da li folder vec postoji u drugom zadatku.
            int suffix = 1;

            while(!zadatakRepository.pronadjiFolderZaIzmenu(repo, folder, zadatak.getId() == null ? -1L : zadatak.getId()).isEmpty()){
                String grupaSaSufiksom = imeGrupe + "-" + grupaId + (suffix == 1 ? "" : "-" + suffix);

                folder = folderRoka + "/" + predmet + "/" + grupaSaSufiksom + "/zadatak";

                suffix++;
            }
        }
        if (zadatak.getGithubRepo() != null && !repo.equals(zadatak.getGithubRepo())) {
            throw new ConflictException("Repozitorijum objavljenog zadatka se ne menja");
        }
        String commit = github.publish(repo, folder, upload, zadatak.getGithubCommit());
        zadatak.setNaziv(naziv);
        zadatak.setGithubRepo(repo);
        zadatak.setGithubFolder(folder);
        zadatak.setGithubCommit(commit);
        zadatak.setRepoPutanja(GitHubFiles.folderUrl(repo, commit, folder));
        return zadatakRepository.save(zadatak);
    }

    @Transactional(readOnly = true)
    public List<GitHubService.TreeEntry> stablo(Long id, Authentication auth) {
        Zadatak zadatak = zaCitanjeFajlova(id, auth);
        return github.tree(zadatak.getGithubRepo(), zadatak.getGithubCommit(), zadatak.getGithubFolder());
    }

    @Transactional(readOnly = true)
    public GitHubService.FileContent fajl(Long id, String path, Authentication auth) {
        Zadatak zadatak = zaCitanjeFajlova(id, auth);
        return github.read(zadatak.getGithubRepo(), zadatak.getGithubCommit(), zadatak.getGithubFolder(), GitHubFiles.path(path));
    }

    @Transactional(readOnly = true)
    public byte[] arhiva(Long id, Authentication auth) {
        Zadatak zadatak = zaCitanjeFajlova(id, auth);
        return github.archive(zadatak.getGithubRepo(), zadatak.getGithubCommit(), zadatak.getGithubFolder());
    }

    private Zadatak zaCitanjeFajlova(Long id, Authentication auth) {
        Zadatak zadatak = zadatakRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));
        Grupa grupa = zadatak.getGrupa();
        Long predmet = predmetId(grupa);
        if (!profesorPredajePredmet(auth, predmet)) {
            if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))) {
                throw new ForbiddenException("Nemas pristup zadatku");
            }
            var student = studentRepository.pronadjiPoEmailu(auth.getName())
                    .orElseThrow(() -> new ForbiddenException("Nemas pristup zadatku"));
            if (!prijavljenStudentRepository.existsByStudent_IdAndGrupa_Id(student.getId(), grupa.getId())) {
                throw new ForbiddenException("Grupa zadatka nije studentova");
            }
            Rad rad = radRepository.findByStudent_IdAndZadatak_Id(student.getId(), id)
                    .orElseThrow(() -> new ConflictException("Prvo zapocni rad na zadatku"));
            if (rad.getStatus() != StatusRada.PREUZET) {
                throw new ConflictException("Preuzimanje je dostupno samo tokom rada");
            }
            VremeTermina.proveriPredaju(grupa.getTermin(), LocalDateTime.now(clock));
            if (rad.getIzvorniCommit() == null || !rad.getIzvorniCommit().equals(zadatak.getGithubCommit())) {
                throw new ConflictException("Rad nema potvrdjen izvorni commit");
            }
        }
        if (zadatak.getGithubRepo() == null || zadatak.getGithubFolder() == null || zadatak.getGithubCommit() == null) {
            throw new ConflictException("Zadatak nema objavljene GitHub fajlove; profesor prvo treba da ih objavi");
        }
        return zadatak;
    }

    private boolean profesorPredajePredmet(
            Authentication auth, Long predmetId) {

        if (auth.getAuthorities().stream()
                .noneMatch(uloga ->
                        "ROLE_PROFESOR".equals(uloga.getAuthority()))) {
            return false;
        }

        return predajePredmetRepository
                .getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza ->
                        veza.getPredmet() != null
                                && predmetId.equals(
                                veza.getPredmet().getId()));
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa == null || grupa.getTermin() == null
                || grupa.getTermin().getPredmet() == null) {
            throw new ConflictException("Grupa nema predmet");
        }

        return grupa.getTermin().getPredmet().getId();
    }

    private String folderRoka(Rok rok){
        if(rok == null
                || rok.getNaziv() == null
                || rok.getNaziv().isBlank()
                || rok.getSkolskaGodina() == null){
            throw new ConflictException(
                    "Roku nedostaju naziv ili skolska godina");
        }

        String naziv = rok.getNaziv().trim().toLowerCase(Locale.ROOT);

        int mesec;

        if (naziv.startsWith("januar")) {
            mesec = 1;
        } else if (naziv.startsWith("februar")) {
            mesec = 2;
        } else if (naziv.startsWith("mart")) {
            mesec = 3;
        } else if (naziv.startsWith("april")) {
            mesec = 4;
        } else if (naziv.startsWith("maj")) {
            mesec = 5;
        } else if (naziv.startsWith("jun")) {
            mesec = 6;
        } else if (naziv.startsWith("jul")) {
            mesec = 7;
        } else if (naziv.startsWith("avgust")) {
            mesec = 8;
        } else if (naziv.startsWith("septembar")) {
            mesec = 9;
        } else if (naziv.startsWith("oktobar")) {
            mesec = 10;
        } else if (naziv.startsWith("novembar")) {
            mesec = 11;
        } else if (naziv.startsWith("decembar")) {
            mesec = 12;
        } else {
            throw new ConflictException(
                    "Naziv roka mora sadrzati naziv meseca"
            );
        }

        int godina = rok.getSkolskaGodina();

        // Januarski do oktobarski rokovi pripadaju narednoj kalendarskoj godini.
        if (mesec <= 10) {
            godina++;
        }

        // Zadrzavamo originalni naziv roka.
        return GitHubFiles.segment(
                rok.getNaziv().trim() + "-" + godina,
                "rok-" + rok.getId()
        );
    }
}
