package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Traveller;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravellerRepository extends CrudRepository<Traveller, Integer> {
    List<Traveller> findByOrder(Order order);
}
