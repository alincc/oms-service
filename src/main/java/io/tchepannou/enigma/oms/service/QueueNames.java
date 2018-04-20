package io.tchepannou.enigma.oms.service;

public interface QueueNames {
    String QUEUE_TICKET_SMS = "enigma-ticket-sms-queue";
    String QUEUE_ORDER_REFUND = "enigma-order-refund-queue";
    String QUEUE_BOOKING_CANCEL = "enigma-booking-cancel-queue";

    String EXCHANGE_ORDER_CONFIRMED = "enigma-order-confirmed";
    String EXCHANGE_ORDER_CANCELLED = "enigma-order-cancelled";
}
