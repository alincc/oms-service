package io.tchepannou.enigma.oms.config;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.BookingBackend;
import io.tchepannou.enigma.ferari.client.FerariEnvironment;
import io.tchepannou.enigma.ferari.client.OfferBackend;
import io.tchepannou.enigma.ferari.client.ProductBackend;
import io.tchepannou.enigma.profile.client.MerchantBackend;
import io.tchepannou.enigma.profile.client.ProfileEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FerariConfiguration {

    @Autowired
    private RestClient rest;

    @Autowired
    private Environment env;

    @Bean
    public ProductBackend productBackend(){
        return new ProductBackend(rest, ferariEnvironment());
    }

    @Bean
    public OfferBackend offerBackend(){
        return new OfferBackend(rest, ferariEnvironment());
    }

    @Bean
    public BookingBackend bookingBackend(){
        return new BookingBackend(rest, ferariEnvironment());
    }

    @Bean
    public FerariEnvironment ferariEnvironment(){
        String name = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : null;
        return FerariEnvironment.get(name);
    }
}
