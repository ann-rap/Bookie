package com.program.bookie.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ClientConfig {
    private static ClientConfig instance;
    private Properties properties;

    private ClientConfig() {
        loadConfig();
    }

    public static synchronized ClientConfig getInstance() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }

    private void loadConfig() {
        properties = new Properties();

        try (FileInputStream input = new FileInputStream("client.properties")) {
            properties.load(input);
            return;
        } catch (IOException e) {
            // File not found in current directory
        }

        // Try JAR directory
        String jarDir = getJarDirectory();
        if (!jarDir.isEmpty()) {
            String configPath = jarDir + "client.properties";
            try (FileInputStream input = new FileInputStream(configPath)) {
                properties.load(input);
                return;
            } catch (IOException e) {
                // File not found in JAR directory
            }
        }

        throw new RuntimeException("Cannot find client.properties file");
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

    public String getServerHost() {
        return properties.getProperty("server.host");
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port"));
    }
}