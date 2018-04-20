package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.TransactionType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name="T_TRANSACTION")
public class Transaction extends Persistent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name="gateway_tid")
    private String gatewayTid;

    private BigDecimal amount;

    @Column(name="currency_code")
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name="transaction_datetime")
    private Date transactionDateTime;

    @ManyToOne
    @JoinColumn(name="order_fk")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name="payment_method")
    private PaymentMethod paymentMethod;

}
