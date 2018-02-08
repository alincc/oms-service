package io.tchepannou.enigma.oms.service.mq;

public interface QueueNames {
    String QUEUE_FINANCE = "enigma-order-finance-queue";
    String QUEUE_NOTIFICATION_CUSTOMER = "enigma-order-customer-queue";
    String QUEUE_NOTIFICATION_MERCHANT = "enigma-order-merchant-queue";

    String EXCHANGE_FINANCE = "enigma-order-finance-exchange";
    String EXCHANGE_NOTIFICATION_CUSTOMER = "enigma-order-customer-exchange";
    String EXCHANGE_NOTIFICATION_MERCHANT = "enigma-order-merchant-exchange";
}
