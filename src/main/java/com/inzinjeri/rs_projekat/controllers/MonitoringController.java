package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.MonitoringResponse;
import com.inzinjeri.rs_projekat.services.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
    private final MonitoringService monitoringService;

    @GetMapping("/termin/{terminId}")
    @PreAuthorize("@pristupService.profesorPredajePredmetTermina(authentication, #terminId)")
    public MonitoringResponse zaTermin(@PathVariable Long terminId) {
        return monitoringService.zaTermin(terminId);
    }
}
