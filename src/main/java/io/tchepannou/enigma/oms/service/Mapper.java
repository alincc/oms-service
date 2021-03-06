package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderLineType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.TicketToken;
import io.tchepannou.enigma.oms.client.dto.CustomerDto;
import io.tchepannou.enigma.oms.client.dto.ErrorDto;
import io.tchepannou.enigma.oms.client.dto.FeesDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.OrderDto;
import io.tchepannou.enigma.oms.client.dto.OrderLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.dto.TransactionDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.domain.Fees;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class Mapper {
    public Order toOrder(final CreateOrderRequest request){
        final Date now = DateHelper.now();
        final Order order = new Order();

        order.setCustomerId(request.getCustomerId());
        order.setOrderDateTime(now);
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setSubTotalAmount(BigDecimal.ZERO);
        order.setTotalFees(BigDecimal.ZERO);
        order.setSiteId(request.getSiteId());

        return order;
    }

    public OrderLine toOrderLine(
            final OfferLineDto dto,
            final Order order
    ) throws InvalidCarOfferTokenException {

        final TransportationOfferToken token = TransportationOfferToken.decode(dto.getToken());
        final BigDecimal unitPrice = token.getAmount();
        final Integer quantity = token.getTravellerCount();

        final OrderLine line = new OrderLine();
        line.setDescription(dto.getDescription());
        line.setOrder(order);
        line.setOfferToken(dto.getToken());
        line.setMerchantId(dto.getMerchantId());
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setTotalPrice(unitPrice.multiply(new BigDecimal(quantity)));
        line.setOfferToken(dto.getToken());
        line.setMerchantId(dto.getMerchantId());
        line.setType(dto.getType());

        order.setCurrencyCode(token.getCurrencyCode());

        return line;
    }

    public OrderLine toOrderLine(
            final Fees fees,
            final Order order
    ) throws InvalidCarOfferTokenException {

        final BigDecimal unitPrice = order.getSubTotalAmount()
                .multiply(fees.getPercent())
                .add(fees.getAmount());

        final OrderLine line = new OrderLine();
        line.setType(OrderLineType.FEES);
        line.setOrder(order);
        line.setQuantity(1);
        line.setUnitPrice(unitPrice);
        line.setTotalPrice(unitPrice);
        line.setFees(fees);

        return line;
    }

    public Traveller toTraveller(final TravellerDto dto, final Order order){
        final Traveller obj = new Traveller();
        obj.setFirstName(dto.getFirstName());
        obj.setLastName(dto.getLastName());
        obj.setSex(dto.getSex());
        obj.setEmail(dto.getEmail());
        obj.setOrder(order);
        return obj;
    }

    public OrderDto toDto(final Order obj){
        final OrderDto dto = new OrderDto();
        dto.setCurrencyCode(obj.getCurrencyCode());
        dto.setId(obj.getId());
        dto.setOrderDateTime(obj.getOrderDateTime());
        dto.setStatus(obj.getStatus());
        dto.setTotalAmount(obj.getTotalAmount());
        dto.setCustomer(toCustomerDto(obj));
        dto.setSiteId(obj.getSiteId());
        dto.setDeviceUID(obj.getDeviceUID());
        dto.setLanguageCode(obj.getLanguageCode());
        dto.setMobileNumber(obj.getMobileNumber());
        dto.setCreationDateTime(obj.getCreationDateTime());
        dto.setCheckoutDateTime(obj.getCheckoutDateTime());
        dto.setCancellationDateTime(obj.getCancellationDateTime());
        dto.setFreeCancellationDateTime(obj.getFreeCancellationDateTime());
        dto.setSubTotalAmount(obj.getSubTotalAmount());
        dto.setTotalFees(obj.getTotalFees());
        dto.setExpiryDateTime(obj.getExpiryDateTime());

        if (obj.getTravellers() != null) {
            dto.setTravellers(
                    obj.getTravellers().stream()
                            .map(t -> toDto(t))
                            .collect(Collectors.toList())
            );
        }
        if (obj.getLines() != null) {
            dto.setLines(
                    obj.getLines().stream()
                            .map(l -> toDto(l))
                            .collect(Collectors.toList())
            );
        }
        return dto;
    }

    private CustomerDto toCustomerDto(final Order obj){
        final CustomerDto dto = new CustomerDto();
        dto.setId(obj.getCustomerId());
        dto.setFirstName(obj.getFirstName());
        dto.setLastName(obj.getLastName());
        dto.setEmail(obj.getEmail());

        return dto;
    }

    public OrderLineDto toDto(final OrderLine obj){
        final OrderLineDto dto = new OrderLineDto();
        dto.setQuantity(obj.getQuantity());
        dto.setUnitPrice(obj.getUnitPrice());
        dto.setTotalPrice(obj.getTotalPrice());
        dto.setBookingId(obj.getBookingId());
        dto.setDescription(obj.getDescription());
        dto.setId(obj.getId());
        dto.setType(obj.getType());
        dto.setOfferToken(obj.getOfferToken());
        dto.setMerchantId(obj.getMerchantId());
        dto.setFees(toFeesDto(obj.getFees()));
        return dto;
    }

    public TicketDto toTicketDto(final Ticket obj){
        final TicketToken token = new TicketToken();
        token.setId(obj.getId());
        token.setLastName(obj.getLastName());
        token.setFirstName(obj.getFirstName());
        token.setOfferToken(obj.getOrderLine().getOfferToken());
        token.setMerchantId(obj.getOrderLine().getMerchantId());
        token.setSequenceNumber(obj.getSequenceNumber());
        token.setOrderLineId(obj.getOrderLine().getId());
        token.setOrderId(obj.getOrderLine().getOrder().getId());

        final TicketDto dto = new TicketDto();
        dto.setId(obj.getId());
        dto.setToken(token.toString());
        dto.setExpiryDateTime(obj.getExpiryDateTime());
        dto.setDepartureDateTime(obj.getDepartureDateTime());
        dto.setDestinationId(obj.getDestinationId());
        dto.setExpiryDateTime(obj.getExpiryDateTime());
        dto.setMerchantId(obj.getMerchantId());
        dto.setOriginId(obj.getOriginId());
        dto.setPrintDateTime(obj.getPrintDateTime());
        dto.setProductId(obj.getProductId());
        dto.setFirstName(obj.getFirstName());
        dto.setLastName(obj.getLastName());
        dto.setOrderId(obj.getOrderLine().getOrder().getId());
        dto.setOrderLineId(obj.getOrderLine().getId());
        dto.setSequenceNumber(obj.getSequenceNumber());
        dto.setStatus(obj.getStatus());
        dto.setCancellationDateTime(obj.getCancellationDateTime());
        return dto;
    }

    public TravellerDto toDto(final Traveller obj){
        final TravellerDto dto = new TravellerDto();
        dto.setEmail(obj.getEmail());
        dto.setFirstName(obj.getFirstName());
        dto.setLastName(obj.getLastName());
        dto.setSex(obj.getSex());
        dto.setEmail(obj.getEmail());
        return dto;
    }

    public ErrorDto toDto(final OMSErrorCode error){
        final ErrorDto dto = new ErrorDto();
        dto.setCode(error.getCode());
        dto.setText(error.getText());
        return dto;
    }

    public TransactionDto toDto(final Transaction tx){
        if (tx == null){
            return null;
        }

        final TransactionDto dto = new TransactionDto();
        dto.setAmount(tx.getAmount());
        dto.setCurrencyCode(tx.getCurrencyCode());
        dto.setGatewayTid(tx.getGatewayTid());
        dto.setId(tx.getId());
        dto.setOrderId(tx.getOrder().getId());
        dto.setTransactionDateTime(tx.getTransactionDateTime());
        dto.setType(tx.getType());
        return dto;
    }

    public FeesDto toFeesDto(final Fees obj){
        if (obj == null){
            return null;
        }

        FeesDto dto = new FeesDto();
        dto.setAmount(obj.getAmount());
        dto.setId(obj.getId());
        dto.setName(obj.getName());
        dto.setPercent(obj.getPercent());
        dto.setSiteId(obj.getSiteId());
        dto.setRefundable(obj.isRefundable());
        return dto;
    }
}
