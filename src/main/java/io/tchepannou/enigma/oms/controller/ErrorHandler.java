package io.tchepannou.enigma.oms.controller;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.dto.ErrorDto;
import io.tchepannou.enigma.oms.client.exception.NotFoundException;
import io.tchepannou.enigma.oms.client.exception.OrderException;
import io.tchepannou.enigma.oms.client.rr.OMSErrorResponse;
import io.tchepannou.enigma.oms.service.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);

    @Autowired
    private Mapper mapper;

    @Autowired
    private KVLogger kv;


    @ResponseBody
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    protected OMSErrorResponse handleNotFoundException(final NotFoundException ex){
        log("Object not found", ex);

        return createErrorResponse(ex.getErrorCode());
    }

    @ResponseBody
    @ExceptionHandler(OrderException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    protected OMSErrorResponse handleOrderException(final OrderException ex){
        log("Order error", ex);

        return createErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected OMSErrorResponse handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException ex
    ) {
        log("Validation error", ex);

        final List<ErrorDto> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> {
                    final ErrorDto dto = new ErrorDto();
                    dto.setText(e.getDefaultMessage());
                    dto.setCode(OMSErrorCode.VALIDATION_ERROR.getCode());
                    dto.setField(e.getField());
                    return dto;
                })
                .collect(Collectors.toList());
        return createErrorResponse(errors);
    }

    @ResponseBody
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    protected OMSErrorResponse handleThrowable(final Throwable ex){
        log("Unexpected error", ex);

        return createErrorResponse(OMSErrorCode.UNEXPECTED_ERROR);
    }

    private OMSErrorResponse createErrorResponse(OMSErrorCode error){
        return createErrorResponse(Arrays.asList(mapper.toDto(error)));
    }

    private OMSErrorResponse createErrorResponse(List<ErrorDto> errors){
        final OMSErrorResponse response = new OMSErrorResponse();
        response.setErrors(errors);
        return response;
    }

    private void log(final String message, final Throwable ex){
        kv.add(KVLogger.EXCEPTION, ex.getClass().getName());
        kv.add(KVLogger.EXCEPTION_MESSAGE, ex.getMessage());

        LOGGER.error(message, ex);
    }
}
