package com.heng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap entry point for running the framework project itself.
 * <p>Applications consuming the library keep their own Spring Boot entry point and obtain
 * framework components through {@link com.heng.aditus.config.AditusConfiguration}.
 */
@SpringBootApplication
public class  AditusCavumApplication {

    /**
     * Starts this project's Spring application context.
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {

        SpringApplication.run(AditusCavumApplication.class, args);

    }

}
