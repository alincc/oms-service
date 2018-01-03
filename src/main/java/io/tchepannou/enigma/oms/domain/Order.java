package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
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

    @Enumerated(EnumType.STRING)
    @Column(name="payment_method")
    private PaymentMethod paymentMethod;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    private String email;
}
