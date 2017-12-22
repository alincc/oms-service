package io.tchepannou.enigma.oms.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
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
    public CreateOrderResponse create(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.create(request);
    }

    @RequestMapping(value="/{id}/checkout", method = RequestMethod.GET)
    @ApiOperation(value = "Checkout", notes = "Checkout a booking")
    public void checkout(@PathVariable Integer id) {
        orderService.checkout(id);
    }
}
