package io.tchepannou.enigma.oms.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.tchepannou.enigma.oms.client.rr.GetTicketResponse;
import io.tchepannou.enigma.oms.client.rr.SendSmsResponse;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/v1/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/v1/tickets", description = "Ticket API")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    @ApiOperation("findById")
    public GetTicketResponse findById(@PathVariable Integer id) {
        return ticketService.findById(id);
    }

    @RequestMapping(value="/{id}/sms", method = RequestMethod.GET)
    @ApiOperation("sms")
    public SendSmsResponse sms(@PathVariable Integer id){
        return ticketService.sms(id);
    }
}
