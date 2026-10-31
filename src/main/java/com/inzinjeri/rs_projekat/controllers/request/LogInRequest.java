package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogInRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String lozinka;
}
