package io.tchepannou.enigma.oms.service.ticket;

import com.google.common.base.Joiner;
import io.tchepannou.enigma.ferari.client.dto.ProductDto;
import io.tchepannou.enigma.oms.backend.ferari.ProductBackend;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.CityBackend;
import io.tchepannou.enigma.refdata.client.dto.CityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ticket message generator:
 * <code>
 * _TICKET_ID_-_BOOKING_ID_
 * _ORIGIN_,_DESTINATION_,_DEPARTURE_DATE_
 * _CARRIER_,_PRODUCT_
 * _CUSTOMER_NAME_
 * </code>
 */
@Component
public class TicketSmsGenerator {
    public static final int MAXLEN_CITY = 8;
    public static final int MAXLEN_CARRIER = 30;
    public static final int MAXLEN_PRODUCT = 30;
    public static final int MAXLEN_CUSTOMER = 50;

    @Autowired
    private CityBackend cityBackend;

    @Autowired
    private ProductBackend productBackend;

    @Autowired
    private MerchantBackend merchantBackend;

    public String generate(final Ticket ticket) {
        final List<Integer> cityIds = Arrays.asList(ticket.getOriginId(), ticket.getDestinationId());
        final Map<Integer, CityDto> cities = cityBackend.findByIds(cityIds).getCities()
                .stream()
                .collect(Collectors.toMap(CityDto::getId, Function.identity()));
        final CityDto origin = cities.get(ticket.getOriginId());
        final CityDto destination = cities.get(ticket.getDestinationId());

        return String.format(
                "%s\n%s,%s\n%s\n%s,%s",
                id(ticket),
                name(origin), name(destination), departureDate(ticket, origin),
                carrier(ticket), product(ticket),
                passenger(ticket)
        ).toUpperCase();
    }

    private String id(final Ticket ticket) {
        final NumberFormat fmt = new DecimalFormat("000000");
        return String.format("%s-%s",
            fmt.format(ticket.getId()),
            fmt.format(ticket.getOrderLine().getBookingId())
        );
    }

    private String name(final CityDto city) {
        final String displayName = city.getDisplayName();
        return toString(displayName, MAXLEN_CITY);
    }

    private String departureDate(final Ticket ticket, CityDto city) {
        final TimeZone tz = TimeZone.getTimeZone(city.getTimezoneId());
        final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fmt.setTimeZone(tz);
        return fmt.format(ticket.getDepartureDateTime());
    }

    private String carrier (final Ticket ticket) {
        final MerchantDto merchant = merchantBackend.findById(ticket.getOrderLine().getMerchantId());
        return toString(merchant.getName(), MAXLEN_CARRIER);
    }

    private String product (final Ticket ticket) {
        final ProductDto product = productBackend.findById(ticket.getProductId());
        return toString(product.getProductType().getName(), MAXLEN_PRODUCT);
    }

    private String passenger(final Ticket ticket) {
        final Order order = ticket.getOrderLine().getOrder();
        final String name = Joiner.on(" ")
                .skipNulls()
                .join(order.getFirstName(), order.getLanguageCode());
        return toString(name, MAXLEN_CUSTOMER);
    }

    private String toString(final String str, final int maxlen){
        if (str == null){
            return "";
        }
        return str.length() > maxlen ? str.substring(0, maxlen) : str;
    }
}
