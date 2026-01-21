package com.example.zadanie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class ZadanieApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZadanieApplication.class, args);
	}

}
