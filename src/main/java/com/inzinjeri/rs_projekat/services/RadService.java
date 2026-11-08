package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.request.IzmenaFajlaRequest;
import com.inzinjeri.rs_projekat.controllers.response.RadResponse;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import com.inzinjeri.rs_projekat.utils.GitHubFiles;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RadService {

    private final RadRepository radRepository;
    private final StudentRepository studentRepository;
    private final ZadatakRepository zadatakRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final Clock clock;
    private final GitHubService github;

    public record PocetakRada(RadResponse rad, String izvorniCommit, String nazivFoldera) {}

    @Transactional(readOnly = true)
    public List<Rad> zaPredmet(Long predmetId, StatusRada status, Long grupaId, Long terminId,
                              Long zadatakId, Authentication auth) {
        proveriDaPredaje(auth, predmetId);
        return radRepository.findByPredmetIdAndFilters(predmetId, status, grupaId, terminId, zadatakId);
    }

    @Transactional(readOnly = true)
    public Rad byId(Long radId, Authentication auth) {
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
        proveriPristupRadu(rad, auth);
        return rad;
    }

    private void proveriPristupRadu(Rad rad, Authentication auth) {
        boolean vlasnik = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))
                && rad.getStudent() != null && auth.getName().equals(rad.getStudent().getEmail());
        if (!vlasnik) {
            if (rad.getZadatak() == null || rad.getZadatak().getGrupa() == null
                    || rad.getZadatak().getGrupa().getTermin() == null
                    || rad.getZadatak().getGrupa().getTermin().getPredmet() == null) {
                throw new ConflictException("Rad nema predmet");
            }
            proveriDaPredaje(auth, rad.getZadatak().getGrupa().getTermin().getPredmet().getId());
        }
    }

    @Transactional
    public Rad izmeni(Long id, OcenaRadaRequest request, Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_PROFESOR".equals(a.getAuthority()))) {
            throw new ForbiddenException("Samo profesor moze da ispravlja poene rada");
        }
        Rad rad = radZaIzmenu(id);
        proveriPristupRadu(rad, auth);
        if (request.getPoeni() == null || !Double.isFinite(request.getPoeni()) || request.getPoeni() < 0) {
            throw new BadRequestException("Poeni moraju biti nenegativan broj");
        }
        if (rad.getStatus() != StatusRada.OCENJEN) {
            throw new ConflictException("Ispravka poena je moguca samo za ocenjen rad");
        }
        rad.setPoeni(request.getPoeni());
        return radRepository.save(rad);
    }

    @Transactional
    public void obrisi(Long id, Authentication auth) {
        Rad rad = radZaIzmenu(id);
        proveriPristupRadu(rad, auth);
        radRepository.delete(rad);
        radRepository.flush();
    }

    private Rad radZaIzmenu(Long id) {
        return radRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
    }

    @Transactional(readOnly = true)
    public List<Rad> moj(Authentication auth) {
        return radRepository.getRadByStudentEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public List<Rad> predati(Long predmetId, Authentication auth) {
        proveriDaPredaje(auth, predmetId);

        return radRepository.findByPredmetIdAndStatus(predmetId, StatusRada.PREDAT);
    }

    @Transactional
    public Rad oceni (Long radId, OcenaRadaRequest request, Authentication auth){
        Rad rad = radZaIzmenu(radId);

        if(rad.getZadatak() == null
                || rad.getZadatak().getGrupa() == null
                || rad.getZadatak().getGrupa().getTermin() == null
                || rad.getZadatak().getGrupa().getTermin().getPredmet() == null){
            throw new ConflictException("Rad nema predmet");
        }

        Long predmetId = rad.getZadatak()
                .getGrupa()
                .getTermin()
                .getPredmet()
                .getId();

        proveriDaPredaje(auth, predmetId);

        if(rad.getStatus() != StatusRada.PREDAT){
            throw  new ConflictException("Rad nije spreman za ocenjivanje");
        }

        if(request.getPoeni() == null
                || !Double.isFinite(request.getPoeni())
                || request.getPoeni() < 0){
            throw new BadRequestException("Poeni moraju biti nenegativan broj");
        }

        rad.setPoeni(request.getPoeni());
        rad.setStatus(StatusRada.OCENJEN);

        return radRepository.save(rad);
    }

    @Transactional
    public Rad predaj(Long radId, Authentication auth){
        return predaj(radId, List.of(), List.of(), auth);
    }

    @Transactional
    public Rad predaj(Long radId, List<MultipartFile> files, List<String> paths, Authentication auth){
        Rad rad = radZaIzmenu(radId);
        if(rad.getStudent() == null
                || !auth.getName().equals(rad.getStudent().getEmail())){
            throw new ForbiddenException("Ovaj rad nije tvoj");
        }
        if(rad.getStatus() != StatusRada.PREUZET){
            throw new ConflictException("Rad ne moze ponovo da se preda");
        }

        LocalDateTime sada = LocalDateTime.now(clock);
        VremeTermina.proveriPredaju(rad.getZadatak() == null || rad.getZadatak().getGrupa() == null
                ? null : rad.getZadatak().getGrupa().getTermin(), sada);

        var upload = GitHubFiles.upload(files, paths);
        if (rad.getGithubRepo() == null || rad.getGithubFolder() == null || rad.getIzvorniCommit() == null) {
            throw new ConflictException("Rad nema GitHub izvor; profesor prvo treba da objavi fajlove zadatka");
        }
        String commit = github.publish(rad.getGithubRepo(), rad.getGithubFolder(), upload, null);
        rad.setGithubCommit(commit);
        rad.setRepoPutanjaResenja(GitHubFiles.folderUrl(rad.getGithubRepo(), commit, rad.getGithubFolder()));
        rad.setVremePredaje(sada);
        rad.setStatus(StatusRada.PREDAT);

        return radRepository.save(rad);
    }

    @Transactional
    public Long preuzmi(Long zadatakId, Authentication auth){
        return preuzmiInterno(zadatakId, auth, false).getId();
    }

    @Transactional
    public PocetakRada zapocni(Long zadatakId, Authentication auth) {
        Rad rad = preuzmiInterno(zadatakId, auth, true);
        String folder = rad.getGithubFolder();
        return new PocetakRada(EntityMappers.toResponse(rad), rad.getIzvorniCommit(),
                folder.substring(folder.lastIndexOf('/') + 1));
    }

    private Rad preuzmiInterno(Long zadatakId, Authentication auth, boolean oporavak){
        Student student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));

        Zadatak zadatak = zadatakRepository.findByIdZaIzmenu(zadatakId)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));

        if (zadatak.getGrupa() == null) {
            throw new ConflictException("Zadatku nije dodeljena grupa");
        }

        boolean smeDaPreuzme = prijavljenStudentRepository
                .existsByStudent_IdAndGrupa_Id(
                        student.getId(),
                        zadatak.getGrupa().getId()
                );

        if (!smeDaPreuzme) {
            throw new ForbiddenException("Grupa zadatka nije studentova");
        }

        Rad postojeci = radRepository.findByStudentIZadatakZaIzmenu(student.getId(), zadatakId).orElse(null);
        if (oporavak && postojeci != null) {
            if (postojeci.getStatus() != StatusRada.PREUZET) {
                throw new ConflictException("Rad je vec predat i ne moze ponovo da se zapocne");
            }
            VremeTermina.proveriPredaju(zadatak.getGrupa().getTermin(), LocalDateTime.now(clock));
            if (postojeci.getIzvorniCommit() == null || postojeci.getGithubFolder() == null) {
                throw new ConflictException("Postojeci rad nema potvrdjen GitHub izvor");
            }
            return postojeci;
        }
        if(postojeci != null || radRepository.existsByStudent_IdAndZadatak_Id(student.getId(),zadatakId)){
            throw new ConflictException("Zadatak je vec preuzet");
        }

        if (oporavak && (zadatak.getGithubRepo() == null || zadatak.getGithubFolder() == null || zadatak.getGithubCommit() == null)) {
            throw new ConflictException("Profesor prvo treba da objavi GitHub fajlove zadatka");
        }

        LocalDateTime sada = LocalDateTime.now(clock);
        VremeTermina.proveriPreuzimanje(zadatak.getGrupa().getTermin(), sada);

        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        if (zadatak.getGithubCommit() != null) {
            pripremiGitHubRad(rad, student, zadatak);
        } else {
            rad.setRepoPutanjaResenja(automatskaPutanjaResenja(student, zadatak));
        }
        rad.setVremePreuzimanja(sada);
        rad.setStatus(StatusRada.PREUZET);

        return radRepository.save(rad);
    }

    private void pripremiGitHubRad(Rad rad, Student student, Zadatak zadatak) {
        if (zadatak.getGithubRepo() == null || zadatak.getGithubFolder() == null
                || !zadatak.getGithubFolder().endsWith("/zadatak")) {
            throw new ConflictException("Zadatak nema ispravnu GitHub putanju");
        }
        String prefix = zadatak.getGithubFolder().substring(0, zadatak.getGithubFolder().lastIndexOf('/') + 1);
        String email = student.getEmail();
        String ime = GitHubFiles.segment(email.substring(0, email.indexOf('@')), "student-" + student.getId());
        String base = prefix + ime;
        String folder = base;
        int suffix = 1;
        while (folder.equalsIgnoreCase(prefix + "zadatak")
                || !radRepository.pronadjiFolderZaIzmenu(zadatak.getGithubRepo(), folder).isEmpty()) {
            folder = base + "-" + student.getId() + (suffix == 1 ? "" : "-" + suffix);
            suffix++;
        }
        rad.setGithubRepo(zadatak.getGithubRepo());
        rad.setGithubFolder(folder);
        rad.setIzvorniCommit(zadatak.getGithubCommit());
    }

    @Transactional(readOnly = true)
    public List<GitHubService.TreeEntry> stablo(Long id, boolean radna, Authentication auth) {
        Rad rad = byId(id, auth);
        String commit = commitZaCitanje(rad, radna);
        return github.tree(rad.getGithubRepo(), commit, rad.getGithubFolder());
    }

    @Transactional(readOnly = true)
    public GitHubService.FileContent fajl(Long id, String path, boolean radna, Authentication auth) {
        Rad rad = byId(id, auth);
        String commit = commitZaCitanje(rad, radna);
        return github.read(rad.getGithubRepo(), commit, rad.getGithubFolder(), GitHubFiles.path(path));
    }

    @Transactional(readOnly = true)
    public byte[] arhiva(Long id, boolean radna, Authentication auth) {
        Rad rad = byId(id, auth);
        String commit = commitZaCitanje(rad, radna);
        return github.archive(rad.getGithubRepo(), commit, rad.getGithubFolder());
    }

    @Transactional
    public String izmeniFajl(Long id, IzmenaFajlaRequest request, Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_PROFESOR".equals(a.getAuthority()))) {
            throw new ForbiddenException("Samo profesor moze da menja fajlove rada");
        }
        Rad rad = radZaIzmenu(id);
        proveriPristupRadu(rad, auth);
        proveriGitHubPredaju(rad);
        return github.edit(rad.getGithubRepo(), rad.getGithubFolder(), GitHubFiles.path(request.getPutanja()),
                request.getPrethodniSha(), request.getTekst());
    }

    private String commitZaCitanje(Rad rad, boolean radna) {
        proveriGitHubPredaju(rad);
        return radna ? github.head(rad.getGithubRepo()) : rad.getGithubCommit();
    }

    private void proveriGitHubPredaju(Rad rad) {
        if (rad.getStatus() == StatusRada.PREUZET || rad.getGithubRepo() == null
                || rad.getGithubFolder() == null || rad.getGithubCommit() == null) {
            throw new ConflictException("Rad nema potvrdjenu GitHub predaju");
        }
    }

    private String automatskaPutanjaResenja(Student student, Zadatak zadatak) {
        var termin = zadatak == null || zadatak.getGrupa() == null ? null : zadatak.getGrupa().getTermin();
        if (student == null || termin == null || termin.getPredmet() == null) {
            throw new ConflictException("Nedostaju podaci za putanju repozitorijuma resenja");
        }
        String putanja = ParseUtils.repoPutanjaResenja(student.getEmail(), termin.getPredmet().getNaziv(), termin.getDatum());
        if (putanja == null) {
            throw new ConflictException("Email, predmet i datum moraju omoguciti putanju resenja do 255 znakova");
        }
        return putanja;
    }

    private void proveriDaPredaje(Authentication auth, Long predmetId) {
        boolean predaje = predajePredmetRepository.getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza -> veza.getPredmet() != null
                        && predmetId.equals(veza.getPredmet().getId()));

        if (!predaje) {
            throw new ForbiddenException("Profesor ne predaje ovaj predmet");
        }
    }
}
