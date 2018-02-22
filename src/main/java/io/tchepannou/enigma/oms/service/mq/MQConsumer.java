package io.tchepannou.enigma.oms.service.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.core.rest.impl.JsonSerializer;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class MQConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    protected RestClient createRestClient(){
        final RestConfig config = new RestConfig();
        config.setSerializer(new JsonSerializer(objectMapper));

        return new DefaultRestClient(config);
    }

}
