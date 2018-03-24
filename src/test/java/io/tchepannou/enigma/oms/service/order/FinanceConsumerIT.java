package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.AccountType;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.TransactionType;
import io.tchepannou.enigma.oms.repository.AccountRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Sql({"classpath:/sql/clean.sql", "classpath:/sql/FinanceConsumer.sql"})
@ActiveProfiles(profiles = {"stub"})
public class FinanceConsumerIT extends NotificationConsumerTestBase {
    @Autowired
    private FinanceConsumer consumer;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void shouldRecordTransactionsOnCheckout() throws Exception {
        // Given
        consumer.consume(100);

        // Then
        final Order order = orderRepository.findOne(100);

        final Account account1001 = accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, 1001);
        assertThat(account1001.getBalance()).isEqualTo(new BigDecimal(5300.00).setScale(2));
        assertThat(account1001.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account1001.getReferenceId()).isEqualTo(1001);
        assertThat(account1001.getType()).isEqualTo(AccountType.MERCHANT);
        assertThat(account1001.getSiteId()).isEqualTo(1);

        final List<Transaction> tx1101 = transactionRepository.findByAccount(account1001);
        assertThat(tx1101).hasSize(1);
        assertThat(tx1101.get(0).getAmount()).isEqualTo(new BigDecimal(6000.00).setScale(2));
        assertThat(tx1101.get(0).getFees()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx1101.get(0).getNet()).isEqualTo(new BigDecimal(5300.00).setScale(2));
        assertThat(tx1101.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1101.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1101.get(0).getReferenceId()).isEqualTo(1000);


        final Account account1002 = accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, 1002);
        assertThat(account1002.getBalance()).isEqualTo(new BigDecimal((10000.00 + 10700.00)).setScale(2));
        assertThat(account1002.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account1002.getReferenceId()).isEqualTo(1002);
        assertThat(account1002.getType()).isEqualTo(AccountType.MERCHANT);
        assertThat(account1002.getSiteId()).isEqualTo(1);

        final List<Transaction> tx1102 = transactionRepository.findByAccount(account1002);
        assertThat(tx1102).hasSize(1);
        assertThat(tx1102.get(0).getAmount()).isEqualTo(new BigDecimal(12000.00).setScale(2));
        assertThat(tx1102.get(0).getFees()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx1102.get(0).getNet()).isEqualTo(new BigDecimal(10700.00).setScale(2));
        assertThat(tx1102.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1102.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1102.get(0).getReferenceId()).isEqualTo(1001);

        final Account account1 = accountRepository.findByTypeAndReferenceId(AccountType.SITE, 1);
        assertThat(account1.getBalance()).isEqualTo(new BigDecimal((2000)).setScale(2));
        assertThat(account1.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account1.getReferenceId()).isEqualTo(1);
        assertThat(account1.getType()).isEqualTo(AccountType.SITE);
        assertThat(account1.getSiteId()).isEqualTo(1);

        final List<Transaction> tx1 = transactionRepository.findByAccount(account1);
        assertThat(tx1).hasSize(2);

        assertThat(tx1.get(0).getAmount()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx1.get(0).getFees()).isEqualTo(new BigDecimal(0.00).setScale(2));
        assertThat(tx1.get(0).getNet()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx1.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1.get(0).getReferenceId()).isEqualTo(1000);

        assertThat(tx1.get(1).getAmount()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx1.get(1).getFees()).isEqualTo(new BigDecimal(0.00).setScale(2));
        assertThat(tx1.get(1).getNet()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx1.get(1).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1.get(1).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1.get(1).getReferenceId()).isEqualTo(1001);
    }
}
