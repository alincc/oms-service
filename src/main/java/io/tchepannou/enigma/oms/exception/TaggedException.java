package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.oms.client.OMSErrorCode;

public class TaggedException extends RuntimeException {
    private OMSErrorCode errorCode;

    public TaggedException(final OMSErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public TaggedException(final Throwable cause, final OMSErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public OMSErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return errorCode.getCode() + ": " + errorCode.getText();
    }
}
