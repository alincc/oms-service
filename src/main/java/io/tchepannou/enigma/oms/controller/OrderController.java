package io.tchepannou.enigma.oms.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.tchepannou.core.rest.Headers;
import io.tchepannou.enigma.oms.client.rr.CancelOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CancelOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.ExpireOrderResponse;
import io.tchepannou.enigma.oms.client.rr.GetOrderResponse;
import io.tchepannou.enigma.oms.client.rr.OMSErrorResponse;
import io.tchepannou.enigma.oms.client.rr.RefundOrderResponse;
import io.tchepannou.enigma.oms.service.QueueNames;
import io.tchepannou.enigma.oms.service.order.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Create")
    public CreateOrderResponse create(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.create(request);
    }

    @RequestMapping(value="/{orderId}/checkout", method = RequestMethod.POST)
    @ApiOperation(value = "Checkout")
    @ApiResponses({
            @ApiResponse(code=200, message = "Success"),
            @ApiResponse(code=404, message = "Order not found", response = OMSErrorResponse.class),
            @ApiResponse(code=409, message = "Checkout error", response = OMSErrorResponse.class)
    })
    public CheckoutOrderResponse checkout(
            @PathVariable Integer orderId,
            @RequestHeader(name= Headers.DEVICE_UID, required = false) String deviceUID,
            @RequestBody @Valid CheckoutOrderRequest request
    ) {
        final CheckoutOrderResponse response = orderService.checkout(orderId, deviceUID, request);
        rabbitTemplate.convertAndSend(QueueNames.EXCHANGE_ORDER_CONFIRMED, "", orderId);
        return response;
    }

    @RequestMapping(value="/{orderId}/cancel", method = RequestMethod.POST)
    @ApiOperation(value = "Cancel Order")
    public CancelOrderResponse cancel(@PathVariable Integer orderId, @Valid @RequestBody CancelOrderRequest request) {
        CancelOrderResponse response = orderService.cancel(orderId, request);
        rabbitTemplate.convertAndSend(QueueNames.EXCHANGE_ORDER_CANCELLED, "", orderId);
        return response;
    }

    @RequestMapping(value="/{orderId}/refund", method = RequestMethod.GET)
    @ApiOperation(value = "Refund a cancelled order")
    public RefundOrderResponse refund(@PathVariable Integer orderId) {
        return orderService.refund(orderId);
    }

    @RequestMapping(value="/{orderId}/expire", method = RequestMethod.GET)
    @ApiOperation(value = "Cancel Order")
    public ExpireOrderResponse expire(@PathVariable Integer orderId) {
        return orderService.expire(orderId);
    }

    @RequestMapping(value="/{orderId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get")
    public GetOrderResponse findById(@PathVariable Integer orderId) {
        return orderService.findById(orderId);
    }
}
