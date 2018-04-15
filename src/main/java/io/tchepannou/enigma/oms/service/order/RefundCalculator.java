package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;

@Component
public class RefundCalculator {
    @Autowired
    private Clock clock;

    private int freeCancellationHours = 24;

    public BigDecimal computeRefundAmount (BookingDto booking) {
        final long hours = (booking.getDepartureDateTime().getTime() - clock.millis())/(60*60*1000);

        return hours < freeCancellationHours
                ? BigDecimal.ZERO
                : booking.getTotalPrice();
    }


    public int getFreeCancellationHours() {
        return freeCancellationHours;
    }

    public void setFreeCancellationHours(final int freeCancellationHours) {
        this.freeCancellationHours = freeCancellationHours;
    }
}
