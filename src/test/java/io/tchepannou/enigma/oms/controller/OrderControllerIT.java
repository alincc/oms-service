package io.tchepannou.enigma.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.tchepannou.core.rest.Headers;
import io.tchepannou.core.test.jetty.StubHandler;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.Sex;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.mq.NewOrderConsumer;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@Sql({"classpath:/sql/clean.sql", "classpath:/sql/OrderController.sql"})
@ActiveProfiles(profiles = {"stub"})
@SuppressWarnings("CPD-START")
public class OrderControllerIT {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private TicketRepository ticketRepository;


    private DateFormat dateFormat;


    @Value("${enigma.service.ferari.port}")
    private int ferariPort;

    @Value("${enigma.service.profile.port}")
    private int profilePort;

    @Value("${enigma.service.refdata.port}")
    private int refdataPort;

    @Value("${enigma.test.sleepMillis}")
    private long sleepMillis;

    @Autowired
    private NewOrderConsumer receiver;

    private Server ferari;
    private StubHandler ferariHanderStub;

    private Server refdata;
    private Server profile;

    private static GreenMail mail;

    @BeforeClass
    public static void setUpAll(){
        mail = new GreenMail(ServerSetupTest.SMTP);
        mail.start();

    }

    @AfterClass
    public static void tearDownAll(){
        mail.stop();
    }

    @Before
    public void setUp() throws Exception{
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        this.dateFormat = DateHelper.createDateFormat();

        ferariHanderStub = new StubHandler();
        ferari = StubHandler.start(ferariPort, ferariHanderStub);

        refdata = StubHandler.start(refdataPort, new StubHandler());
        profile = StubHandler.start(profilePort, new StubHandler());

    }

    @After
    public void tearDown() throws Exception {
        StubHandler.stop(ferari, refdata, profile);
    }


    /* =========== CREATE ============ */
    @Test
    public void shouldCreateOrder() throws Exception {
        final CreateOrderRequest request = createCreateOrderRequest(2);

        final MvcResult result = mockMvc
                .perform(
                        post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.orderId", notNullValue()))

                .andReturn();

        // Then
        final CreateOrderResponse response = mapper.readValue(result.getResponse().getContentAsString(), CreateOrderResponse.class);
        final Integer id = response.getOrderId();
        final Order order = orderRepository.findOne(id);
        assertThat(order).isNotNull();

        assertThat(order.getExpiryDateTime()).isNotNull();
        assertThat(order.getOrderDateTime()).isNotNull();
        assertThat(order.getCurrencyCode()).isEqualTo("XAF");
        assertThat(order.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(order.getPaymentId()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal(12000).setScale(2));
        assertThat(order.getSiteId()).isEqualTo(request.getSiteId());
        assertThat(order.getLanguageCode()).isNull();


        final List<OrderLine> lines = orderLineRepository.findByOrder(order);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getUnitPrice()).isEqualTo(new BigDecimal(6000).setScale(2));
        assertThat(lines.get(0).getTotalPrice()).isEqualTo(new BigDecimal(12000).setScale(2));
        assertThat(lines.get(0).getQuantity()).isEqualTo(2);
        assertThat(lines.get(0).getBookingId()).isNull();
        assertThat(lines.get(0).getDescription()).isEqualTo(request.getOfferLines().get(0).getDescription());
        assertThat(lines.get(0).getOfferToken()).isEqualTo(request.getOfferLines().get(0).getToken());
        assertThat(lines.get(0).getMerchantId()).isEqualTo(request.getOfferLines().get(0).getMerchantId());
        assertThat(lines.get(0).getOfferType()).isEqualTo(request.getOfferLines().get(0).getType());
    }

    @Test
    public void shouldNotCreateOrderWithInvalidToken() throws Exception {
        final CreateOrderRequest request = createCreateOrderRequest();
        request.getOfferLines().get(0).setToken("122094039");

        mockMvc
                .perform(
                        post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isConflict())

                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(OMSErrorCode.MALFORMED_OFFER_TOKEN.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(OMSErrorCode.MALFORMED_OFFER_TOKEN.getText())))
        ;

    }



