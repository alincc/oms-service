package io.tchepannou.enigma.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.enigma.ferari.client.rr.ErrorResponse;
import io.tchepannou.enigma.oms.client.dto.ErrorDto;
import io.tchepannou.enigma.oms.client.rr.OMSErrorResponse;
import io.tchepannou.enigma.oms.exception.DownstreamException;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.exception.OrderException;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
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
    private ObjectMapper objectMapper;

    @ResponseBody
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    protected OMSErrorResponse handleNotFoundException(final NotFoundException ex){
        LOGGER.error("Object not found", ex);

        return createErrorResponse(ex.getErrorCode());
    }

    @ResponseBody
    @ExceptionHandler(DownstreamException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    protected OMSErrorResponse handleDownstreamException(final DownstreamException ex){
        LOGGER.error("Checkout error - details={}", ex.getDetails(), ex);

        OMSErrorResponse response = new OMSErrorResponse();
        try{
            final ErrorResponse errorResponse = objectMapper.readValue(ex.getDetails(), ErrorResponse.class);
            if (errorResponse.getErrors() != null){
                response.setErrors(
                    errorResponse.getErrors().stream()
                        .map(e -> {
                            ErrorDto error = new ErrorDto();
                            error.setField(e.getField());
                            error.setCode(e.getCode());
                            error.setText(e.getText());
                            return error;
                        })
                        .collect(Collectors.toList())
                );
            }
        }catch(Exception e){
        }

        return response;
    }

    @ResponseBody
    @ExceptionHandler(OrderException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    protected OMSErrorResponse handleOrderException(final OrderException ex){
        LOGGER.error("Order error", ex);

        return createErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected OMSErrorResponse handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException ex
    ) {
        final List<ErrorDto> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> {
                    final ErrorDto dto = new ErrorDto();
                    dto.setText(e.getDefaultMessage());
                    dto.setCode(ErrorCode.VALIDATION_ERROR.getCode());
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
        LOGGER.error("Unexpected error", ex);

        return createErrorResponse(ErrorCode.UNEXPECTED_ERROR);
    }

    private OMSErrorResponse createErrorResponse(ErrorCode error){
        return createErrorResponse(Arrays.asList(mapper.toDto(error)));
    }
    private OMSErrorResponse createErrorResponse(List<ErrorDto> errors){
        final OMSErrorResponse response = new OMSErrorResponse();
        response.setErrors(errors);
        return response;
    }
}
