package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.LogInRequest;
import com.inzinjeri.rs_projekat.controllers.response.LoginResponse;
import com.inzinjeri.rs_projekat.services.AuthService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/auth")
public class AuthController {

    final AuthService authService;

    @PostMapping(path = "/login")
    public LoginResponse login(@RequestBody @Valid LogInRequest request){
        return EntityMappers.toLoginResponse(authService.login(request.getEmail(), request.getLozinka()));
    }
}
