package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
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

        for (final OrderLine line : order.getLines()){
            final PlanDto plan = merchants.get(line.getMerchantId()).getPlan();
            final Account merchantAccount = findMerchantAccount(line.getMerchantId(), order);
            final Transaction merchantTx = toMerchantTransaction(order, line, merchantAccount, plan);
            transactionRepository.save(merchantTx);

            merchantAccount.setBalance(merchantAccount.getBalance().add(merchantTx.getNet()));
            accountRepository.save(merchantAccount);
        }
    }

    private Account findMerchantAccount(final Integer merchantId, final Order order){
        Account account = accountRepository.findByMerchantId(merchantId);
        if (account == null){
            account = new Account();
            account.setBalance(BigDecimal.ZERO);
            account.setCurrencyCode(order.getCurrencyCode());
            account.setMerchantId(merchantId);

            accountRepository.save(account);
        }
        return account;
    }

    private Transaction toMerchantTransaction(final Order order, final OrderLine line, final Account account, final PlanDto plan) {
        final Transaction tx = new Transaction();
        final BigDecimal fees = line.getTotalPrice().multiply(plan.getPercent()).add(plan.getAmount());
        final BigDecimal net = line.getTotalPrice().subtract(fees);

        tx.setAccount(account);
        tx.setAmount(line.getTotalPrice());
        tx.setFees(fees);
        tx.setNet(net);
        tx.setEntryDateTime(DateHelper.now());
        tx.setReferenceId(line.getBookingId());
        tx.setTransactionDateTime(order.getOrderDateTime());
        tx.setType("ORDER");

        return tx;
    }
}
