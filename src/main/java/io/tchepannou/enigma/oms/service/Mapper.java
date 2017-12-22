package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.ferari.client.CarOfferToken;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.dto.ErrorDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.domain.MobilePayment;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import io.tchepannou.enigma.tontine.client.dto.MobilePaymentInfoDto;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
public class Mapper {
    public Order toOrder(final CreateOrderRequest request, final int orderTTLMinutes){
        final Date now = DateHelper.now();
        final Order order = new Order();

        order.setCustomerId(request.getCustomer().getId());
        order.setOrderDateTime(now);
        order.setExpiryDateTime(DateUtils.addHours(now, orderTTLMinutes));
        order.setMerchantId(request.getMerchantId());
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(BigDecimal.ZERO);

        MobilePaymentInfoDto mobileDto = request.getPaymentInfo().getMobile();
        order.setMobilePayment(mobileDto == null ? null : toMobilePayment(mobileDto));

        return order;
    }
    private MobilePayment toMobilePayment(MobilePaymentInfoDto dto){
        MobilePayment obj = new MobilePayment();
        obj.setAreaCode(dto.getAreaCode());
        obj.setCountryCode(dto.getCountryCode());
        obj.setNumber(dto.getNumber());
        obj.setProvider(dto.getProvider());
        return obj;
    }

    public OrderLine toOrderLine(final OfferLineDto dto, final Order order) throws InvalidCarOfferTokenException{
        final OrderLine line = new OrderLine();
        line.setOfferType(dto.getType());
        line.setOfferToken(dto.getToken());
        line.setDescription(dto.getDescription());
        line.setOrder(order);

        if (OfferType.CAR.equals(line.getOfferType())){
            final CarOfferToken token = CarOfferToken.decode(line.getOfferToken());
            final BigDecimal amount = token.getAmount();

            line.setAmount(amount);
            order.setTotalAmount(order.getTotalAmount().add(amount));
            order.setCurrencyCode(token.getCurrencyCode());
        }
        return line;
    }

    public Traveller toTraveller(final TravellerDto dto, final Order order){
        final Traveller obj = new Traveller();
        obj.setFirstName(dto.getFirstName());
        obj.setLastName(dto.getLastName());
        obj.setSex(dto.getSex());
        obj.setOrder(order);
        return obj;
    }

    public ErrorDto toDto(final ErrorCode error){
        final ErrorDto dto = new ErrorDto();
        dto.setCode(error.getCode());
        dto.setText(error.getText());
        return dto;
    }

}
