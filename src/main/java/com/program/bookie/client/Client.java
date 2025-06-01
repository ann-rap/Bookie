package com.program.bookie.client;

import com.program.bookie.models.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    private static Client instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 999;
    private boolean connected = false;




    public static synchronized Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Błąd połączenia: " + e.getMessage());
            return false;
        }
    }

    public Response sendRequest(Request request) {
        try {
            output.writeObject(request);
            output.flush();
            return (Response) input.readObject();
        } catch (Exception e) {
            System.err.println("Błąd komunikacji: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Błąd komunikacji z serwerem");
        }
    }

    public void disconnect() {
        try {
            if (connected && socket != null && !socket.isClosed()) {
                // Send disconnect request to server
                try {
                    Request disconnectRequest = new Request(RequestType.DISCONNECT, null);
                    output.writeObject(disconnectRequest);
                    output.flush();

                    // Give the server a moment to process the disconnect
                    Thread.sleep(100);
                } catch (Exception e) {
                    // Ignore errors when sending disconnect request
                    System.out.println("Error sending disconnect request: " + e.getMessage());
                }

                // Close connections
                if (output != null) output.close();
                if (input != null) input.close();
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Błąd rozłączania: " + e.getMessage());
        } finally {
            connected = false;
            socket = null;
            output = null;
            input = null;
        }
    }

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
}