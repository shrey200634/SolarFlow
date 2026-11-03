package com.solarFlow.solarFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SolarFlowApplication {

	public static void main(String[] args) {
		// Must run before Spring opens any DB connection (JDBC sends the JVM timezone to Postgres)
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(SolarFlowApplication.class, args);
	}
}