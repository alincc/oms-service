package io.tchepannou.enigma.oms.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class MobilePayment {
    private String countryCode;
    private String areaCode;
    private String number;
    private String provider;

}
