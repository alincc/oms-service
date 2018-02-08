package io.tchepannou.enigma.oms.service.mq.notification;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderMailModel {
    private String siteLogoUrl;
    private String siteBrandName;
    private Integer orderId;
    private String customerName;
    private List<OrderLineModel> lines;
    private String formattedOrderDateTime;
    private String formattedTotalPrice;
}
