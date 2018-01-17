package io.tchepannou.enigma.oms.backend.ferari;

import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class FerariMapperTest {

    @InjectMocks
    private FerariMapper mapper;

    @Test
    public void toCreateBookingRequest() throws Exception {
        // Given
        final Order order = new Order();
        order.setLines(Arrays.asList(
                createOrderLine("1"),
                createOrderLine("2"),
                createOrderLine("3")
        ));

        // When
        CreateBookingRequest request = mapper.toCreateBookingRequest(order);

        // Then
        assertThat(request.getOfferTokens()).containsExactly("1", "2", "3");
    }

    private OrderLine createOrderLine(final String token) {
        final OrderLine line = new OrderLine();
        line.setOfferToken(token);
        return line;
    }

}
