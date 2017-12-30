package io.tchepannou.enigma.oms.service.ferari;

import io.tchepannou.core.rest.exception.HttpException;

public class FerrariException extends RuntimeException{
    public FerrariException(final String message, final HttpException ex){
        super(message, ex);
    }
}
