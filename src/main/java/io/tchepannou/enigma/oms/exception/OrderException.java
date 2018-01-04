package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.oms.client.OMSErrorCode;

public class OrderException extends TaggedException {
    public OrderException(final String message, final Throwable cause, final OMSErrorCode errorCode) {
        super(message, cause, errorCode);
    }

    public OrderException(final Throwable cause, final OMSErrorCode errorCode) {
        super(cause, errorCode);
    }
}
