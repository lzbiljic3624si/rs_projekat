package com.inzinjeri.rs_projekat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class RsProjekatApplicationTests {
	@MockitoBean
	private Seeder seeder;

	@Test
	void contextLoads() {
	}

}
