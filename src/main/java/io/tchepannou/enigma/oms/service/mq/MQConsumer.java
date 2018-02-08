package io.tchepannou.enigma.oms.service.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class MQConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestConfig configTemplate;

    protected RestClient createRestClient(){
        final RestConfig config = new RestConfig();
        config.setSerializer(configTemplate.getSerializer());
        config.setClientInfo(configTemplate.getClientInfo());
        return new DefaultRestClient(config);
    }

}
