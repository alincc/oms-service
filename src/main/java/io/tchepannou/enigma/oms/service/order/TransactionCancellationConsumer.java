package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.oms.client.AccountType;
import io.tchepannou.enigma.oms.domain.Cancellation;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.repository.CancellationRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.service.QueueNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionCancellationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionCancellationConsumer.class);

    @Autowired
    private Clock clock;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CancellationRepository cancellationRepository;

    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_ORDER_CANCEL)
    public void onOrderCancelled(Integer cancelId){
        LOGGER.info("Consuming {}", cancelId);

        final Cancellation cancellation = cancellationRepository.findOne(cancelId);
        if (cancellation == null){
            LOGGER.warn("Cancellation#{} not found", cancelId);
            return;
        }
        if (cancellation.getTransaction() != null){
            LOGGER.warn("Cancellation#{} already processed", cancelId);
            return;
        }

        final Integer bookingId = cancellation.getBookingId();
        final Order order = cancellation.getOrder();
        for (final OrderLine line : order.getLines()){
            if (!line.getBookingId().equals(bookingId)){
                continue;
            }

            final Transaction originTx = line.getTransaction();
            if (originTx != null){
                Transaction merchantTx = revertMerchantTransaction(originTx, cancellation);
                link(cancellation, originTx);

                final List<Transaction> transactions = transactionRepository.findByCorrelationId(originTx.getCorrelationId());
                for (final Transaction tx : transactions){
                    if (AccountType.SITE.equals(tx.getAccount().getType())){
                        revertSiteTransaction(merchantTx, tx, cancellation);
                    }
                }
            }
        }
    }

    private Transaction revertMerchantTransaction(final Transaction originTx, final Cancellation cancellation) {
        final Transaction tx = new Transaction();
        final BigDecimal amount = originTx.getNet().multiply(new BigDecimal(-1d));
        final BigDecimal fees = BigDecimal.ZERO;
        final BigDecimal net = amount;

        tx.setAccount(originTx.getAccount());
        tx.setAmount(amount);
        tx.setFees(fees);
        tx.setNet(net);
        tx.setEntryDateTime(new Date(clock.millis()));
        tx.setReferenceId(cancellation.getId());
        tx.setTransactionDateTime(cancellation.getCancellationDateTime());
        tx.setType(TransactionType.CANCELLATION);
        tx.setCorrelationId(UUID.randomUUID().toString());

        transactionRepository.save(tx);
        return tx;
    }

    private Transaction revertSiteTransaction(final Transaction merchantTx, final Transaction originTx, final Cancellation cancellation) {
        final Transaction tx = new Transaction();
        final BigDecimal amount = originTx.getNet().multiply(new BigDecimal(-1d));
        final BigDecimal fees = BigDecimal.ZERO;
        final BigDecimal net = amount;

        tx.setAccount(originTx.getAccount());
        tx.setAmount(amount);
        tx.setFees(fees);
        tx.setNet(net);
        tx.setEntryDateTime(new Date(clock.millis()));
        tx.setReferenceId(merchantTx.getReferenceId());
        tx.setTransactionDateTime(cancellation.getCancellationDateTime());
        tx.setType(TransactionType.CANCELLATION);
        tx.setCorrelationId(merchantTx.getCorrelationId());

        transactionRepository.save(tx);
        return tx;
    }

    private void link(Cancellation cancellation, Transaction tx){
        cancellation.setTransaction(tx);
        cancellationRepository.save(cancellation);
    }
}
