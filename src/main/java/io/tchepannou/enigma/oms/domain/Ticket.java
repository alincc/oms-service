package io.tchepannou.enigma.oms.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name="T_TICKET")
public class Ticket extends Persistent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="order_fk")
    private Order order;

    @Column(name="merchant_id")
    private Integer merchantId;

    @Column(name="booking_id")
    private Integer bookingId;

    @Column(name="offer_token")
    private String offerToken;

    private String hash;

    @Column(name="print_datetime")
    private Date printDateTime;

    @Column(name="expiry_datetime")
    private Date expiryDateTime;
}
