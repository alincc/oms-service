package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.oms.client.OMSErrorCode;

public class OrderException extends TaggedException {
    public OrderException(final OMSErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(final Throwable cause, final OMSErrorCode errorCode) {
        super(cause, errorCode);
    }
}
