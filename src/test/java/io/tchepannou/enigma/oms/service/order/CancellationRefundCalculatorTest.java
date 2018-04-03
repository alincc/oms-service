package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CancellationRefundCalculatorTest {
    @Mock
    private Clock clock;

    @InjectMocks
    private CancellationRefundCalculator calculator;

    @Test
    public void computeRefundAmount() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final BookingDto booking = createBooking(1, DateUtils.addDays(new Date(now), 3), 1000);

        // When
        calculator.setFreeCancellationHours(24);
        BigDecimal amount = calculator.computeRefundAmount(booking);

        // Then
        assertThat(amount).isEqualTo(booking.getTotalPrice());
    }


    @Test
    public void computeRefundAmountBelowThreshold() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final BookingDto booking = createBooking(1, DateUtils.addHours(new Date(now), 3), 1000);

        // When
        calculator.setFreeCancellationHours(24);
        BigDecimal amount = calculator.computeRefundAmount(booking);

        // Then
        assertThat(amount).isEqualTo(BigDecimal.ZERO);
    }


    @Test
    public void computeRefundAmountAtThreshold() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final BookingDto booking = createBooking(1, DateUtils.addHours(new Date(now), 24), 1000);

        // When
        calculator.setFreeCancellationHours(24);
        BigDecimal amount = calculator.computeRefundAmount(booking);

        // Then
        assertThat(amount).isEqualTo(booking.getTotalPrice());
    }

    private BookingDto createBooking(Integer id, Date departureDate, double amount){
        BookingDto booking = new BookingDto();
        booking.setId(id);
        booking.setDepartureDateTime(departureDate);
        booking.setTotalPrice(new BigDecimal(amount));
        return booking;
    }

}
