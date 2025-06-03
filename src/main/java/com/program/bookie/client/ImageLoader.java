package com.program.bookie.client;

import com.program.bookie.models.Book;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Centralna klasa do ładowania i zarządzania obrazami w JavaFX
 * Obsługuje cache, fallback do lokalnych zasobów i różne rozmiary obrazów
 */
public class ImageLoader {

    private final Client client;

    // Predefiniowane rozmiary
    public static class ImageSize {
        public final double width;
        public final double height;

        private ImageSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        // Standardowe rozmiary dla różnych części aplikacji
        public static final ImageSize BOOK_CARD = new ImageSize(120, 160);
        public static final ImageSize SEARCH_RESULT = new ImageSize(150, 200);
        public static final ImageSize BOOK_DETAILS = new ImageSize(200, 251);
        public static final ImageSize REVIEW_COVER = new ImageSize(164, 202);
    }

    public ImageLoader(Client client) {
        this.client = client;
    }

    /**
     * Ładuje okładkę książki do ImageView z automatyczną optymalizacją
     * @param book książka
     * @param imageView docelowy ImageView
     * @param size rozmiar obrazu
     */
    public void loadBookCover(Book book, ImageView imageView, ImageSize size) {
        loadBookCover(book, imageView, size, false);
    }

    /**
     * Ładuje okładkę książki do ImageView z opcjonalnym viewport
     * @param book książka
     * @param imageView docelowy ImageView
     * @param size rozmiar obrazu
     * @param useViewport czy używać viewport do przycinania
     */
    public void loadBookCover(Book book, ImageView imageView, ImageSize size, boolean useViewport) {
        if (imageView == null || book == null) {
            return;
        }

        String imagePath = book.getCoverImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultCover(imageView, size);
            return;
        }

        // Skonfiguruj ImageView
        setupImageView(imageView, size);

        // Spróbuj z cache
        Image cachedImage = client.getImageFX(imagePath);
        if (cachedImage != null) {
            setImageToView(imageView, cachedImage, size, useViewport);
            logImageLoad("CACHE", imagePath);
            return;
        }

        // Załaduj w tle
        loadImageAsync(imagePath, imageView, size, useViewport);
    }

    /**
     * Asynchroniczne ładowanie obrazu z serwera z fallback do lokalnych zasobów
     */
    private void loadImageAsync(String imagePath, ImageView imageView, ImageSize size, boolean useViewport) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Spróbuj z serwera
                Image serverImage = client.getImageFX(imagePath);
                if (serverImage != null) {
                    return new ImageResult(serverImage, "SERVER");
                }

                // Fallback do lokalnych zasobów
                Image localImage = loadFromLocalResources(imagePath);
                if (localImage != null) {
                    return new ImageResult(localImage, "LOCAL");
                }

                return null;

            } catch (Exception e) {
                System.err.println("Error loading image " + imagePath + ": " + e.getMessage());
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null) {
                    setImageToView(imageView, result.image, size, useViewport);
                    logImageLoad(result.source, imagePath);
                } else {
                    setDefaultCover(imageView, size);
                    logImageLoad("DEFAULT", imagePath);
                }
            });
        });
    }

    /**
     * Klasa pomocnicza do przechowywania wyniku ładowania obrazu
     */
    private static class ImageResult {
        final Image image;
        final String source;

        ImageResult(Image image, String source) {
            this.image = image;
            this.source = source;
        }
    }

    /**
     * Ładuje obraz z lokalnych zasobów próbując różne ścieżki
     */
    private Image loadFromLocalResources(String imagePath) {
        String[] possiblePaths = {
                "/img/" + imagePath,
                "/img/covers/" + imagePath,
                "/img/covers" + imagePath
        };

        for (String path : possiblePaths) {
            try {
                URL imageUrl = getClass().getResource(path);
                if (imageUrl != null) {
                    return new Image(imageUrl.toString());
                }
            } catch (Exception e) {
                // Próbuj następną ścieżkę
            }
        }

        return null;
    }

    /**
     * Konfiguruje podstawowe właściwości ImageView
     */
    private void setupImageView(ImageView imageView, ImageSize size) {
        imageView.setFitWidth(size.width);
        imageView.setFitHeight(size.height);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
    }

    /**
     * Ustawia obraz w ImageView z opcjonalnym viewport
     */
    private void setImageToView(ImageView imageView, Image image, ImageSize size, boolean useViewport) {
        imageView.setImage(image);

        if (useViewport && image != null) {
            setImageViewport(imageView, image, size);
        }
    }

    /**
     * Ustawia viewport dla lepszego kadrowania obrazu
     */
    private void setImageViewport(ImageView imageView, Image image, ImageSize size) {
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        if (imageWidth > 0 && imageHeight > 0) {
            double scaleX = imageWidth / size.width;
            double scaleY = imageHeight / size.height;
            double scale = Math.min(scaleX, scaleY);

            double viewportWidth = size.width * scale;
            double viewportHeight = size.height * scale;
            double viewportX = Math.max(0, (imageWidth - viewportWidth) / 2);
            double viewportY = Math.max(0, (imageHeight - viewportHeight) / 2);

            imageView.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
        }
    }

    /**
     * Ustawia domyślny wygląd dla ImageView gdy brak obrazu
     */
    private void setDefaultCover(ImageView imageView, ImageSize size) {
        imageView.setImage(null);
        imageView.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc; -fx-border-width: 1;");
        setupImageView(imageView, size);
    }

    /**
     * Loguje informacje o ładowaniu obrazu
     */
    private void logImageLoad(String source, String imagePath) {
        System.out.println("Image loaded from " + source + ": " + imagePath);
    }

    /**
     * Preloaduje obrazy w tle dla lepszej wydajności
     */
    public void preloadImages(String... imagePaths) {
        if (imagePaths == null || imagePaths.length == 0) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            for (String imagePath : imagePaths) {
                if (imagePath != null && !imagePath.isEmpty() && !client.isImageCached(imagePath)) {
                    try {
                        client.getImageFX(imagePath);
                        // Małe opóźnienie między obrazami
                        Thread.sleep(50);
                    } catch (Exception e) {
                        System.err.println("Error preloading image " + imagePath + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Preloaduje obrazy z listy książek
     */
    public void preloadBookCovers(java.util.List<Book> books) {
        String[] imagePaths = books.stream()
                .map(Book::getCoverImagePath)
                .filter(path -> path != null && !path.isEmpty())
                .toArray(String[]::new);

        preloadImages(imagePaths);
    }

    /**
     * Czyści cache i resetuje stan
     */
    public void clearCache() {
        client.clearImageCache();
    }

    /**
     * Zwraca statystyki cache
     */
    public String getCacheStats() {
        return client.getCacheStats();
    }
}