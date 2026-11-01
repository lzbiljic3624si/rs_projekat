package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.LogInRequest;
import com.inzinjeri.rs_projekat.controllers.request.PromenaLozinkeRequest;
import com.inzinjeri.rs_projekat.controllers.response.LoginResponse;
import com.inzinjeri.rs_projekat.services.AuthService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/auth")
public class AuthController {

    final AuthService authService;

    @PutMapping("/lozinka")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promeniLozinku(@RequestBody @Valid PromenaLozinkeRequest request, Authentication auth) {
        authService.promeniLozinku(request, auth);
    }

    @PostMapping(path = "/login")
    public LoginResponse login(@RequestBody @Valid LogInRequest request){
        return EntityMappers.toLoginResponse(authService.login(request.getEmail(), request.getLozinka()));
    }
}
