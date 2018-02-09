package io.tchepannou.enigma.oms.service.mq;

public interface QueueNames {
    String QUEUE_FINANCE = "enigma-order-finance-queue";
    String QUEUE_NOTIFICATION_CUSTOMER = "enigma-order-customer-queue";
    String QUEUE_NOTIFICATION_MERCHANT = "enigma-order-merchant-queue";

    String EXCHANGE_NEW_ORDER = "enigma-new-order-exchange";
}
