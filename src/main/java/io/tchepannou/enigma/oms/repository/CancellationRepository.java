package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Cancellation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CancellationRepository extends CrudRepository<Cancellation, Integer> {
}
