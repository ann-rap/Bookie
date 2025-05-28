package com.program.bookie.models;


public class Request implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private RequestType type;
    private Object data;


    public Request(RequestType type, Object data) {
        this.type = type;
        this.data = data;
    }


    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }


    @Override
    public String toString() {
        return "Request{type=" + type + ", data=" + data + "}";
    }
}
