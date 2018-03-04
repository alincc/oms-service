package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TicketService {
    @Autowired
    private KVLogger kv;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MerchantBackend merchantBackend;

    @Autowired
    private Mapper mapper;

    @Transactional
    public List<TicketDto> create(Order order){
        final List<TicketDto> result = new ArrayList<>();
        final List<Traveller> travellers = order.getTravellers();

        final Set<Integer> merchantIds = order.getLines().stream()
                .map(l -> l.getMerchantId())
                .collect(Collectors.toSet());
        final Map<Integer, MerchantDto> merchants = merchantBackend.search(merchantIds).stream()
                .collect(Collectors.toMap(MerchantDto::getId, Function.identity()));

        for (OrderLine line : order.getLines()){
            int sequenceNumber = 0;
            for (int i=0 ; i<line.getQuantity() ; i++) {
                final Traveller traveller = CollectionUtils.isEmpty(travellers) ? null : travellers.get(i);
                final Ticket ticket = createTicket(++sequenceNumber, line, traveller, merchants);
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

    private Ticket createTicket(
            final int sequenceNumber,
            final OrderLine line,
            final Traveller traveller,
            final Map<Integer, MerchantDto> merchants
    ){
        final String offerToken = line.getOfferToken();
        final TransportationOfferToken token = TransportationOfferToken.decode(offerToken);
        final Date expiryDate = DateUtils.addDays(token.getDepartureDateTime(), 1);
        final MerchantDto merchant = merchants.get(line.getMerchantId());

        final Ticket ticket = new Ticket();
        ticket.setSequenceNumber(sequenceNumber);
        ticket.setOrderLine(line);
        ticket.setPrintDateTime(DateHelper.now());
        ticket.setExpiryDateTime(expiryDate);
        if (traveller != null){
            ticket.setFirstName(traveller.getFirstName());
            ticket.setLastName(traveller.getLastName());
            ticket.setSex(traveller.getSex());
        } else {
            final Order order = line.getOrder();
            ticket.setFirstName(order.getFirstName());
            ticket.setLastName(order.getLastName());
        }

        ticket.setHash(generateHash(sequenceNumber, ticket, merchant));

        ticketRepository.save(ticket);
        return ticket;
    }

    private String generateHash(
            final int sequenceNumber,
            final Ticket ticket,
            final MerchantDto merchant
    ){
        final OrderLine line = ticket.getOrderLine();

        final String str = String.format("%s-%s-%s-%s-%s-%s",
                line.getId(),
                sequenceNumber,
                line.getId(),
                ticket.getFirstName(),
                ticket.getLastName(),
                merchant.getApiKey()
        );
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}

