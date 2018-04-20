package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Date;

@Component
@ConfigurationProperties("enigma.service.refund")
public class RefundService {
    @Autowired
    private Clock clock;

    private int freeCancellationHours = 24;


    public BigDecimal computeRefundAmount (Order order) {
        final long hours = (getDepartureDateTime(order).getTime() - clock.millis())/(60*60*1000);

        return hours < freeCancellationHours
                ? BigDecimal.ZERO
                : computeRefundableAmount(order);
    }

    private Date getDepartureDateTime(Order order) {
        Date departureDateTime = null;
        for (OrderLine line : order.getLines()){
            TransportationOfferToken token = TransportationOfferToken.decode(line.getOfferToken());
            if (departureDateTime == null || token.getDepartureDateTime().before(departureDateTime)){
                departureDateTime = token.getDepartureDateTime();
            }
        }
        return departureDateTime;
    }

    private BigDecimal computeRefundableAmount(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : order.getLines()){
            total = total.add(line.getTotalPrice());
        }
        return total;
    }

    public int getFreeCancellationHours() {
        return freeCancellationHours;
    }

    public void setFreeCancellationHours(final int freeCancellationHours) {
        this.freeCancellationHours = freeCancellationHours;
    }
}
