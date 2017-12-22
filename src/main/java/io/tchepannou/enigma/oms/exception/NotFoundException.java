package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import io.tchepannou.enigma.refdata.client.exception.TaggedException;

public class NotFoundException extends TaggedException{
    public NotFoundException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
