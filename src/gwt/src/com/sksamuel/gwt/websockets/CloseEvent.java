package com.sksamuel.gwt.websockets;

public class CloseEvent {

    private final short code;
    private final String reason;
    private final boolean wasClean;

    public CloseEvent(short code, String reason, boolean wasClean) {
        this.code = code;
        this.reason = reason;
        this.wasClean = wasClean;
    }

    public short code() {
        return code;
    }

    public String reason() {
        return reason;
    }

    public boolean wasClean() {
        return wasClean;
    }
}