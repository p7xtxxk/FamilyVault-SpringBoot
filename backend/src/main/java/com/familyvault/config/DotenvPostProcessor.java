package com.familyvault.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the .env file from the project root (parent of backend/) into
 * Spring's Environment BEFORE application.properties is resolved.
 * Handles duplicate keys by keeping the last value (like Python's dotenv).
 * Registered via META-INF/spring.factories.
 */
public class DotenvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = findEnvFile();
        if (envFile == null) {
            System.err.println("[DOTENV] .env file not found, skipping");
            return;
        }

        Map<String, Object> envMap = parseEnvFile(envFile);
        if (!envMap.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", envMap));
            System.out.println("[DOTENV] Loaded " + envMap.size() + " properties from " + envFile);
        }
    }

    private Map<String, Object> parseEnvFile(Path path) {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // Remove surrounding quotes if present
                if (value.length() >= 2 &&
                    ((value.startsWith("\"") && value.endsWith("\"")) ||
                     (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value); // last value wins for duplicates
            }
        } catch (IOException e) {
            System.err.println("[DOTENV] Error reading " + path + ": " + e.getMessage());
        }
        return map;
    }

    private Path findEnvFile() {
        // 1. Project root (parent of backend/)
        Path parent = Path.of("../.env").toAbsolutePath().normalize();
        if (Files.exists(parent)) return parent;

        // 2. Current directory
        Path current = Path.of(".env").toAbsolutePath().normalize();
        if (Files.exists(current)) return current;

        return null;
    }
}
