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
import javax.persistence.Table;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name="T_ACCOUNT")
public class Account extends Persistent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name="site_id")
    private Integer siteId;

    @Column(name="reference_id")
    private Integer referenceId;

    private BigDecimal balance;

    @Column(name="currency_code")
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    private AccountType type;
}
