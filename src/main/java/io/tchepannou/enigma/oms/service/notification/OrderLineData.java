package io.tchepannou.enigma.oms.service.notification;

import io.tchepannou.enigma.oms.client.dto.OrderLineDto;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.dto.CityDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderLineData extends OrderLineDto {
    private CityDto origin;
    private CityDto destination;
    private MerchantDto merchant;
    private String formattedDepartureDate;
    private String formattedDepartureTime;
    private String formattedArrivalTime;
    private String formattedUnitPrice;
}
