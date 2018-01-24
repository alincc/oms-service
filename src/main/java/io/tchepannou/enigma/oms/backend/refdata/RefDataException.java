package io.tchepannou.enigma.oms.backend.refdata;

import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.core.rest.exception.HttpStatusException;

public class RefDataException extends RuntimeException{
    public RefDataException(final HttpException ex){
        super(ex);
    }
    public RefDataException(final String message, final HttpException ex){
        super(message, ex);
    }

    public int getStatusCode (){
        if (getCause() instanceof HttpStatusException){
            return ((HttpStatusException)getCause()).getStatusCode();
        }
        return -1;
    }
}
