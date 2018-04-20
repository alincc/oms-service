package io.tchepannou.enigma.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.features.LanguageFeature;
import io.tchepannou.core.rest.features.TracingFeature;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.core.rest.impl.JsonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class RestConfiguration {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpServletRequest request;

    @Bean
    public RestConfig restConfig(){
        final RestConfig config = new RestConfig();
        config.setSerializer(new JsonSerializer(objectMapper));
        config.addFeatuure(new TracingFeature("oms-service", request));
        config.addFeatuure(new LanguageFeature(request));
        return config;
    }

    @Bean
    public RestClient restClient (RestConfig restConfig) {
        return new DefaultRestClient(restConfig);
    }
}
