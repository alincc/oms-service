package io.tchepannou.enigma.oms.config;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.profile.client.MerchantBackend;
import io.tchepannou.enigma.profile.client.ProfileEnvironment;
import io.tchepannou.enigma.refdata.client.CityBackend;
import io.tchepannou.enigma.refdata.client.RefDataEnvironment;
import io.tchepannou.enigma.refdata.client.SiteBackend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ProfileConfiguration {

    @Autowired
    private RestClient rest;

    @Autowired
    private Environment env;

    @Bean
    public MerchantBackend merchantBackend(){
        return new MerchantBackend(rest, profileEnvironment());
    }

    @Bean
    public ProfileEnvironment profileEnvironment(){
        String name = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : null;
        return ProfileEnvironment.get(name);
    }
}
