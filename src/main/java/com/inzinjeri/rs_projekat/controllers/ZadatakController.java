package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.inzinjeri.rs_projekat.services.ZadatakService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import com.inzinjeri.rs_projekat.services.GitHubService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/zadaci")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class ZadatakController {

    private final ZadatakService zadatakService;

    @PostMapping(value = "/objavi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #grupaId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ZadatakResponse objavi(@RequestParam String naziv, @RequestParam Long grupaId,
                                  @RequestPart("files") List<MultipartFile> files,
                                  @RequestParam("paths") List<String> paths, Authentication auth) {
        return EntityMappers.toResponse(zadatakService.objavi(naziv, grupaId, files, paths, auth));
    }

    @GetMapping("/{id}/stablo")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #id) or @pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public List<GitHubService.TreeEntry> stablo(@PathVariable Long id, Authentication auth) {
        return zadatakService.stablo(id, auth);
    }

    @GetMapping("/{id}/fajl")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #id) or @pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public GitHubService.FileContent fajl(@PathVariable Long id, @RequestParam String putanja, Authentication auth) {
        return zadatakService.fajl(id, putanja, auth);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #id) or @pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("zadatak-" + id + ".zip").build().toString())
                .body(zadatakService.arhiva(id, auth));
    }

    @GetMapping("/{id}/download-fajl")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #id) or @pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public ResponseEntity<byte[]> downloadFajl(@PathVariable Long id, @RequestParam String putanja, Authentication auth) {
        GitHubService.FileContent file = zadatakService.fajl(id, putanja, auth);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.path().substring(file.path().lastIndexOf('/') + 1), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public ZadatakResponse izmeni(@PathVariable Long id, @Valid @RequestBody ZadatakRequest request,
                                  Authentication auth) {
        return EntityMappers.toResponse(zadatakService.izmeni(id, request, auth));
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ZadatakResponse izmeniAlias(@PathVariable Long id, @Valid @RequestBody ZadatakRequest request,
                                      Authentication auth) {
        return EntityMappers.toResponse(zadatakService.izmeni(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        zadatakService.obrisi(id, auth);
    }

    @GetMapping("/grupa/{grupaId}")
    @PreAuthorize("@pristupService.studentPripadaGrupi(authentication, #grupaId) or "
            + "@pristupService.profesorPredajePredmetGrupe(authentication, #grupaId)")
    public ZadatakResponse zaGrupu(@PathVariable Long grupaId, Authentication auth) {
        return zadatakService.zaGrupu(grupaId, auth);
    }

    @PostMapping
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #request.grupaId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody ZadatakRequest request, Authentication auth) {
        return zadatakService.dodaj(request, auth);
    }
}
