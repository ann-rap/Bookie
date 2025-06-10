package com.program.bookie.client;

import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;

public class Client {
    private static Client instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 999;
    private boolean connected = false;

    //operacje w tle
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    // Cache dla obrazów
    private final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    private Client() {

    }

    public static synchronized Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public boolean connect() {
        try {
            if (socket != null && !socket.isClosed() && connected) {
                return true; // Już połączony
            }

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;
            System.out.println("Connected to server: " + SERVER_HOST + ":" + SERVER_PORT);
            return true;
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public synchronized Response sendRequest(Request request) {
        try {
            if (!connected || socket == null || socket.isClosed()) {
                if (!connect()) {
                    return new Response(ResponseType.ERROR, "Cannot connect to server");
                }
            }

            output.writeObject(request);
            output.flush();
            return (Response) input.readObject();
        } catch (Exception e) {
            System.err.println("Communication error: " + e.getMessage());
            connected = false;
            return new Response(ResponseType.ERROR, "Communication error with server");
        }
    }

    public void disconnect() {
        try {
            //WAZNE: Zamkniecie puli watkow
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
            if (connected && socket != null && !socket.isClosed()) {
                try {
                    Request disconnectRequest = new Request(RequestType.DISCONNECT, null);
                    output.writeObject(disconnectRequest);
                    output.flush();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("Error sending disconnect request: " + e.getMessage());
                }

                if (output != null) output.close();
                if (input != null) input.close();
                socket.close();
                System.out.println("Disconnected from server");
            }
        } catch (IOException e) {
            System.err.println("Disconnect error: " + e.getMessage());
        } finally {
            connected = false;
            socket = null;
            output = null;
            input = null;
        }

    }

    /**
     * Pobiera obraz z serwera z cache'owaniem
     */
    public ImageData getImage(String filename) {
        try {
            Request request = new Request(RequestType.GET_IMAGE, filename);
            Response response = sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                return (ImageData) response.getData();
            } else {
                System.err.println("Error getting image: " + response.getData());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Exception getting image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Pobiera obraz jako JavaFX Image z cache'owaniem
     */
    public Image getImageFX(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }

        // Sprawdź cache
        Image cachedImage = imageCache.get(filename);
        if (cachedImage != null) {
            System.out.println("Image loaded from CLIENT cache: " + filename);
            return cachedImage;
        }

        // Pobierz z serwera
        ImageData imageData = getImage(filename);
        if (imageData != null && imageData.getImageBytes() != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(imageData.getImageBytes());
                Image fxImage = new Image(bis);

                // Dodaj do cache jeśli nie jest za duży
                if (imageCache.size() < MAX_CACHE_SIZE) {
                    imageCache.put(filename, fxImage);
                    System.out.println("Image cached on CLIENT: " + filename +
                            " (cache size: " + imageCache.size() + "/" + MAX_CACHE_SIZE + ")");
                }

                return fxImage;
            } catch (Exception e) {
                System.err.println("Error creating JavaFX Image: " + e.getMessage());
                return null;
            }
        }

        return null;
    }
    // Prosta metoda asynchroniczna - tylko podstawowe callbacki
    public void executeAsync(Request request, Runnable onSuccess, Runnable onError) {
        Callable<Response> callable = () -> {
            synchronized (this) {
                return sendRequest(request);
            }
        };

        Future<Response> future = executorService.submit(callable);

        executorService.submit(() -> {
            try {
                Response response = future.get();
                Platform.runLater(() -> {
                    if (response.getType() == ResponseType.SUCCESS) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onError != null) onError.run();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (onError != null) onError.run();
                });
            }
        });
    }

    // Metoda gdy potrzebujesz dostępu do Response
    public void executeAsyncWithData(Request request, ResponseHandler handler) {
        Callable<Response> callable = () -> {
            synchronized (this) {
                return sendRequest(request);
            }
        };

        Future<Response> future = executorService.submit(callable);

        executorService.submit(() -> {
            try {
                Response response = future.get();
                Platform.runLater(() -> handler.handle(response));
            } catch (Exception e) {
                Platform.runLater(() -> handler.handleError(e));
            }
        });
    }

    // Prosty interface - tylko jeden
    public interface ResponseHandler {
        void handle(Response response);
        default void handleError(Exception e) {
            System.err.println("Request error: " + e.getMessage());
        }
    }

    /**
     * Czyści cache obrazów
     */
    public void clearImageCache() {
        imageCache.clear();
        System.out.println("Client image cache cleared");
    }

    /**
     * Zwraca statystyki cache
     */
    public String getCacheStats() {
        return String.format("Client cache: %d/%d images", imageCache.size(), MAX_CACHE_SIZE);
    }

    /**
     * Sprawdza czy obraz jest w cache
     */
    public boolean isImageCached(String filename) {
        return imageCache.containsKey(filename);
    }

    /**
     * Wstępnie ładuje obrazy do cache (przydatne dla książek widocznych na ekranie)
     */
    public void preloadImages(String... filenames) {
        for (String filename : filenames) {
            if (filename != null && !isImageCached(filename)) {
                new Thread(() -> {
                    try {
                        getImageFX(filename);
                    } catch (Exception e) {
                        System.err.println("Error preloading image " + filename + ": " + e.getMessage());
                    }
                }).start();
            }
        }
    }

    /**
     * Sprawdza czy klient jest połączony
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}