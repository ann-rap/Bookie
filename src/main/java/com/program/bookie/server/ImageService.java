package com.program.bookie.server;

import com.program.bookie.models.ImageData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ImageService {

    // Folder gdzie przechowujemy obrazy na serwerze
    private static final String IMAGES_DIRECTORY = "server_images";
    private static final String COVERS_DIRECTORY = IMAGES_DIRECTORY + "/covers";
    private static final String THUMBNAILS_DIRECTORY = IMAGES_DIRECTORY + "/thumbnails";


    // Cache w pamięci dla często używanych obrazów
    private final ConcurrentHashMap<String, ImageData> imageCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    public ImageService() {
        createDirectoriesIfNotExist();
    }

    private void createDirectoriesIfNotExist() {
        try {
            Files.createDirectories(Paths.get(IMAGES_DIRECTORY));
            Files.createDirectories(Paths.get(COVERS_DIRECTORY));
            Files.createDirectories(Paths.get(THUMBNAILS_DIRECTORY));
            System.out.println("Image directories created/verified: " + IMAGES_DIRECTORY);
        } catch (IOException e) {
            System.err.println("Error creating image directories: " + e.getMessage());
        }
    }


    /**
     * Pobiera obraz z serwera (preferuje thumbnail)
     */
    public ImageData getImage(String filename) throws IOException {
        return getImage(filename, true);
    }

    /**
     * Pobiera obraz z serwera
     * @param filename nazwa pliku
     * @param preferThumbnail czy preferować thumbnail (mniejszy rozmiar)
     */
    public ImageData getImage(String filename, boolean preferThumbnail) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        // Sprawdź cache
        String cacheKey = filename + (preferThumbnail ? "_thumb" : "_full");
        ImageData cachedImage = imageCache.get(cacheKey);
        if (cachedImage != null) {
            System.out.println("Image loaded from cache: " + filename);
            return cachedImage;
        }

        // Usuń "covers/" z początku jeśli istnieje
        String cleanFilename = filename.startsWith("covers/") ?
                filename.substring(7) : filename;

        Path imagePath;
        String contentType;

        if (preferThumbnail) {
            // Spróbuj najpierw thumbnail
            imagePath = Paths.get(THUMBNAILS_DIRECTORY, cleanFilename);
            if (!Files.exists(imagePath)) {
                // Jeśli thumbnail nie istnieje, użyj oryginału
                imagePath = Paths.get(COVERS_DIRECTORY, cleanFilename);
            }
        } else {
            // Użyj oryginału
            imagePath = Paths.get(COVERS_DIRECTORY, cleanFilename);
        }

        if (!Files.exists(imagePath)) {
            throw new FileNotFoundException("Image not found: " + cleanFilename);
        }

        byte[] imageBytes = Files.readAllBytes(imagePath);
        contentType = determineContentType(cleanFilename);

        ImageData imageData = new ImageData(cleanFilename, imageBytes, contentType);

        // Dodaj do cache (jeśli nie jest za duży)
        if (imageCache.size() < MAX_CACHE_SIZE) {
            imageCache.put(cacheKey, imageData);
        }

        System.out.println("Image loaded from disk: " + imagePath +
                " (size: " + imageBytes.length + " bytes)");

        return imageData;
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

}