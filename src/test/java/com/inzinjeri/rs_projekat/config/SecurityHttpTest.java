package com.inzinjeri.rs_projekat.config;

import com.inzinjeri.rs_projekat.controllers.PredmetController;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.services.PredmetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PredmetController.class)
@Import({SecurityConfig.class, SecurityHttpTest.LoginProbe.class})
class SecurityHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private PredmetService predmeti;

    @Test
    void bezTokenaVraca401BezPreusmeravanja() throws Exception {
        mvc.perform(get("/api/predmet/all")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        verifyNoInteractions(predmeti);
    }

    @Test
    void studentSaPotpisanimTokenomMozeDaCitaBezSesije() throws Exception {
        when(predmeti.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/predmet/all").header("Authorization", "Bearer " + token("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().json("[]"))
                .andExpect(result -> assertNull(result.getRequest().getSession(false)))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(predmeti).findAll();
    }

    @Test
    void profesorMozeDaDodaPredmetBezCsrfTokena() throws Exception {
        Predmet predmet = new Predmet(); predmet.setId(1L);
        when(predmeti.save(any())).thenReturn(predmet);
        mvc.perform(post("/api/predmet/add").header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json")
                        .content("{\"naziv\":\"RS\",\"jezik\":\"Java\",\"uslov\":\"OOP\"}"))
                .andExpect(status().isCreated()).andExpect(content().string("1"));
        verify(predmeti).save(any());
    }

    @Test
    void studentNeMozeDaDodaPredmet() throws Exception {
        mvc.perform(post("/api/predmet/add").header("Authorization", "Bearer " + token("STUDENT"))
                        .contentType("application/json")
                        .content("{\"naziv\":\"RS\",\"jezik\":\"Java\",\"uslov\":\"OOP\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(predmeti);
    }

    @Test
    void ostecenTokenSeOdbija() throws Exception {
        mvc.perform(get("/api/predmet/all").header("Authorization", "Bearer neispravan-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(predmeti);
    }

    @Test
    void istekaoTokenSeOdbija() throws Exception {
        String istekao = token(encoder, "STUDENT", Instant.now().minusSeconds(120));
        mvc.perform(get("/api/predmet/all").header("Authorization", "Bearer " + istekao))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(predmeti);
    }

    @Test
    void tokenPotpisanDrugimKljucemSeOdbija() throws Exception {
        var config = new SecurityConfig();
        var key = config.jwtKey(new SecurityProperties("drugi-test-only-secret-at-least-32-bytes",
                Duration.ofHours(1), List.of()));
        String drugi = token(config.jwtEncoder(key), "STUDENT", Instant.now().plusSeconds(3600));
        mvc.perform(get("/api/predmet/all").header("Authorization", "Bearer " + drugi))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(predmeti);
    }

    @Test
    void loginPutanjaNeZahtevaPrijavu() throws Exception {
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk())
                .andExpect(content().string("login-dozvoljen"));
    }

    @Test
    void dozvoljeniCorsPreflightNeZahtevaToken() throws Exception {
        mvc.perform(options("/api/predmet/all").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        verifyNoInteractions(predmeti);
    }

    @Test
    void nepoznatCorsOriginSeOdbija() throws Exception {
        mvc.perform(options("/api/predmet/all").header("Origin", "https://nepoznat.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(predmeti);
    }

    private String token(String uloga) {
        return token(encoder, uloga, Instant.now().plusSeconds(3600));
    }

    private String token(JwtEncoder jwtEncoder, String uloga, Instant istek) {
        var claims = JwtClaimsSet.builder().subject("korisnik@test.rs")
                .issuedAt(istek.minusSeconds(3600)).expiresAt(istek).claim("uloga", uloga).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // Proverava samo permitAll pravilo; stvarni login ostaje u koleginom AuthController.
    @RestController
    static class LoginProbe {
        @PostMapping("/api/auth/login")
        public String login() {
            return "login-dozvoljen";
        }
    }
}
