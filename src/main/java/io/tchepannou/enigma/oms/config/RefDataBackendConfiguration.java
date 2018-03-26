package io.tchepannou.enigma.oms.config;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.refdata.client.CityBackend;
import io.tchepannou.enigma.refdata.client.RefDataEnvironment;
import io.tchepannou.enigma.refdata.client.SiteBackend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RefDataBackendConfiguration {

    @Autowired
    private RestClient rest;

    @Autowired
    private Environment env;

    @Bean
    public SiteBackend siteBackend(){
        return new SiteBackend(rest, refDataEnvironment());
    }

    @Bean
    public CityBackend cityBackend() {
        return new CityBackend(rest, refDataEnvironment());
    }

    @Bean
    public RefDataEnvironment refDataEnvironment(){
        String name = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : null;
        return RefDataEnvironment.get(name);
    }
}
