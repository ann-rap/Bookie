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
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Błąd rozłączania: " + e.getMessage());
        }
    }
}