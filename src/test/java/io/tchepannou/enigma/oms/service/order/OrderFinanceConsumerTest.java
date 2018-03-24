package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.AccountType;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.TransactionType;
import io.tchepannou.enigma.oms.repository.AccountRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.profile.client.dto.PlanDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("CPD-START")
public class OrderFinanceConsumerTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MerchantBackend merchantBackend;

    @InjectMocks
    private OrderFinanceConsumer service;

    @Test
    public void onOrderConfirmedOneWay() {
        // Given
        OrderLine line = createOrderLine(1, 111, 11, 1000d);
        Order order = createOrder(1, OrderStatus.CONFIRMED, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        MerchantDto merchant = createMerchant(11, 50, .1);
        when(merchantBackend.search(anyCollection(), any())).thenReturn(Arrays.asList(merchant));

        Account siteAccount = createAccount(21,  order.getSiteId(), AccountType.SITE, 100);
        when(accountRepository.findByTypeAndReferenceId(AccountType.SITE, order.getSiteId())).thenReturn(siteAccount);

        Account merchantAccount = createAccount(22,  order.getSiteId(), AccountType.MERCHANT, 1000);
        when(accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, merchant.getId())).thenReturn(merchantAccount);

        // When
        service.onOrderConfirmed(1);

        // Then
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(tx.capture());

        assertThat(tx.getAllValues().get(0).getAmount().doubleValue()).isEqualTo(1000d);
        assertThat(tx.getAllValues().get(0).getFees().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(0).getNet().doubleValue()).isEqualTo(850d);
        assertThat(tx.getAllValues().get(0).getReferenceId()).isEqualTo(line.getBookingId());
        assertThat(tx.getAllValues().get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(0).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(0).getAccount()).isEqualTo(merchantAccount);

        assertThat(tx.getAllValues().get(1).getAmount().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(1).getFees().doubleValue()).isEqualTo(0d);
        assertThat(tx.getAllValues().get(1).getNet().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(1).getReferenceId()).isEqualTo(line.getBookingId());
        assertThat(tx.getAllValues().get(1).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(1).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(1).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(1).getAccount()).isEqualTo(siteAccount);

        ArgumentCaptor<Account> act = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(act.capture());
        assertThat(act.getAllValues().get(0).getId()).isEqualTo(merchantAccount.getId());
        assertThat(act.getAllValues().get(0).getBalance().doubleValue()).isEqualTo(1850d);

        assertThat(act.getAllValues().get(1).getId()).isEqualTo(siteAccount.getId());
        assertThat(act.getAllValues().get(1).getBalance().doubleValue()).isEqualTo(250d);
    }
    
    @Test
    public void onOrderConfirmedReturn() {
        // Given
        OrderLine line1 = createOrderLine(1, 111, 11, 1000d);
        OrderLine line2 = createOrderLine(2, 112, 11, 800d);
        Order order = createOrder(1, OrderStatus.CONFIRMED, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        MerchantDto merchant = createMerchant(11, 50, .1);
        when(merchantBackend.search(anyCollection(), any())).thenReturn(Arrays.asList(merchant));

        Account siteAccount = createAccount(21,  order.getSiteId(), AccountType.SITE, 100);
        when(accountRepository.findByTypeAndReferenceId(AccountType.SITE, order.getSiteId())).thenReturn(siteAccount);

        Account merchantAccount = createAccount(22,  order.getSiteId(), AccountType.MERCHANT, 1000);
        when(accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, merchant.getId())).thenReturn(merchantAccount);

        // When
        service.onOrderConfirmed(1);

        // Then
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(4)).save(tx.capture());

        assertThat(tx.getAllValues().get(0).getAmount().doubleValue()).isEqualTo(1000d);
        assertThat(tx.getAllValues().get(0).getFees().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(0).getNet().doubleValue()).isEqualTo(850d);
        assertThat(tx.getAllValues().get(0).getReferenceId()).isEqualTo(line1.getBookingId());
        assertThat(tx.getAllValues().get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(0).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(0).getAccount()).isEqualTo(merchantAccount);

        assertThat(tx.getAllValues().get(1).getAmount().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(1).getFees().doubleValue()).isEqualTo(0d);
        assertThat(tx.getAllValues().get(1).getNet().doubleValue()).isEqualTo(150d);
        assertThat(tx.getAllValues().get(1).getReferenceId()).isEqualTo(line1.getBookingId());
        assertThat(tx.getAllValues().get(1).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(1).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(1).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(1).getAccount()).isEqualTo(siteAccount);

        assertThat(tx.getAllValues().get(2).getAmount().doubleValue()).isEqualTo(800d);
        assertThat(tx.getAllValues().get(2).getFees().doubleValue()).isEqualTo(130d);
        assertThat(tx.getAllValues().get(2).getNet().doubleValue()).isEqualTo(670d);
        assertThat(tx.getAllValues().get(2).getReferenceId()).isEqualTo(line2.getBookingId());
        assertThat(tx.getAllValues().get(2).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(2).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(2).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(2).getAccount()).isEqualTo(merchantAccount);

        assertThat(tx.getAllValues().get(3).getAmount().doubleValue()).isEqualTo(130d);
        assertThat(tx.getAllValues().get(3).getFees().doubleValue()).isEqualTo(0d);
        assertThat(tx.getAllValues().get(3).getNet().doubleValue()).isEqualTo(130d);
        assertThat(tx.getAllValues().get(3).getReferenceId()).isEqualTo(line2.getBookingId());
        assertThat(tx.getAllValues().get(3).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx.getAllValues().get(3).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx.getAllValues().get(3).getEntryDateTime()).isNotNull();
        assertThat(tx.getAllValues().get(3).getAccount()).isEqualTo(siteAccount);


        ArgumentCaptor<Account> act = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(4)).save(act.capture());
        assertThat(merchantAccount.getBalance().doubleValue()).isEqualTo(2520d);
        assertThat(siteAccount.getBalance().doubleValue()).isEqualTo(380d);
    }

    private Order createOrder(Integer id, OrderStatus status, OrderLine...lines){
        final Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setSiteId(1);
        order.setOrderDateTime(new Date());
        if (lines != null) {
            order.setLines(Arrays.asList(lines));
            order.getLines().forEach(line -> line.setOrder(order));
        }
        return order;
    }

    private OrderLine createOrderLine(Integer id, Integer bookingId, Integer merchantId, double unitPrice){
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setMerchantId(merchantId);
        line.setBookingId(bookingId);
        line.setQuantity(1);
        line.setMerchantId(merchantId);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setTotalPrice(new BigDecimal(unitPrice));
        return line;
    }

    private MerchantDto createMerchant(Integer id, double amount, double percent){
        PlanDto plan = new PlanDto();
        plan.setCurrencyCode("XAF");
        plan.setPercent(new BigDecimal(String.valueOf(percent)));
        plan.setAmount(new BigDecimal(amount));

        MerchantDto merchant = new MerchantDto();
        merchant.setId(id);
        merchant.setPlan(plan);
        return merchant;
    }

    private Account createAccount(Integer id, Integer referenceId, AccountType type, double balance){
        Account account = new Account();
        account.setId(id);
        account.setType(type);
        account.setReferenceId(referenceId);
        account.setSiteId(1);
        account.setBalance(new BigDecimal(balance));
        account.setCurrencyCode("XAF");
        return account;
    }
}
