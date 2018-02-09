package io.tchepannou.enigma.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.CurrentRequestProvider;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.core.rest.impl.JsonSerializer;
import io.tchepannou.enigma.oms.service.CurrentRequestProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestConfiguration {
    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public RestConfig restConfig(){
        final RestConfig config = new RestConfig();
        config.setSerializer(new JsonSerializer(objectMapper));
        config.setCurrentRequestProvider(currentRequestProvider());
        config.setClientInfo("oms-service");
        return config;
    }

    @Bean
    public RestClient restClient (RestConfig restConfig) {
        return new DefaultRestClient(restConfig);
    }

    @Bean
    public CurrentRequestProvider currentRequestProvider(){
        return new CurrentRequestProviderImpl();
    }
}
