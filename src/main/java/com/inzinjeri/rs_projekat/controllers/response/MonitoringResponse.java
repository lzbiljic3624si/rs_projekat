package com.inzinjeri.rs_projekat.controllers.response;

import com.inzinjeri.rs_projekat.model.StatusRada;
import java.util.List;

public record MonitoringResponse(Long terminId, int ukupnoPrijavljenih, int preuzeli,
                                 int predali, int josRade, int pregledani,
                                 List<StudentNaTerminu> studenti) {
    public record StudentNaTerminu(StudentResponse student, StatusRada status) {}
}
