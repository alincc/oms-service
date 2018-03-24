package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.backend.refdata.CityBackend;
import io.tchepannou.enigma.oms.backend.refdata.SiteBackend;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.oms.service.MailService;
import io.tchepannou.enigma.oms.service.mq.MQConsumer;
import io.tchepannou.enigma.oms.service.order.model.OrderLineModel;
import io.tchepannou.enigma.oms.service.order.model.OrderMailModel;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.dto.CityDto;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseNotificationConsumer extends MQConsumer {
    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected CityBackend cityBackend;

    @Autowired
    protected MerchantBackend merchantBackend;

    @Autowired
    protected MailService emailService;

    @Autowired
    protected SiteBackend siteBackend;

    @Value("${enigma.assetUrl}")
    protected String assetUrl;


    protected Mail buildMail (
            final String subject,
            final String to,
            final String template,
            final Locale locale,
            final SiteDto site
    ){
        final String subjectPrefix = "[" + site.getBrandName() + "]";
        final Mail mail = new Mail();
        mail.setSubject(subjectPrefix + " " + subject);
        mail.setTo(to);
        mail.setTemplate(template);
        mail.setLocale(locale);
        return mail;
    }

    protected OrderMailModel buildOrderMail(
            final Order order,
            final SiteDto site,
            final Locale locale,
            final OrderLineFilter filter,
            final RestClient rest
    ) throws InvalidCarOfferTokenException{
        // Order
        final OrderMailModel model = new OrderMailModel();

        // Cities
        final Set<Integer> merchantIds = new HashSet<>();
        final Set<Integer> cityIds = new HashSet<>();
        final Map<Integer, TransportationOfferToken> offerTokens = new HashMap<>();
        for (OrderLine line : order.getLines()){
            final TransportationOfferToken token = TransportationOfferToken.decode(line.getOfferToken());

            cityIds.add(token.getDestinationId());
            cityIds.add(token.getOriginId());
            merchantIds.add(line.getMerchantId());
            offerTokens.put(line.getId(), token);
        }

        final Map<Integer, CityDto> cities = cityBackend.search(cityIds, rest).stream()
                .collect(Collectors.toMap(CityDto::getId, Function.identity()));
        final Map<Integer, MerchantDto> merchants = merchantBackend.search(merchantIds, rest).stream()
                .collect(Collectors.toMap(MerchantDto::getId, Function.identity()));;

        // Lines
        final DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        final DateFormat dateTimeFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        model.setLines(
            order.getLines().stream()
                    .filter(line -> filter.accept(line))
                    .map(line -> {
                        final TransportationOfferToken token = offerTokens.get(line.getId());

                        OrderLineModel data = new OrderLineModel();
                        data.setId(line.getId());
                        data.setBookingId(line.getBookingId());
                        data.setMerchantId(line.getMerchantId());
                        data.setQuantity(line.getQuantity());
                        data.setUnitPrice(line.getUnitPrice());
                        data.setTotalPrice(line.getTotalPrice());
                        data.setUnitPrice(line.getUnitPrice());
                        data.setTotalPrice(line.getTotalPrice());
                        data.setDestination(cities.get(token.getDestinationId()));
                        data.setOrigin(cities.get(token.getOriginId()));
                        data.setMerchant(merchants.get(line.getMerchantId()));
                        data.setFormattedArrivalTime(timeFormat.format(token.getArrivalDateTime()));
                        data.setFormattedDepartureTime(timeFormat.format(token.getDepartureDateTime()));
                        data.setFormattedDepartureDate(dateFormat.format(token.getDepartureDateTime()));
                        data.setFormattedUnitPrice(formatMoney(line.getUnitPrice(), site));
                        data.setFormattedTotalPrice(formatMoney(line.getTotalPrice(), site));

                        return data;
                    })
                    .collect(Collectors.toList())
        );

        // Total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineModel line : model.getLines()){
            total = total.add(line.getTotalPrice());
        }

        model.setOrderId(order.getId());
        model.setSiteLogoUrl(assetUrl + site.getLogoUrl());
        model.setSiteBrandName(site.getBrandName());
        model.setCustomerName(order.getFirstName() + " " + order.getLastName());
        model.setFormattedOrderDateTime(dateTimeFormat.format(order.getOrderDateTime()));
        model.setFormattedTotalPrice(formatMoney(total, site));

        return model;
    }

    protected String formatMoney(final Number amount, final SiteDto site){
        final Locale locale = Locale.getDefault();
        final NumberFormat fmt = NumberFormat.getNumberInstance(locale);
        return String.format("%s %s", fmt.format(amount), site.getCurrency().getSymbol());
    }

    public interface OrderLineFilter {
        boolean accept(OrderLine line);
    }
}
