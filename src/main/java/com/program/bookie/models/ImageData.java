package com.program.bookie.models;

import java.io.Serializable;

public class ImageData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filename;
    private byte[] imageBytes;
    private String contentType;

    public ImageData() {}

    public ImageData(String filename, byte[] imageBytes, String contentType) {
        this.filename = filename;
        this.imageBytes = imageBytes;
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getSize() {
        return imageBytes != null ? imageBytes.length : 0;
    }

    @Override
    public String toString() {
        return "ImageData{" +
                "filename='" + filename + '\'' +
                ", size=" + getSize() + " bytes" +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}