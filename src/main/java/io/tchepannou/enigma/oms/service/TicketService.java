package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

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
        for (OrderLine line : order.getLines()){
            for (int i=0 ; i<line.getQuantity() ; i++) {
                final Ticket ticket = createTicket(line);
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

    private Ticket createTicket(final OrderLine line){
        final Ticket ticket = new Ticket();
        final String offerToken = line.getOfferToken();
        final TransportationOfferToken token = TransportationOfferToken.decode(offerToken);
        final Date expiryDate = DateUtils.addDays(token.getDepartureDateTime(), 1);

        ticket.setBookingId(line.getBookingId());
        ticket.setMerchantId(line.getMerchantId());
        ticket.setOrder(line.getOrder());
        ticket.setPrintDateTime(DateHelper.now());
        ticket.setExpiryDateTime(expiryDate);
        ticket.setOfferToken(line.getOfferToken());

        ticket.setHash(generateHash(ticket));

        ticketRepository.save(ticket);
        return ticket;
    }

    private String generateHash(final Ticket ticket){
        final String str = String.format("%s-%s-%s-%s-%s-%s",
                ticket.getBookingId(),
                ticket.getMerchantId(),
                ticket.getOrder().getId(),
                ticket.getPrintDateTime().getTime(),
                ticket.getExpiryDateTime().getTime(),
                ticket.getOfferToken()
        );
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}

