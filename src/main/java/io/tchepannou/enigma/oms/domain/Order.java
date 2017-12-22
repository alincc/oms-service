package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="T_ORDER")
public class Order extends Persistent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name="merchant_id")
    private Integer merchantId;

    @Column(name="payment_id")
    private Integer paymentId;

    @Column(name="customer_id")
    private Integer customerId;

    @Column(name="order_datetime")
    private Date orderDateTime;

    @Column(name="expiry_datetime")
    private Date expiryDateTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name="total_amount")
    private BigDecimal totalAmount;

    @Column(name="currency_code")
    private String currencyCode;

    @OneToMany(mappedBy = "order")
    private List<OrderLine> lines;

    @OneToMany(mappedBy = "order")
    private List<Traveller> travellers;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="countryCode", column = @Column(name="mobile_payment_country_code")),
        @AttributeOverride(name="areaCode", column = @Column(name="mobile_payment_area_code")),
        @AttributeOverride(name="number", column = @Column(name="mobile_payment_number")),
        @AttributeOverride(name="provider", column = @Column(name="mobile_payment_provider"))
    })
    private MobilePayment mobilePayment;
}
