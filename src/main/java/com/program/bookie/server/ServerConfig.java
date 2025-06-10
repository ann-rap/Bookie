package com.program.bookie.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {
    private static ServerConfig instance;
    private Properties properties;

    private ServerConfig() {
        loadConfig();
    }

    public static synchronized ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }

    private void loadConfig() {
        properties = new Properties();

        // Try current directory first (development or JAR directory)
        try (FileInputStream input = new FileInputStream("server.properties")) {
            properties.load(input);
            return;
        } catch (IOException e) {
            // File not found in current directory
        }

        // Try JAR directory
        String jarDir = getJarDirectory();
        if (!jarDir.isEmpty()) {
            String configPath = jarDir + "server.properties";
            try (FileInputStream input = new FileInputStream(configPath)) {
                properties.load(input);
                return;
            } catch (IOException e) {
                // File not found in JAR directory
            }
        }

        throw new RuntimeException("Cannot find server.properties file");
    }

    private String getJarDirectory() {
        try {
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (jarPath.endsWith(".jar")) {
                return jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port"));
    }

    public String getDatabaseHost() {
        return properties.getProperty("database.host");
    }

    public int getDatabasePort() {
        return Integer.parseInt(properties.getProperty("database.port"));
    }

    public String getDatabaseName() {
        return properties.getProperty("database.name");
    }

    public String getDatabaseUsername() {
        return properties.getProperty("database.username");
    }

    public String getDatabasePassword() {
        return properties.getProperty("database.password");
    }

    public String getFullDatabaseUrl() {
        return String.format("jdbc:mysql://%s:%d/%s",
                getDatabaseHost(), getDatabasePort(), getDatabaseName());
    }
}