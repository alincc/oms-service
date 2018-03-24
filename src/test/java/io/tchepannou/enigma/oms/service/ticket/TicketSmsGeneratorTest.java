package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.ferari.client.dto.ProductDto;
import io.tchepannou.enigma.ferari.client.dto.ProductTypeDto;
import io.tchepannou.enigma.oms.backend.ferari.ProductBackend;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.backend.refdata.CityBackend;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.dto.CityDto;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TicketSmsGeneratorTest {
    @Mock
    private CityBackend cityBackend;

    @Mock
    private ProductBackend productBackend;

    @Mock
    private MerchantBackend merchantBackend;

    @InjectMocks
    private TicketSmsGenerator generator;

    @Test
    public void generate() throws Exception {
        // Given
        final TransportationOfferToken offerToken = createOfferToken(1, 2, 100);
        final Ticket ticket = createTicket(1, 1000, "Ray", "Sponsible", offerToken);

        final MerchantDto merchant = createMerchant(ticket.getMerchantId(), "Buca Voyages");
        when(merchantBackend.findById(1000)).thenReturn(merchant);

        final ProductDto product = createProduct(offerToken.getProductId(), "vip");
        when(productBackend.findById(100)).thenReturn(product);

        final CityDto origin = createCity(1, "Yaounde");
        final CityDto destination = createCity(2, "Baffoussam");
        when(cityBackend.search(anyCollection())).thenReturn(Arrays.asList(origin, destination));

        // When
        final String result = generator.generate(ticket);

        // Then
        final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//        fmt.setTimeZone(DateHelper.getTimeZone());

        final String expected = "000001\n"
                + "YAOUNDE,BAFFOUSS\n"
                + fmt.format(offerToken.getDepartureDateTime()) + "\n"
                + "BUCA VOYAGES,VIP";
        assertThat(result).isEqualTo(expected);
    }

    private TransportationOfferToken createOfferToken(
            Integer originId,
            Integer destinationId,
            Integer productId
    ){
        final TransportationOfferToken token = new TransportationOfferToken();
        final Calendar departureDate = DateHelper.getCalendar();
        departureDate.set(Calendar.HOUR_OF_DAY, 15);
        departureDate.set(Calendar.MINUTE, 30);

        token.setDirection(Direction.OUTBOUND);
        token.setOriginId(originId);
        token.setAmount(new BigDecimal(100d));
        token.setDestinationId(destinationId);
        token.setArrivalDateTime(new Date());
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(departureDate.getTime());
        token.setExpiryDateTime(DateUtils.addDays(new Date(), 1));
        token.setPriceId(1);
        token.setProductId(productId);
        token.setTravellerCount(1);
        return token;
    }
    private Ticket createTicket(
            final Integer id,
            final Integer merchantId,
            final String firstName,
            final String lastName,
            final TransportationOfferToken offerToken
    ){
        final Order order = new Order ();
        order.setMobileNumber("4309430943");

        final OrderLine line = new OrderLine();
        line.setOrder(order);
        line.setMerchantId(merchantId);
        line.setOfferToken(offerToken.toString());

        final Ticket ticket = new Ticket();
        ticket.setOrderLine(line);
        ticket.setId(id);
        ticket.setOriginId(offerToken.getOriginId());
        ticket.setDestinationId(offerToken.getDestinationId());
        ticket.setDepartureDateTime(offerToken.getDepartureDateTime());
        ticket.setProductId(offerToken.getProductId());
        ticket.setMerchantId(merchantId);
        ticket.setFirstName(firstName);
        ticket.setLastName(lastName);
        return ticket;
    }

    private MerchantDto createMerchant(Integer id, String name){
        MerchantDto merchant = new MerchantDto();
        merchant.setId(id);
        merchant.setName(name);
        return merchant;
    }

    private ProductDto createProduct(Integer id, String name){
        ProductTypeDto type = new ProductTypeDto();
        type.setId(id);
        type.setName(name);

        ProductDto product = new ProductDto();
        product.setId(id);
        product.setProductType(type);
        return product;
    }

    private CityDto createCity(Integer id, String name){
        final CityDto city = new CityDto();
        city.setId(id);
        city.setName(name);
        city.setDisplayName(name);
        return city;
    }
}
