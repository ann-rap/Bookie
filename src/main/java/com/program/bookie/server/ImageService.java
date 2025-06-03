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

    // Maksymalny rozmiar pliku (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Rozmiary thumbnailów
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final float JPEG_QUALITY = 0.8f;

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
     * Zapisuje obraz na serwerze, tworzy thumbnail i zwraca nazwę pliku
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

        // Zapisz oryginalny obraz
        Path originalPath = Paths.get(COVERS_DIRECTORY, uniqueFilename);
        try (FileOutputStream fos = new FileOutputStream(originalPath.toFile())) {
            fos.write(imageData.getImageBytes());
        }

        // Stwórz i zapisz thumbnail
        createThumbnail(imageData.getImageBytes(), uniqueFilename);

        System.out.println("Image saved: " + originalPath);
        return uniqueFilename;
    }

    /**
     * Tworzy thumbnail o określonym rozmiarze
     */
    private void createThumbnail(byte[] originalImageBytes, String filename) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalImageBytes)) {
            BufferedImage originalImage = ImageIO.read(bis);

            if (originalImage == null) {
                throw new IOException("Cannot read image data");
            }

            // Oblicz nowe wymiary zachowując proporcje
            Dimension newDimensions = calculateThumbnailSize(
                    originalImage.getWidth(),
                    originalImage.getHeight()
            );

            // Stwórz thumbnail
            BufferedImage thumbnail = new BufferedImage(
                    newDimensions.width,
                    newDimensions.height,
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, newDimensions.width, newDimensions.height, null);
            g2d.dispose();

            // Zapisz thumbnail jako JPEG z kompresją
            Path thumbnailPath = Paths.get(THUMBNAILS_DIRECTORY, filename);
            try (FileOutputStream fos = new FileOutputStream(thumbnailPath.toFile())) {
                ImageIO.write(thumbnail, "jpg", fos);
            }

            System.out.println("Thumbnail created: " + thumbnailPath +
                    " (size: " + newDimensions.width + "x" + newDimensions.height + ")");
        }
    }

    /**
     * Oblicza rozmiar thumbnail zachowując proporcje
     */
    private Dimension calculateThumbnailSize(int originalWidth, int originalHeight) {
        double widthRatio = (double) THUMBNAIL_WIDTH / originalWidth;
        double heightRatio = (double) THUMBNAIL_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        return new Dimension(newWidth, newHeight);
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

    /**
     * Sprawdza czy obraz istnieje
     */
    public boolean imageExists(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String cleanFilename = filename.startsWith("covers/") ?
                filename.substring(7) : filename;

        Path thumbnailPath = Paths.get(THUMBNAILS_DIRECTORY, cleanFilename);
        Path originalPath = Paths.get(COVERS_DIRECTORY, cleanFilename);

        return Files.exists(thumbnailPath) || Files.exists(originalPath);
    }

    /**
     * Czyści cache
     */
    public void clearCache() {
        imageCache.clear();
        System.out.println("Image cache cleared");
    }

    /**
     * Zwraca statystyki cache
     */
    public String getCacheStats() {
        return String.format("Cache: %d/%d images", imageCache.size(), MAX_CACHE_SIZE);
    }

    /**
     * Konwertuje plik lokalny na ImageData z automatycznym tworzeniem thumbnail
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

    /**
     * Tworzy thumbnail bezpośrednio z pliku
     */
    public static ImageData createThumbnailFromFile(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Cannot read image file: " + file.getName());
        }

        // Oblicz wymiary thumbnail
        double widthRatio = (double) THUMBNAIL_WIDTH / originalImage.getWidth();
        double heightRatio = (double) THUMBNAIL_HEIGHT / originalImage.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalImage.getWidth() * ratio);
        int newHeight = (int) (originalImage.getHeight() * ratio);

        // Stwórz thumbnail
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Konwertuj do byte array
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] thumbnailBytes = baos.toByteArray();

            return new ImageData(file.getName(), thumbnailBytes, "image/jpeg");
        }
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