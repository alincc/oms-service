package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.ferari.client.CarOfferToken;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.dto.ErrorDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.OrderDto;
import io.tchepannou.enigma.oms.client.dto.OrderLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class Mapper {
    public Order toOrder(final CreateOrderRequest request, final int orderTTLMinutes){
        final Date now = DateHelper.now();
        final Order order = new Order();

        order.setCustomerId(request.getCustomerId());
        order.setOrderDateTime(now);
        order.setExpiryDateTime(DateUtils.addHours(now, orderTTLMinutes));
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(BigDecimal.ZERO);

        return order;
    }

    public OrderLine toOrderLine(
            final OfferLineDto dto,
            final Order order
    ) throws InvalidCarOfferTokenException{
        final OrderLine line = new OrderLine();
        line.setOfferType(dto.getType());
        line.setDescription(dto.getDescription());
        line.setOrder(order);
        line.setOfferToken(dto.getToken());

        if (OfferType.CAR.equals(line.getOfferType())){
            final CarOfferToken token = CarOfferToken.decode(dto.getToken());

            final BigDecimal unitPrice = token.getAmount();
            final BigDecimal quantity = new BigDecimal(token.getTravellerCount());

            line.setQuantity(quantity.intValue());
            line.setUnitPrice(unitPrice);
            line.setTotalPrice(unitPrice.multiply(quantity));
            line.setOfferToken(dto.getToken());

            order.setTotalAmount(order.getTotalAmount().add(line.getTotalPrice()));
            order.setCurrencyCode(token.getCurrencyCode());
        }
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
        dto.setCustomerId(obj.getCustomerId());
        dto.setExpiryDateTime(obj.getExpiryDateTime());
        dto.setId(obj.getId());
        dto.setOrderDateTime(obj.getOrderDateTime());
        dto.setPaymentId(obj.getPaymentId());
        dto.setPaymentMethod(obj.getPaymentMethod());
        dto.setStatus(obj.getStatus());
        dto.setTotalAmount(obj.getTotalAmount());

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

    public OrderLineDto toDto(final OrderLine obj){
        final OrderLineDto dto = new OrderLineDto();
        dto.setQuantity(obj.getQuantity());
        dto.setUnitPrice(obj.getUnitPrice());
        dto.setTotalPrice(obj.getTotalPrice());
        dto.setBookingId(obj.getBookingId());
        dto.setDescription(obj.getDescription());
        dto.setId(obj.getId());
        dto.setOfferType(obj.getOfferType());
        dto.setOfferToken(obj.getOfferToken());
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
    public ErrorDto toDto(final ErrorCode error){
        final ErrorDto dto = new ErrorDto();
        dto.setCode(error.getCode());
        dto.setText(error.getText());
        return dto;
    }

}
