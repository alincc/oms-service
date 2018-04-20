package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.apache.commons.lang.time.DateUtils;
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

    public Date computeFreeCancellationDateTime(Order order){
        try {
            final Date tomorrow = DateUtils.addHours(new Date(clock.millis()), freeCancellationHours);

            final Date departureDate = getDepartureDateTime(order);
            return departureDate != null && departureDate.after(tomorrow) ? tomorrow : null;
        } catch (Exception e){
            return null;
        }
    }


    public BigDecimal computeRefundAmount (Order order) {
        final Date free = order.getFreeCancellationDateTime();
        if (free == null){
            return BigDecimal.ZERO;
        } else {
            return free.getTime() > clock.millis()
                    ? BigDecimal.ZERO
                    : computeRefundableAmount(order);
        }
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

}
