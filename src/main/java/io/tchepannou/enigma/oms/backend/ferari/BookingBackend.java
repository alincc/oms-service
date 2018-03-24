package io.tchepannou.enigma.oms.backend.ferari;

import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.enigma.ferari.client.CancellationReason;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingResponse;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("enigma.service.ferari.booking")
public class BookingBackend {
    @Autowired
    private FerariMapper bookingMapper;

    @Autowired
    private RestClient rest;

    private String url;

    @Loggable("Ferari.Booking.book")
    public List<BookingDto> book(final Order order){
        try {

            final CreateBookingRequest request = bookingMapper.toCreateBookingRequest(order);
            return rest.post(url, request, CreateBookingResponse.class).getBody().getBookings();

        } catch (HttpException e){
            throw new FerrariException("Unable to book Order#" + order.getId(), e);
        }
    }

    @Loggable("Ferari.Booking.confirm")
    public void confirm (final Order order){
        try {

            for (final OrderLine line : order.getLines()) {
                final String confirmUrl = String.format("%s/%s/confirm", url, line.getBookingId());
                rest.get(confirmUrl, Object.class);
            }

        } catch (HttpException e){
            throw new FerrariException("Unable to book Order#" + order.getId(), e);
        }
    }

    @Loggable("Ferari.Booking.expire")
    public void expire (final Order order, final RestClient rest) {
        try {

            final CancelBookingRequest request = new CancelBookingRequest();
            request.setReason(CancellationReason.EXPIRED);

            for (final OrderLine line : order.getLines()) {
                final String cancelUrl = String.format("%s/%s/cancel", url, line.getBookingId());
                rest.post(cancelUrl, request, CancelBookingResponse.class);
            }

        } catch (HttpException e){
            throw new FerrariException("Unable to cancel Order#" + order.getId(), e);
        }

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
