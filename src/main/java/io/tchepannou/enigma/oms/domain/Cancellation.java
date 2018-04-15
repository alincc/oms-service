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
@Table(name="T_CANCELLATION")
public class Cancellation extends Persistent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="order_fk")
    private Order order;

    @Column(name="booking_id")
    private Integer bookingId;

    @Column(name="cancellation_datetime")
    private Date cancellationDateTime;
}
