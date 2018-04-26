package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Fees;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeesRepository extends CrudRepository<Fees, Integer> {
    List<Fees> findBySiteId(Integer siteId);
}
