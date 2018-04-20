package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Integer>{
    Transaction findByOrderAndType(Order order, TransactionType type);
}
