package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        log.info("Starting Kopitiam Agent Application...");
        SpringApplication.run(Main.class, args);
        log.info("Kopitiam Agent Application started successfully");
    }
}
