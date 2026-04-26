package com.ticketing.queue_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QueueServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueueServerApplication.class, args);
	}

}
