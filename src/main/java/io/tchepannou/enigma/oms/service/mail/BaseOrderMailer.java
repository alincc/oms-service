package io.tchepannou.enigma.oms.service.mail;

import com.google.common.base.Strings;
import io.tchepannou.enigma.ferari.client.CarOfferToken;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.backend.refdata.CityBackend;
import io.tchepannou.enigma.oms.backend.refdata.SiteBackend;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.oms.service.MailService;
import io.tchepannou.enigma.oms.service.Mapper;
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

public abstract class BaseOrderMailer {
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

    @Autowired
    protected Mapper mapper;

    @Value("${enigma.assetUrl}")
    protected String assetUrl;


    protected Order findOrder(final Integer orderId){
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }
        if (Strings.isNullOrEmpty(order.getEmail())){
            throw new NotFoundException(OMSErrorCode.CUSTOMER_HAS_NO_EMAIL);
        }
        if (!OrderStatus.CONFIRMED.equals(order.getStatus())){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_CONFIRMED);
        }
        return order;
    }

    protected Mail buildMail (
            final String subject,
            final String to,
            final String template,
            final SiteDto site
    ){
        final String subjectPrefix = "[" + site.getBrandName() + "]";
        final Mail mail = new Mail();
        mail.setSubject(subjectPrefix + " " + subject);
        mail.setTo(to);
        mail.setTemplate(template);
        return mail;
    }

    protected OrderMailModel buildOrderMail(
            final Order order,
            final SiteDto site,
            final OrderLineFilter filter
    ) throws InvalidCarOfferTokenException{
        // Order
        final OrderMailModel model = new OrderMailModel();

        // Cities
        final Set<Integer> merchantIds = new HashSet<>();
        final Set<Integer> cityIds = new HashSet<>();
        final Map<Integer, CarOfferToken> offerTokens = new HashMap<>();
        for (OrderLine line : order.getLines()){
            final CarOfferToken token = CarOfferToken.decode(line.getOfferToken());

            cityIds.add(token.getDestinationId());
            cityIds.add(token.getOriginId());
            merchantIds.add(line.getMerchantId());
            offerTokens.put(line.getId(), token);
        }

        final Map<Integer, CityDto> cities = cityBackend.search(cityIds).stream()
                .collect(Collectors.toMap(CityDto::getId, Function.identity()));
        final Map<Integer, MerchantDto> merchants = merchantBackend.search(merchantIds).stream()
                .collect(Collectors.toMap(MerchantDto::getId, Function.identity()));;

        // Lines
        final DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        final DateFormat dateTimeFormat = SimpleDateFormat.getDateTimeInstance();
        model.setLines(
            order.getLines().stream()
                    .filter(line -> filter.accept(line))
                    .map(line -> {
                        final CarOfferToken token = offerTokens.get(line.getId());

                        OrderLineData data = new OrderLineData();
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
        for (OrderLineData line : model.getLines()){
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
