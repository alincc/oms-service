package io.tchepannou.enigma.oms.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.tchepannou.enigma.oms.client.rr.GetTicketsResponse;
import io.tchepannou.enigma.oms.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value="/v1/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/v1/tickets", description = "Ticket API")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @RequestMapping(value="/{ticketIds}", method = RequestMethod.GET)
    @ApiOperation(value = "Find")
    public GetTicketsResponse findById(
            @PathVariable @ApiParam("comma separated list of Ids of the tickets to resolve") String ticketIds
    ) {
        final List<Integer> ids = Arrays.stream(ticketIds.split(","))
                .map(i -> Integer.valueOf(i))
                .collect(Collectors.toList());
        return ticketService.findByIds(ids);
    }
}
