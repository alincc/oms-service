package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.client.AccountType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<Account, Integer>{
    Account findByTypeAndReferenceId(AccountType type, Integer referenceId);
}
