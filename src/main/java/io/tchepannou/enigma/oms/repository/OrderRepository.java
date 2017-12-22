package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends CrudRepository<Order, Integer> {
}
