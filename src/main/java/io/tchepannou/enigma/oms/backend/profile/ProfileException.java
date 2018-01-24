package io.tchepannou.enigma.oms.backend.profile;

import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.core.rest.exception.HttpStatusException;

public class ProfileException extends RuntimeException{
    public ProfileException(final HttpException ex){
        super(ex);
    }
    public ProfileException(final String message, final HttpException ex){
        super(message, ex);
    }

    public int getStatusCode (){
        if (getCause() instanceof HttpStatusException){
            return ((HttpStatusException)getCause()).getStatusCode();
        }
        return -1;
    }
}
