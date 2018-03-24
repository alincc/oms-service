package io.tchepannou.enigma.oms.service.ticket;

import com.google.common.annotations.VisibleForTesting;
import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.exception.HttpNotFoundException;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.rr.GetTicketResponse;
import io.tchepannou.enigma.oms.client.rr.SendSmsResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.mq.QueueNames;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TicketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    @Autowired
    private KVLogger kv;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketSmsSender smsSender;

    @Autowired
    private Mapper mapper;

    @Value("${server.port}")
    private int port;

    @Transactional
    public List<TicketDto> create(Order order){
        final List<TicketDto> result = new ArrayList<>();
        final List<Traveller> travellers = order.getTravellers();

        for (OrderLine line : order.getLines()){
            int sequenceNumber = 0;
            for (int i=0 ; i<line.getQuantity() ; i++) {
                final Traveller traveller = CollectionUtils.isEmpty(travellers) ? null : travellers.get(i);
                final Ticket ticket = createTicket(++sequenceNumber, line, traveller);
                final TicketDto dto = mapper.toTicketDto(ticket);
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
                .map(t -> mapper.toTicketDto(t))
                .collect(Collectors.toList());
    }

    public SendSmsResponse sms(Integer id) {
        final Ticket ticket = ticketRepository.findOne(id);
        if (ticket == null){
            throw new NotFoundException(OMSErrorCode.TICKET_NOT_FOUND);
        }

        final String transactionId = smsSender.send(ticket);
        return new SendSmsResponse(transactionId);
    }

    public GetTicketResponse findById(Integer id){
        final Ticket ticket = ticketRepository.findOne(id);
        if (ticket == null){
            throw new NotFoundException(OMSErrorCode.TICKET_NOT_FOUND);
        }

        return new GetTicketResponse(mapper.toTicketDto(ticket));
    }

    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_TICKET_SMS)
    public void onNewOrder(Integer orderId){
        onNewOrder(orderId, new DefaultRestClient(new RestConfig()));
    }

    @VisibleForTesting
    protected void onNewOrder(Integer orderId, RestClient rest){
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            LOGGER.error("Order#{} not found", orderId);
            return;
        }

        final List<Ticket> tickets = ticketRepository.findByOrder(order);
        for (Ticket ticket : tickets) {
            final String url = "http://localhost:" + port + "/v1/tickets/" + ticket.getId() + "/sms";
            try {
                rest.get(url, SendSmsResponse.class);
            } catch (HttpNotFoundException e) {
                LOGGER.error("Unable to send via SMS Ticket#{}", url, e);
            }
        }
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
        ticket.setOriginId(token.getOriginId());
        ticket.setDestinationId(token.getDestinationId());
        ticket.setMerchantId(line.getMerchantId());
        ticket.setProductId(token.getProductId());
        ticket.setDepartureDateTime(token.getDepartureDateTime());
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

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }
}

