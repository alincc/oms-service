package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.rr.GetTicketResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TicketService {
    @Autowired
    private KVLogger kv;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private Mapper mapper;

    @Transactional
    public List<TicketDto> create(Order order){
        final List<TicketDto> result = new ArrayList<>();
        final List<Traveller> travellers = order.getTravellers();

        for (OrderLine line : order.getLines()){
            int sequenceNumber = 0;
            for (int i=0 ; i<line.getQuantity() ; i++) {
                final Traveller traveller = CollectionUtils.isEmpty(travellers) ? null : travellers.get(i);
                final Ticket ticket = createTicket(++sequenceNumber, line, traveller);
                final TicketDto dto = mapper.toDto(ticket);
                result.add(dto);
            }
        }

        kv.add("TicketCount", result.size());
        kv.add("TicketId", result.stream()
                .map(TicketDto::getId)
                .collect(Collectors.toList())
        );
        return result;
    }

    public List<TicketDto> findByOrder(Order order){
        return ticketRepository.findByOrder(order)
                .stream()
                .map(t -> mapper.toDto(t))
                .collect(Collectors.toList());
    }

    public GetTicketResponse findById(Integer id){
        final Ticket ticket = ticketRepository.findOne(id);
        if (ticket == null){
            throw new NotFoundException(OMSErrorCode.TICKET_NOT_FOUND);
        }

        /* make sure order is not confirmed */
        final Order order = ticket.getOrderLine().getOrder();
        if (!OrderStatus.CONFIRMED.equals(order.getStatus())){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_CONFIRMED);
        }

        return new GetTicketResponse(mapper.toDto(ticket));
    }

    private Ticket createTicket(
            final int sequenceNumber,
            final OrderLine line,
            final Traveller traveller
    ){
        final String offerToken = line.getOfferToken();
        final TransportationOfferToken token = TransportationOfferToken.decode(offerToken);
        final Date expiryDate = DateUtils.addDays(token.getDepartureDateTime(), 1);

        final Ticket ticket = new Ticket();
        ticket.setSequenceNumber(sequenceNumber);
        ticket.setOrderLine(line);
        ticket.setPrintDateTime(DateHelper.now());
        ticket.setExpiryDateTime(expiryDate);
        if (traveller != null){
            ticket.setFirstName(traveller.getFirstName());
            ticket.setLastName(traveller.getLastName());
        } else {
            final Order order = line.getOrder();
            ticket.setFirstName(order.getFirstName());
            ticket.setLastName(order.getLastName());
        }

        ticketRepository.save(ticket);
        return ticket;
    }
}

