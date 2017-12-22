package io.tchepannou.enigma.oms.exception;

import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import io.tchepannou.enigma.refdata.client.exception.TaggedException;

public class DownstreamException extends TaggedException{
    private String details;

    public DownstreamException(
            final Throwable cause,
            final ErrorCode errorCode,
            final String details
    ) {
        super(cause, errorCode);

        this.details = details;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " - " + details;
    }

    public String getDetails() {
        return details;
    }
}
