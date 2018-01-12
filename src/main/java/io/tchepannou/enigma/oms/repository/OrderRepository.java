package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order, Integer> {
    List<Order> findByStatusAndExpiryDateTimeLessThan(OrderStatus status, Date expiryDateTime);
}
