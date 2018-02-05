package io.tchepannou.enigma.oms.service.mail;

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
    private List<OrderLineData> lines;
    private String formattedOrderDateTime;
    private String formattedTotalPrice;
}
