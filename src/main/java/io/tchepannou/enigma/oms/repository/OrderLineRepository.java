package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLineRepository extends CrudRepository<OrderLine, Integer> {
    List<OrderLine> findByOrder(Order order);
    OrderLine findByOrderAndBookingId(Order order, Integer bookingId);
}
