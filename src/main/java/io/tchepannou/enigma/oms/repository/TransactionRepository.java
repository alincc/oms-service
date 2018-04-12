package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Integer>{
    List<Transaction> findByAccount(Account account);
    List<Transaction> findByCorrelationId(String correlationId);
}
