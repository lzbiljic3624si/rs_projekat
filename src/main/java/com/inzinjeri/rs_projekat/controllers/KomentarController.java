package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.KomentarRequest;
import com.inzinjeri.rs_projekat.controllers.response.KomentarResponse;
import com.inzinjeri.rs_projekat.model.Komentar;
import com.inzinjeri.rs_projekat.services.KomentarService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/komentar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class KomentarController {

    private final KomentarService komentarService;

    @PostMapping("/rad/{radId}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public KomentarResponse dodaj(@PathVariable Long radId, @Valid @RequestBody KomentarRequest request, Authentication auth) {

        Komentar komentar = komentarService.dodaj(radId, request.getKomentar(), request.getPutanjaFajla(), request.getBrojLinije(), auth);

        return EntityMappers.toResponse(komentar);
    }

    // Prikaz komentara za odredjeni rad
    @GetMapping("/rad/{radId}")
    public List<KomentarResponse> zaRad(@PathVariable Long radId, Authentication auth) {

        return komentarService.zaRad(radId, auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/{komentarId}")
    public KomentarResponse findById(@PathVariable Long komentarId, Authentication auth) {
        return EntityMappers.toResponse(komentarService.findById(komentarId, auth));
    }

    @PutMapping("/{komentarId}")
    @PreAuthorize("hasRole('PROFESOR')")
    public KomentarResponse izmeni(@PathVariable Long komentarId, @Valid @RequestBody KomentarRequest request,
                                  Authentication auth) {
        return EntityMappers.toResponse(komentarService.izmeni(komentarId, request.getKomentar(),
                request.getPutanjaFajla(), request.getBrojLinije(), auth));
    }

    // Brisanje komentara
    @DeleteMapping("/{komentarId}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long komentarId, Authentication auth) {

        komentarService.obrisi(komentarId, auth);
    }
}
