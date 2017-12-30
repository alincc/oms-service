package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.OfferType;
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

@Getter
@Setter
@Entity
@Table(name="T_ORDER_LINE")
public class OrderLine extends Persistent{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="order_fk")
    private Order order;

    @Column(name="booking_id")
    private Integer bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name="offer_type")
    private OfferType offerType;

    private Integer quantity;

    @Column(name="unit_price")
    private BigDecimal unitPrice;

    @Column(name="total_price")
    private BigDecimal totalPrice;

    @Column(name="offer_token")
    private String offerToken;

    private String description;
}
