package io.tchepannou.enigma.oms.service.tontine;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpConflictException;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.tontine.client.rr.ChargeRequest;
import io.tchepannou.enigma.tontine.client.rr.ChargeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("enigma.service.tontine")
public class TontineService {
    private String url;

    @Autowired
    private TontineMapper tontineMapper;

    @Autowired
    private RestClient rest;



    public ChargeResponse charge(final Order order, CheckoutOrderRequest checkoutRequest){
        try {

            final ChargeRequest request = tontineMapper.toChargeRequest(order, checkoutRequest);
            return rest.post(url + "/v1/transactions/charge", request, ChargeResponse.class).getBody();

        } catch (HttpConflictException e){

            throw new TontineException("Unable to charge the order", e);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

}
