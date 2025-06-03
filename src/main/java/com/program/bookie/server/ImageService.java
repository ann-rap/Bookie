package com.program.bookie.server;

import com.program.bookie.models.ImageData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ImageService {

    // Folder gdzie przechowujemy obrazy na serwerze
    private static final String IMAGES_DIRECTORY = "server_images";
    private static final String COVERS_DIRECTORY = IMAGES_DIRECTORY + "/covers";

    // Maksymalny rozmiar pliku (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public ImageService() {
        createDirectoriesIfNotExist();
    }

    private void createDirectoriesIfNotExist() {
        try {
            Files.createDirectories(Paths.get(IMAGES_DIRECTORY));
            Files.createDirectories(Paths.get(COVERS_DIRECTORY));
            System.out.println("Image directories created/verified: " + IMAGES_DIRECTORY);
        } catch (IOException e) {
            System.err.println("Error creating image directories: " + e.getMessage());
        }
    }

    /**
     * Zapisuje obraz na serwerze i zwraca nazwę pliku
     */
    public String saveImage(ImageData imageData) throws IOException {
        if (imageData.getImageBytes() == null || imageData.getImageBytes().length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }

        if (imageData.getImageBytes().length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large. Max size: " + MAX_FILE_SIZE + " bytes");
        }

        // Generuj unikalną nazwę pliku
        String extension = getFileExtension(imageData.getFilename());
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path filePath = Paths.get(COVERS_DIRECTORY, uniqueFilename);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageData.getImageBytes());
        }

        System.out.println("Image saved: " + filePath);
        return uniqueFilename;
    }

    /**
     * Pobiera obraz z serwera
     */
    public ImageData getImage(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        // Usuń "covers/" z początku jeśli istnieje (dla kompatybilności z bazą)
        String cleanFilename = filename.startsWith("covers/") ?
                filename.substring(7) : filename;

        Path filePath = Paths.get(COVERS_DIRECTORY, cleanFilename);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Image not found: " + cleanFilename);
        }

        byte[] imageBytes = Files.readAllBytes(filePath);
        String contentType = determineContentType(cleanFilename);

        return new ImageData(cleanFilename, imageBytes, contentType);
    }

    /**
     * Sprawdza czy obraz istnieje
     */
    public boolean imageExists(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String cleanFilename = filename.startsWith("covers/") ?
                filename.substring(7) : filename;

        Path filePath = Paths.get(COVERS_DIRECTORY, cleanFilename);
        return Files.exists(filePath);
    }

    /**
     * Konwertuje plik lokalny na ImageData
     */
    public static ImageData fileToImageData(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        byte[] imageBytes = Files.readAllBytes(file.toPath());
        String filename = file.getName();
        String contentType = determineContentTypeStatic(filename);

        return new ImageData(filename, imageBytes, contentType);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String determineContentType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "image/jpeg";
        }
    }

    private static String determineContentTypeStatic(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "image/jpeg";
        }
    }
}