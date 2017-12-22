package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import io.tchepannou.enigma.refdata.client.exception.TaggedException;

public class OrderException extends TaggedException {
    public OrderException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(final Throwable cause, final ErrorCode errorCode) {
        super(cause, errorCode);
    }
}
