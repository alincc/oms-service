package io.tchepannou.enigma.oms.domain;

import io.tchepannou.enigma.oms.client.Sex;
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

    @Enumerated(EnumType.STRING)
    private Sex sex;

    private String hash;

    @Column(name="print_datetime")
    private Date printDateTime;

    @Column(name="expiry_datetime")
    private Date expiryDateTime;
}
