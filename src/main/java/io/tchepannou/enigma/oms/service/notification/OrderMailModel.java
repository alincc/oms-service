package io.tchepannou.enigma.oms.service.notification;

import io.tchepannou.enigma.oms.client.dto.OrderDto;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderMailModel {
    private SiteDto site;
    private OrderDto order;
    private List<OrderLineData> lines;
    private String formattedTotalPrice;
}
