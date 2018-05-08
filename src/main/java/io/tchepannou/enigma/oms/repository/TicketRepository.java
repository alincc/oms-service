package io.tchepannou.enigma.oms.repository;

import io.tchepannou.enigma.oms.client.TicketStatus;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Ticket;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TicketRepository extends CrudRepository<Ticket, Integer>{
    @Query("SELECT t FROM Ticket t WHERE t.orderLine.order = ?1")
    List<Ticket> findByOrder(Order order);

    @Query("SELECT t FROM Ticket t WHERE t.orderLine.bookingId = ?1")
    List<Ticket> findByBookingId(Integer bookingId);

    @Query("SELECT t FROM Ticket t WHERE t.orderLine.order.customerId = ?1 AND t.status=?2 AND t.expiryDateTime>=?3")
    List<Ticket> findByUserIdAndStatusAndExpiryDate(Integer userId, TicketStatus ticketStatus, Date expiryDate);
}
