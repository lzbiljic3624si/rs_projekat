package com.inzinjeri.rs_projekat.controllers.response;

import com.inzinjeri.rs_projekat.model.Profesor;
import lombok.Data;

@Data
public class PredajePredmetResponse {
    private Long id;
    private ProfesorResponse profesor;
    private PredmetResponse predmet;
}
