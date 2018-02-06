package io.tchepannou.enigma.oms.domain;

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

    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;

    private BigDecimal amount;

    private BigDecimal net;

    private BigDecimal fees;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name="entry_datetime")
    private Date entryDateTime;

    @Column(name="reference_id")
    private Integer referenceId;

    @Column(name="transaction_datetime")
    private Date transactionDateTime;
}
