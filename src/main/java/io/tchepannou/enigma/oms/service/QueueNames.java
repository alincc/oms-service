package io.tchepannou.enigma.oms.service;

public interface QueueNames {
    String QUEUE_ORDER_FINANCE = "enigma-order-finance-queue";
    String QUEUE_ORDER_CANCEL = "enigma-order-cancel-queue";
    String QUEUE_TICKET_SMS = "enigma-ticket-sms-queue";

    String EXCHANGE_ORDER_CONFIRMED = "enigma-order-confirmed";
    String EXCHANGE_ORDER_CANCELLED = "enigma-order-cancelled";
}
