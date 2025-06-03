package com.program.bookie.models;


public class Response implements java.io.Serializable {
    private static final long serialVersionUID = 2L;

    private ResponseType type;
    private Object data;
    private String message; // Dodatkowa wiadomość tekstowa


    public Response(ResponseType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Response(ResponseType type, Object data, String message) {
        this.type = type;
        this.data = data;
        this.message = message;
    }

    // Metody pomocnicze do tworzenia odpowiedzi
    public static Response success(Object data) {
        return new Response(ResponseType.SUCCESS, data);
    }

    public static Response success(Object data, String message) {
        return new Response(ResponseType.SUCCESS, data, message);
    }

    public static Response error(String errorMessage) {
        return new Response(ResponseType.ERROR, null, errorMessage);
    }

    public static Response info(String infoMessage) {
        return new Response(ResponseType.INFO, null, infoMessage);
    }

    // Gettery i settery
    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Response{type=" + type + ", message='" + message + "', data=" + data + "}";
    }
}