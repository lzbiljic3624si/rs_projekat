package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.request.IzmenaFajlaRequest;
import com.inzinjeri.rs_projekat.services.GitHubService;
import com.inzinjeri.rs_projekat.controllers.response.RadResponse;
import com.inzinjeri.rs_projekat.controllers.response.StudentResponse;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.inzinjeri.rs_projekat.services.RadService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rad")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class RadController {

    private final RadService radService;

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public RadResponse izmeni(@PathVariable Long id, @Valid @RequestBody OcenaRadaRequest request, Authentication auth) {
        return EntityMappers.toResponse(radService.izmeni(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        radService.obrisi(id, auth);
    }

    @GetMapping("/predmet/{predmetId}")
    @PreAuthorize("@pristupService.profesorPredajePredmet(authentication, #predmetId)")
    public List<RadResponse> zaPredmet(@PathVariable Long predmetId,
                                      @RequestParam(required = false) StatusRada status,
                                      @RequestParam(required = false) Long grupaId,
                                      @RequestParam(required = false) Long terminId,
                                      @RequestParam(required = false) Long zadatakId, Authentication auth) {
        return radService.zaPredmet(predmetId, status, grupaId, terminId, zadatakId, auth)
                .stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping(value = "/predmet/{predmetId}/resenja.zip", produces = "application/zip")
    @PreAuthorize("hasRole('PROFESOR') and @pristupService.profesorPredajePredmet(authentication, #predmetId)")
    public ResponseEntity<byte[]> downloadResenjaPredmeta(
            @PathVariable Long predmetId,
            @RequestParam(required = false) StatusRada status,
            @RequestParam(required = false) Long grupaId,
            @RequestParam(required = false) Long terminId,
            @RequestParam(required = false) Long zadatakId,
            Authentication auth){

        byte[] arhiva = radService.arhivaPredmeta(predmetId, status, grupaId, terminId, zadatakId, auth);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("resenja-predmeta-" + predmetId + ".zip")
                                .build()
                                .toString()
                )
                .body(arhiva);
    }

    @GetMapping("/{radId}")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId) or "
            + "@pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public RadResponse byId(@PathVariable Long radId, Authentication auth) {
        return EntityMappers.toResponse(radService.byId(radId, auth));
    }

    @GetMapping("/moj")
    public List<RadResponse> moj(Authentication auth) {
        return radService.moj(auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/predmet/{predmetId}/predati")
    @PreAuthorize("@pristupService.profesorPredajePredmet(authentication, #predmetId)")
    public List<RadResponse> predati(@PathVariable Long predmetId, Authentication auth) {
        return radService.predati(predmetId, auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @PostMapping("/{radId}/oceni")
    @PreAuthorize("@pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public RadResponse oceni(@PathVariable Long radId,
                             @Valid @RequestBody OcenaRadaRequest request,
                             Authentication auth) {
        return EntityMappers.toResponse(radService.oceni(radId, request, auth));
    }

    @PostMapping("/{radId}/predaj")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId)")
    public RadResponse predaj(@PathVariable Long radId, Authentication auth) {
        return EntityMappers.toResponse(radService.predaj(radId, auth));
    }

    @PostMapping(value = "/{radId}/predaj", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId)")
    public RadResponse predajFajlove(@PathVariable Long radId, @RequestPart("files") List<MultipartFile> files,
                                     @RequestParam("paths") List<String> paths, Authentication auth) {
        return EntityMappers.toResponse(radService.predaj(radId, files, paths, auth));
    }

    @PostMapping("/{zadatakId}/zapocni")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #zadatakId)")
    public RadService.PocetakRada zapocni(@PathVariable Long zadatakId, Authentication auth) {
        return radService.zapocni(zadatakId, auth);
    }

    @GetMapping("/{radId}/stablo")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId) or @pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public List<GitHubService.TreeEntry> stablo(@PathVariable Long radId,
            @RequestParam(defaultValue = "false") boolean radna, Authentication auth) {
        return radService.stablo(radId, radna, auth);
    }

    @GetMapping("/{radId}/fajl")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId) or @pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public GitHubService.FileContent fajl(@PathVariable Long radId, @RequestParam String putanja,
            @RequestParam(defaultValue = "false") boolean radna, Authentication auth) {
        return radService.fajl(radId, putanja, radna, auth);
    }

    @PutMapping("/{radId}/fajl")
    @PreAuthorize("@pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public java.util.Map<String, String> izmeniFajl(@PathVariable Long radId,
            @Valid @RequestBody IzmenaFajlaRequest request, Authentication auth) {
        return java.util.Map.of("githubCommit", radService.izmeniFajl(radId, request, auth));
    }

    @GetMapping("/{radId}/download")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId) or @pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public ResponseEntity<byte[]> download(@PathVariable Long radId,
            @RequestParam(defaultValue = "false") boolean radna, Authentication auth) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("rad-" + radId + ".zip").build().toString())
                .body(radService.arhiva(radId, radna, auth));
    }

    @GetMapping("/{radId}/download-fajl")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId) or @pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public ResponseEntity<byte[]> downloadFajl(@PathVariable Long radId, @RequestParam String putanja,
            @RequestParam(defaultValue = "false") boolean radna, Authentication auth) {
        GitHubService.FileContent file = radService.fajl(radId, putanja, radna, auth);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.path().substring(file.path().lastIndexOf('/') + 1), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }

    @PostMapping("/{zadatakId}/preuzmi")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #zadatakId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long preuzmi(@PathVariable Long zadatakId, Authentication auth) {
        return radService.preuzmi(zadatakId, auth);
    }





}
