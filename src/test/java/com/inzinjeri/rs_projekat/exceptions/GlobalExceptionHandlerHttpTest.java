package com.inzinjeri.rs_projekat.exceptions;

import com.inzinjeri.rs_projekat.controllers.PredmetController;
import com.inzinjeri.rs_projekat.services.PredmetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerHttpTest {
    private final PredmetService predmeti = mock(PredmetService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PredmetController(predmeti))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @ParameterizedTest
    @MethodSource("poslovneGreske")
    void poslovniIzuzetakVracaOdgovarajuciStatusIPoruku(RuntimeException exception, int status) throws Exception {
        when(predmeti.findById(1L)).thenThrow(exception);

        mvc.perform(get("/api/predmet/1"))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value("Poruka iz servisa"))
                .andExpect(jsonPath("$.instance").value("/api/predmet/1"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist());
    }

    @Test
    void greskaBazeNeIzlazeSqlNiPodatke() throws Exception {
        when(predmeti.findById(1L)).thenThrow(new DataIntegrityViolationException("SQL sa tajnim podacima"));

        mvc.perform(get("/api/predmet/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Narusen integritet podataka (duplikat ili postojeca veza)"))
                .andExpect(jsonPath("$.cause").doesNotExist());
    }

    @Test
    void nepodrzanMetodZadrzavaAllowZaglavlje() throws Exception {
        mvc.perform(put("/api/predmet/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void pogresanTipIdaVracaProblemDetail() throws Exception {
        mvc.perform(get("/api/predmet/tekst"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(predmeti);
    }

    static Stream<Arguments> poslovneGreske() {
        return Stream.of(
                Arguments.of(new BadRequestException("Poruka iz servisa"), 400),
                Arguments.of(new UnauthorizedException("Poruka iz servisa"), 401),
                Arguments.of(new ForbiddenException("Poruka iz servisa"), 403),
                Arguments.of(new ResourceNotFoundException("Poruka iz servisa"), 404),
                Arguments.of(new ConflictException("Poruka iz servisa",
                        new DataIntegrityViolationException("Tajni SQL detalji")), 409));
    }
}
