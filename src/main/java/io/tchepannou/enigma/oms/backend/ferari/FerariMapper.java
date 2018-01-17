package io.tchepannou.enigma.oms.backend.ferari;

import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.oms.domain.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FerariMapper {
    public CreateBookingRequest toCreateBookingRequest(final Order order){
        final CreateBookingRequest request = new CreateBookingRequest();
        request.setOrderId(order.getId());
        request.setLastName(order.getLastName());
        request.setFirstName(order.getFirstName());
        request.setEmail(order.getEmail());
        request.setOfferTokens(
                order.getLines().stream()
                        .map(l -> l.getOfferToken())
                        .collect(Collectors.toList())
        );
        return request;
    }

}
