package com.skylife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point for the Skylife application.  This class uses the
 * {@link SpringBootApplication} annotation to enable component
 * scanning, auto‑configuration and property support.  When run, it
 * starts an embedded servlet container and exposes the REST API
 * defined in {@code com.skylife.controller.GcsController}.
 */
@SpringBootApplication
public class SkylifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkylifeApplication.class, args);
    }
}