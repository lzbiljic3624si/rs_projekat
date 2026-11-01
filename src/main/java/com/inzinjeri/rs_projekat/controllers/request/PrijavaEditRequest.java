package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrijavaEditRequest {
    @NotNull
    private Long grupaId;
}
