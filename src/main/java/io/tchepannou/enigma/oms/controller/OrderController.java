package io.tchepannou.enigma.oms.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.OMSErrorResponse;
import io.tchepannou.enigma.oms.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping(value="/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/v1/orders", description = "Order API")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Create", notes = "Perform a booking")
    @ApiResponses({
            @ApiResponse(code=200, message = "Success"),
            @ApiResponse(code=400, message = "Invalid request", response = OMSErrorResponse.class),
            @ApiResponse(code=404, message = "Party not found", response = OMSErrorResponse.class),
            @ApiResponse(code=409, message = "Failure when creating the order", response = OMSErrorResponse.class)
    })
    public CreateOrderResponse create(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.create(request);
    }

    @RequestMapping(value="/{id}/checkout", method = RequestMethod.GET)
    @ApiOperation(value = "Checkout", notes = "Checkout a booking")
    @ApiResponses({
            @ApiResponse(code=200, message = "Success"),
            @ApiResponse(code=404, message = "Order not found", response = OMSErrorResponse.class),
            @ApiResponse(code=409, message = "Checkout error", response = OMSErrorResponse.class)
    })
    public void checkout(@PathVariable Integer id) {
        orderService.checkout(id);
    }
}
