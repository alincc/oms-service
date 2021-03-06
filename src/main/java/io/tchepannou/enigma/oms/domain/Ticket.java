package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.TicketStatus;
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
    @JoinColumn(name="order_line_fk")
    private OrderLine orderLine;

    @Column(name="sequence_number")
    private Integer sequenceNumber;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    @Column(name="departure_datetime")
    private Date departureDateTime;

    @Column(name="origin_id")
    private Integer originId;

    @Column(name="destination_id")
    private Integer destinationId;

    @Column(name="merchant_id")
    private Integer merchantId;

    @Column(name="product_id")
    private Integer productId;

    @Column(name="print_datetime")
    private Date printDateTime;

    @Column(name="expiry_datetime")
    private Date expiryDateTime;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Column(name="cancellation_datetime")
    private Date cancellationDateTime;
}
