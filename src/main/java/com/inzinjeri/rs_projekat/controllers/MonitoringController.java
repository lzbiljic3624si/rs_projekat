package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.MonitoringResponse;
import com.inzinjeri.rs_projekat.controllers.response.MonitoringResponse.StudentNaTerminu;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.TerminRepository;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
    private final TerminRepository termini;
    private final PrijavljenStudentRepository prijave;
    private final RadRepository radovi;

    @GetMapping("/termin/{terminId}")
    @PreAuthorize("@pristupService.profesorPredajePredmetTermina(authentication, #terminId)")
    @Transactional(readOnly = true)
    public MonitoringResponse zaTermin(@PathVariable Long terminId) {
        termini.findById(terminId)
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));

        Map<Long, StudentNaTerminu> studenti = new LinkedHashMap<>();
        prijave.findByGrupa_Termin_Id(terminId).forEach(prijava ->
                studenti.putIfAbsent(prijava.getStudent().getId(),
                        new StudentNaTerminu(EntityMappers.toResponse(prijava.getStudent()), null)));

        radovi.findByZadatak_Grupa_Termin_Id(terminId).forEach(rad -> {
            Long studentId = rad.getStudent().getId();
            StudentNaTerminu prethodni = studenti.get(studentId);
            if (prethodni != null && (prethodni.status() == null
                    || rad.getStatus().ordinal() > prethodni.status().ordinal())) {
                studenti.put(studentId, new StudentNaTerminu(prethodni.student(), rad.getStatus()));
            }
        });

        var lista = studenti.values().stream().toList();
        int preuzeli = (int) lista.stream().filter(s -> s.status() != null).count();
        int predali = (int) lista.stream().filter(s -> s.status() == StatusRada.PREDAT
                || s.status() == StatusRada.OCENJEN).count();
        int josRade = (int) lista.stream().filter(s -> s.status() == StatusRada.PREUZET).count();
        int pregledani = (int) lista.stream().filter(s -> s.status() == StatusRada.OCENJEN).count();
        return new MonitoringResponse(terminId, lista.size(), preuzeli, predali, josRade, pregledani, lista);
    }
}
