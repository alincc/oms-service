package io.tchepannou.enigma.oms.backend.tontine;

import io.tchepannou.core.rest.exception.HttpException;

public class TontineException extends RuntimeException{
    public TontineException(final String message, final HttpException ex){
        super(message, ex);
    }
}
