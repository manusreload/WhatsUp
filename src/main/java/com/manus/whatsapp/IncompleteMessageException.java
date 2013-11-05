package com.manus.whatsapp;

public class IncompleteMessageException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 42L * 63L;
    private byte[] data;

    public IncompleteMessageException(String message, byte[] input) {
        super(message);
        this.data = input;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
