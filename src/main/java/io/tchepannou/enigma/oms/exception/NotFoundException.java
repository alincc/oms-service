package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.oms.client.OMSErrorCode;

public class NotFoundException extends TaggedException{
    public NotFoundException(final OMSErrorCode errorCode) {
        super(errorCode);
    }
}