    /* =========== CHECKOUT ============ */
    @Test
    public void shouldCheckoutOrderWithMobileMoney() throws Exception {
        final CheckoutOrderRequest request = createCheckoutOrderRequest();
        final String deviceUID = UUID.randomUUID().toString();
        final Date now = DateHelper.now();

        mockMvc
                .perform(
                        post("/v1/orders/100/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header(Headers.DEVICE_UID, deviceUID)
                )


                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andReturn()
        ;
        Thread.sleep(sleepMillis);

        // Then
        final Order order = orderRepository.findOne(100);
        assertThat(order).isNotNull();

        assertThat(order.getPaymentId()).isEqualTo(-1);
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(order.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(order.getLastName()).isEqualTo(request.getLastName());
        assertThat(order.getEmail()).isEqualTo(request.getEmail());
        assertThat(order.getDeviceUID()).isEqualTo(deviceUID);
        assertThat(order.getLanguageCode()).isEqualTo(request.getLanguageCode());

        final List<Traveller> travellers = travellerRepository.findByOrder(order);
        assertThat(travellers).hasSize(2);
        assertThat(travellers.get(0).getFirstName()).isEqualTo("John");
        assertThat(travellers.get(0).getLastName()).isEqualTo("Doe");
        assertThat(travellers.get(0).getSex()).isEqualTo(Sex.M);
        assertThat(travellers.get(0).getEmail()).isEqualTo("john.doe@gmail.com");
        assertThat(travellers.get(1).getFirstName()).isEqualTo("Jane");
        assertThat(travellers.get(1).getLastName()).isEqualTo("Smith");
        assertThat(travellers.get(1).getSex()).isEqualTo(Sex.F);
        assertThat(travellers.get(1).getEmail()).isEqualTo("jane.smith@gmail.com");

        final List<OrderLine> lines = orderLineRepository.findByOrder(order);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getBookingId()).isEqualTo(1000);
        assertThat(lines.get(1).getBookingId()).isEqualTo(1001);

        final List<Ticket> tickets = ticketRepository.findByOrder(order);
        assertThat(tickets).hasSize(3);

        assertThat(tickets.get(0).getOrderLine()).isEqualTo(lines.get(0));
        assertThat(tickets.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(tickets.get(0).getFirstName()).isEqualTo("John");
        assertThat(tickets.get(0).getLastName()).isEqualTo("Doe");
        assertThat(tickets.get(0).getSex()).isEqualTo(Sex.M);
        assertThat(tickets.get(0).getPrintDateTime()).isAfter(now);
        assertThat(tickets.get(0).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(0).getHash()).isNotNull();

        assertThat(tickets.get(1).getOrderLine()).isEqualTo(lines.get(1));
        assertThat(tickets.get(1).getSequenceNumber()).isEqualTo(1);
        assertThat(tickets.get(1).getFirstName()).isEqualTo("John");
        assertThat(tickets.get(1).getLastName()).isEqualTo("Doe");
        assertThat(tickets.get(1).getSex()).isEqualTo(Sex.M);
        assertThat(tickets.get(1).getPrintDateTime()).isAfter(now);
        assertThat(tickets.get(1).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(1).getHash()).isNotNull();

        assertThat(tickets.get(2).getOrderLine()).isEqualTo(lines.get(1));
        assertThat(tickets.get(2).getSequenceNumber()).isEqualTo(2);
        assertThat(tickets.get(2).getFirstName()).isEqualTo("Jane");
        assertThat(tickets.get(2).getLastName()).isEqualTo("Smith");
        assertThat(tickets.get(2).getSex()).isEqualTo(Sex.F);
        assertThat(tickets.get(2).getPrintDateTime()).isAfter(now);
        assertThat(tickets.get(2).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(2).getHash()).isNotNull();

    }

//    @Test
//    public void shouldNotCheckoutOrderWhenPaymentFailed() throws Exception {
//        final CheckoutOrderRequest request = createCheckoutOrderRequest();
//
//        mockMvc
//                .perform(
//                        post("/v1/orders/100/checkout")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(mapper.writeValueAsString(request))
//                )
//
//                .andDo(MockMvcResultHandlers.print())
//                .andExpect(status().isConflict())
//        ;
//
//        // Then
//        final Order order = orderRepository.findOne(100);
//        assertThat(order).isNotNull();
//
//        assertThat(order.getPaymentId()).isNull();
//        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
//    }

    @Test
    public void shouldNotCheckoutExpiredOrder() throws Exception {
        final CheckoutOrderRequest request = createCheckoutOrderRequest();

        mockMvc
                .perform(
                        post("/v1/orders/900/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isConflict())

                .andExpect(jsonPath("$.errors[0].code", is(OMSErrorCode.ORDER_EXPIRED.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(OMSErrorCode.ORDER_EXPIRED.getText())))
        ;
    }

    @Test
    public void shouldNotCheckoutCancelledOrder() throws Exception {
        final CheckoutOrderRequest request = createCheckoutOrderRequest();

        mockMvc
                .perform(
                        post("/v1/orders/901/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isConflict())

                .andExpect(jsonPath("$.errors[0].code", is(OMSErrorCode.ORDER_CANCELLED.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(OMSErrorCode.ORDER_CANCELLED.getText())))
        ;
    }

    @Test
    @Ignore
    public void shouldPublishEventOnCheckout() throws Exception {
        // When
        final CheckoutOrderRequest request = createCheckoutOrderRequest();

        mockMvc
                .perform(
                        post("/v1/orders/110/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
        ;
        Thread.sleep(sleepMillis);

        // Then
        assertThat(receiver.getOrderId()).isEqualTo(110);
    }

    /* =========== GET ============ */
    @Test
    public void shouldNotReturnInvalidOrder() throws Exception {
        mockMvc
                .perform(get("/v1/orders/99999"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void shouldReturnOrder() throws Exception {
        mockMvc
                .perform(get("/v1/orders/200"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.order.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.order.currencyCode", is("XAF")))
                .andExpect(jsonPath("$.order.totalAmount", is(6000d)))
                .andExpect(jsonPath("$.order.orderDateTime", notNullValue()))
                .andExpect(jsonPath("$.order.expiryDateTime", notNullValue()))
                .andExpect(jsonPath("$.order.paymentMethod", is("ONLINE")))
                .andExpect(jsonPath("$.order.paymentId", is(123)))
                .andExpect(jsonPath("$.order.siteId", is(1)))
                .andExpect(jsonPath("$.order.deviceUID", is("1234-1234")))
                .andExpect(jsonPath("$.order.languageCode", is("fr")))

                .andExpect(jsonPath("$.order.lines.length()", is(1)))
                .andExpect(jsonPath("$.order.lines[0].bookingId", is(5678)))
                .andExpect(jsonPath("$.order.lines[0].offerType", is("CAR")))
                .andExpect(jsonPath("$.order.lines[0].offerToken", notNullValue()))
                .andExpect(jsonPath("$.order.lines[0].description", is("hello")))
                .andExpect(jsonPath("$.order.lines[0].unitPrice", is(6000d)))
                .andExpect(jsonPath("$.order.lines[0].totalPrice", is(6000d)))
                .andExpect(jsonPath("$.order.lines[0].quantity", is(1)))
                .andExpect(jsonPath("$.order.lines[0].merchantId", is(2001)))

                .andExpect(jsonPath("$.order.customer.id", is(3)))
                .andExpect(jsonPath("$.order.customer.firstName", is("Ray")))
                .andExpect(jsonPath("$.order.customer.lastName", is("Sponsible")))
                .andExpect(jsonPath("$.order.customer.email", is("ray@gmail.com")))
        ;

    }



    private CreateOrderRequest createCreateOrderRequest() throws Exception {
        return createCreateOrderRequest(2);
    }

    private CheckoutOrderRequest createCheckoutOrderRequest() throws Exception {
        final CheckoutOrderRequest request = new CheckoutOrderRequest();
        request.setTravellers(
            Arrays.asList(
                createTraveller("John", "Doe", Sex.M, "john.doe@gmail.com"),
                createTraveller("Jane", "Smith", Sex.F, "jane.smith@gmail.com")
            )
        );

        final MobilePaymentDto mobile = new MobilePaymentDto();
        mobile.setCountryCode("237");
        mobile.setNumber("99505678");
        mobile.setProvider("Orange");

        request.setCustomerId(11);
        request.setFirstName("Ray");
        request.setLastName("Sponsible");
        request.setEmail("ray.sponsible@gmail.com");
        request.setMobilePayment(mobile);
        request.setLanguageCode("fr");

        return request;
    }

    private CreateOrderRequest createCreateOrderRequest(int travellerCount) throws Exception {
        final Date departureDate = dateFormat.parse("2030-04-15T05:00:00+0000");
        final TransportationOfferToken TransportationOfferToken = createTransportationOfferToken(100, 1000 , departureDate, travellerCount);

        final List<OfferLineDto> lines = new ArrayList();
        final OfferLineDto line = new OfferLineDto();
        line.setToken(TransportationOfferToken.toString());
        line.setType(OfferType.CAR);
        line.setDescription("description #1");
        line.setMerchantId(101);

        lines.add(line);


        final CreateOrderRequest request = new CreateOrderRequest();
        request.setOfferLines(lines);
        request.setCustomerId(1);
        request.setSiteId(1);

        return request;
    }

    private TransportationOfferToken createTransportationOfferToken(Integer productId, Integer priceId, Date departureDate, int travellerCount){
        final TransportationOfferToken token = new TransportationOfferToken();
        token.setProductId(productId);
        token.setTravellerCount(travellerCount);
        token.setPriceId(priceId);
        token.setAmount(BigDecimal.valueOf(6000));
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(departureDate);
        token.setArrivalDateTime(DateUtils.addHours(departureDate, 2));
        token.setOriginId(2370001);
        token.setDestinationId(2370002);
        token.setExpiryDateTime(new Date(10*System.currentTimeMillis()));
        return token;
    }

    private TravellerDto createTraveller(String firstName, String lastName, Sex sex, String email){
        TravellerDto dto = new TravellerDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setSex(sex);
        dto.setEmail(email);
        return dto;
    }

}
