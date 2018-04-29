package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.OrderStatus;
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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;
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

    @Column(name="site_id")
    private Integer siteId;

    @Column(name="device_uid")
    private String deviceUID;

    @Column(name="customer_id")
    private Integer customerId;

    @Column(name="order_datetime")
    private Date orderDateTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name="sub_total_amount")
    private BigDecimal subTotalAmount;

    @Column(name="total_fees")
    private BigDecimal totalFees;

    @Column(name="total_amount")
    private BigDecimal totalAmount;

    @Column(name="currency_code")
    private String currencyCode;

    @OneToMany(mappedBy = "order")
    @OrderBy("id")
    private List<OrderLine> lines;

    @OneToMany(mappedBy = "order")
    private List<Traveller> travellers;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    private String email;

    @Column(name="mobile_number")
    private String mobileNumber;

    @Column(name="mobile_provider")
    private String mobileProvider;

    @Column(name="language_code")
    private String languageCode;

    @Column(name="creation_datetime")
    private Date creationDateTime;

    @Column(name="checkout_datetime")
    private Date checkoutDateTime;

    @Column(name="cancellation_datetime")
    private Date cancellationDateTime;

    @Column(name="free_cancellation_datetime")
    private Date freeCancellationDateTime;

    @Column(name="expiry_datetime")
    private Date expiryDateTime;

    @Version
    private Long version;
}
