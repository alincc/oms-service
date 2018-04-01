package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Ticket;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends CrudRepository<Ticket, Integer>{
    @Query("select t from Ticket t where t.orderLine.order = ?1")
    List<Ticket> findByOrder(Order order);

    @Query("select t from Ticket t where t.orderLine.bookingId = ?1")
    List<Ticket> findByBookingId(Integer bookingId);
}
