package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.AccountType;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.TransactionType;
import io.tchepannou.enigma.oms.repository.AccountRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.profile.client.dto.PlanDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FinanceService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MerchantBackend merchantBackend;

    @Transactional
    public void created(Integer orderId){
        final Order order = orderRepository.findOne(orderId);

        // Merchant
        final Set<Integer> merchantIds = order.getLines().stream()
                .map(l -> l.getMerchantId())
                .collect(Collectors.toSet());

        final Map<Integer, MerchantDto> merchants = merchantBackend.search(merchantIds).stream()
                .collect(Collectors.toMap(MerchantDto::getId, Function.identity()));

        // Site
        final Account siteAccount = findSiteAccount(order);

        for (final OrderLine line : order.getLines()){
            final PlanDto plan = merchants.get(line.getMerchantId()).getPlan();

            // Merchant
            final MerchantDto merchant = merchants.get(line.getMerchantId());
            final Account merchantAccount = findMerchantAccount(merchant, order);
            final Transaction merchantTx = toMerchantTransaction(order, line, merchantAccount, plan);
            updateAccount(merchantTx, merchantAccount);

            // Site
            updateAccount(
                    toSiteTransaction(merchantTx, order, line, siteAccount, plan),
                    siteAccount
            );

        }
    }

    private void updateAccount(Transaction tx, Account account){
        account.setBalance(account.getBalance().add(tx.getNet()));
        accountRepository.save(account);
    }

    private Account findMerchantAccount(final MerchantDto merchant, final Order order){
        Account account = accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, merchant.getId());
        if (account == null){
            account = new Account();
            account.setBalance(BigDecimal.ZERO);
            account.setCurrencyCode(order.getCurrencyCode());
            account.setReferenceId(merchant.getId());
            account.setSiteId(order.getSiteId());
            account.setType(AccountType.MERCHANT);
            accountRepository.save(account);
        }
        return account;
    }

    private Transaction toMerchantTransaction(final Order order, final OrderLine line, final Account account, final PlanDto plan) {
        final Transaction tx = new Transaction();
        final BigDecimal amount = line.getTotalPrice();
        final BigDecimal fees = amount.multiply(plan.getPercent()).add(plan.getAmount());
        final BigDecimal net = amount.subtract(fees);

        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setFees(fees);
        tx.setNet(net);
        tx.setEntryDateTime(DateHelper.now());
        tx.setReferenceId(line.getBookingId());
        tx.setTransactionDateTime(order.getOrderDateTime());
        tx.setType(TransactionType.BOOKING);

        transactionRepository.save(tx);
        return tx;
    }

    private Account findSiteAccount(final Order order){
        Account account = accountRepository.findByTypeAndReferenceId(AccountType.SITE, order.getId());
        if (account == null){
            account = new Account();
            account.setBalance(BigDecimal.ZERO);
            account.setCurrencyCode(order.getCurrencyCode());
            account.setReferenceId(order.getSiteId());
            account.setSiteId(order.getSiteId());
            account.setType(AccountType.SITE);
            accountRepository.save(account);
        }
        return account;
    }

    private Transaction toSiteTransaction(final Transaction merchantTx, final Order order, final OrderLine line, final Account account, final PlanDto plan) {
        final Transaction tx = new Transaction();
        final BigDecimal amount = merchantTx.getFees();
        final BigDecimal fees = BigDecimal.ZERO;
        final BigDecimal net = amount.subtract(fees);

        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setFees(fees);
        tx.setNet(net);
        tx.setEntryDateTime(DateHelper.now());
        tx.setReferenceId(line.getBookingId());
        tx.setTransactionDateTime(order.getOrderDateTime());
        tx.setType(TransactionType.BOOKING);

        transactionRepository.save(tx);
        return tx;
    }
}
