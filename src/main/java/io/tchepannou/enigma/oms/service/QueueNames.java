package io.tchepannou.enigma.oms.service;

public interface QueueNames {
    String QUEUE_TICKET_SMS = "enigma-ticket-sms-queue";

    String EXCHANGE_ORDER_CONFIRMED = "enigma-order-confirmed";
    String EXCHANGE_ORDER_CANCELLED = "enigma-order-cancelled";
}
