package io.tchepannou.enigma.oms.backend.finance;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.oms.client.dto.OrderDto;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("enigma.service.finance.order")
public class FinanceOrderBackend {
    @Autowired
    private RestClient rest;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Mapper mapper;

    private String url;

    public void created(final Integer orderId){
        final Order order = orderRepository.findOne(orderId);
        final OrderDto dto = mapper.toDto(order);
        rest.post(url + "/created", dto, Object.class);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
