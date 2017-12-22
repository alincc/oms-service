package io.tchepannou.enigma.oms.service.ferari;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpConflictException;
import io.tchepannou.enigma.ferari.client.rr.ConfirmBookingResponse;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.exception.DownstreamException;
import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("enigma.service.ferari")
public class FerariService {
    @Autowired
    private FerariMapper bookingMapper;

    @Autowired
    private RestClient rest;

    private String url;

    public CreateBookingResponse book(final Order order){
        try {

            final CreateBookingRequest request = bookingMapper.toCreateBookingRequest(order);
            return rest.post(url + "/v1/bookings", request, CreateBookingResponse.class).getBody();

        } catch (HttpConflictException e){
            throw new DownstreamException(e, ErrorCode.OMS_CHECKOUT_FAILURE, e.getResponse().getBody());
        }
    }

    public void confirm (final  Order order){
        try {

            for (final OrderLine line : order.getLines()) {
                final String confirmUrl = String.format("%s/v1/bookings/%s/confirm", url, line.getBookingId());
                rest.get(confirmUrl, ConfirmBookingResponse.class).getBody();
            }

        } catch (HttpConflictException e){
            throw new DownstreamException(e, ErrorCode.OMS_CHECKOUT_FAILURE, e.getResponse().getBody());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
