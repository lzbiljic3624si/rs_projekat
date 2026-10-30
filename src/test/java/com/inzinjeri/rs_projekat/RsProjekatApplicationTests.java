package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@MockitoBean(types = {
	GrupaRepository.class, PredajePredmetRepository.class, PredmetRepository.class,
	PrijavljenStudentRepository.class, ProfesorRepository.class, RadRepository.class,
	RokRepository.class, StudentRepository.class, TerminRepository.class,
	UcionicaRepository.class, ZadatakRepository.class
})
class RsProjekatApplicationTests {
	@MockitoBean
	private Seeder seeder;

	@Test
	void contextLoads() {
	}

}
