package com.inzinjeri.rs_projekat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class RsProjekatApplication {

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Europe/Belgrade"));
	}

	public static void main(String[] args) {
		SpringApplication.run(RsProjekatApplication.class, args);
	}

}
