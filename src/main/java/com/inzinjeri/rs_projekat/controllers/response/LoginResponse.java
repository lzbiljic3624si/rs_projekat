package com.inzinjeri.rs_projekat.controllers.response;

import com.inzinjeri.rs_projekat.model.Uloga;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Instant istice;
    private Uloga uloga;
}
