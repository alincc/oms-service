package io.tchepannou.enigma.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.Headers;
import io.tchepannou.core.test.jetty.StubHandler;
import io.tchepannou.enigma.ferari.client.FerariEnvironment;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderLineType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.Sex;
import io.tchepannou.enigma.oms.client.TicketStatus;
import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CancelOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CancelOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.profile.client.ProfileEnvironment;
import io.tchepannou.enigma.refdata.client.RefDataEnvironment;
import org.apache.commons.lang.time.DateUtils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
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

    @Autowired
    private RefDataEnvironment refDataEnvironment;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProfileEnvironment profileEnvironment;

    @Autowired
    private FerariEnvironment ferariEnvironment;

    private DateFormat dateFormat;

    @Value("${enigma.test.sleepMillis}")
    private long sleepMillis;

    private Server ferari;
    private Server refdata;
    private Server profile;

    @Before
    public void setUp() throws Exception{
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        this.dateFormat = DateHelper.createDateFormat();

        ferari = StubHandler.start(ferariEnvironment.getPort(), new StubHandler());
        refdata = StubHandler.start(refDataEnvironment.getPort(), new StubHandler());
        profile = StubHandler.start(profileEnvironment.getPort(), new StubHandler());

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

                .andExpect(jsonPath("$.order", notNullValue()))

                .andReturn();

        // Then
        final CreateOrderResponse response = mapper.readValue(result.getResponse().getContentAsString(), CreateOrderResponse.class);
        final Integer id = response.getOrder().getId();
        final Order order = orderRepository.findOne(id);
        assertThat(order).isNotNull();

        assertThat(order.getOrderDateTime()).isNotNull();
        assertThat(order.getCurrencyCode()).isEqualTo("XAF");
        assertThat(order.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getSubTotalAmount()).isEqualTo(new BigDecimal(12000).setScale(2));
        assertThat(order.getTotalFees()).isEqualTo(new BigDecimal(300).setScale(2));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal(12300).setScale(2));
        assertThat(order.getSiteId()).isEqualTo(request.getSiteId());
        assertThat(order.getLanguageCode()).isNull();
        assertThat(order.getCreationDateTime()).isNotNull();
        assertThat(order.getFreeCancellationDateTime()).isNotNull();

        
        final List<OrderLine> lines = orderLineRepository.findByOrder(order);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getUnitPrice()).isEqualTo(new BigDecimal(6000).setScale(2));
        assertThat(lines.get(0).getTotalPrice()).isEqualTo(new BigDecimal(12000).setScale(2));
        assertThat(lines.get(0).getQuantity()).isEqualTo(2);
        assertThat(lines.get(0).getBookingId()).isNull();
        assertThat(lines.get(0).getDescription()).isEqualTo(request.getOfferLines().get(0).getDescription());
        assertThat(lines.get(0).getOfferToken()).isEqualTo(request.getOfferLines().get(0).getToken());
        assertThat(lines.get(0).getMerchantId()).isEqualTo(request.getOfferLines().get(0).getMerchantId());
        assertThat(lines.get(0).getType()).isEqualTo(request.getOfferLines().get(0).getType());
        assertThat(lines.get(0).getFees()).isNull();

        assertThat(lines.get(1).getUnitPrice()).isEqualTo(new BigDecimal(300).setScale(2));
        assertThat(lines.get(1).getTotalPrice()).isEqualTo(new BigDecimal(300).setScale(2));
        assertThat(lines.get(1).getQuantity()).isEqualTo(1);
        assertThat(lines.get(1).getBookingId()).isNull();
        assertThat(lines.get(1).getDescription()).isNull();
        assertThat(lines.get(1).getOfferToken()).isNull();
        assertThat(lines.get(1).getMerchantId()).isNull();
        assertThat(lines.get(1).getType()).isEqualTo(OrderLineType.FEES);
        assertThat(lines.get(1).getFees()).isNotNull();
        assertThat(lines.get(1).getFees().getId()).isEqualTo(1);
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
                .andExpect(jsonPath("$.errors[0].code", is(OMSErrorCode.OFFER_MALFORMED_TOKEN.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(OMSErrorCode.OFFER_MALFORMED_TOKEN.getText())))
        ;

    }



    /* =========== CHECKOUT ============ */
    @Test
    public void shouldCheckoutOrderWithMobileMoney() throws Exception {
        final CheckoutOrderRequest request = createCheckoutOrderRequest();
        final String deviceUID = UUID.randomUUID().toString();

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

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(order.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(order.getLastName()).isEqualTo(request.getLastName());
        assertThat(order.getEmail()).isEqualTo(request.getEmail());
        assertThat(order.getDeviceUID()).isEqualTo(deviceUID);
        assertThat(order.getLanguageCode()).isEqualTo(request.getLanguageCode());
        assertThat(order.getMobileNumber()).isEqualTo("23799505678");
        assertThat(order.getMobileProvider()).isEqualTo("Orange");

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
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).getBookingId()).isEqualTo(1000);
        assertThat(lines.get(1).getBookingId()).isEqualTo(1001);
        assertThat(lines.get(2).getBookingId()).isNull();

        final List<Ticket> tickets = ticketRepository.findByOrder(order);
        assertThat(tickets).hasSize(3);

        assertThat(tickets.get(0).getOrderLine()).isEqualTo(lines.get(0));
        assertThat(tickets.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(tickets.get(0).getFirstName()).isEqualTo("John");
        assertThat(tickets.get(0).getLastName()).isEqualTo("Doe");
        assertThat(tickets.get(0).getPrintDateTime()).isNotNull();
        assertThat(tickets.get(0).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(0).getCancellationDateTime()).isNull();
        assertThat(tickets.get(0).getStatus()).isEqualTo(TicketStatus.NEW);

        assertThat(tickets.get(1).getOrderLine()).isEqualTo(lines.get(1));
        assertThat(tickets.get(1).getSequenceNumber()).isEqualTo(1);
        assertThat(tickets.get(1).getFirstName()).isEqualTo("John");
        assertThat(tickets.get(1).getLastName()).isEqualTo("Doe");
        assertThat(tickets.get(1).getPrintDateTime()).isNotNull();
        assertThat(tickets.get(1).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(1).getCancellationDateTime()).isNull();
        assertThat(tickets.get(1).getStatus()).isEqualTo(TicketStatus.NEW);

        assertThat(tickets.get(2).getOrderLine()).isEqualTo(lines.get(1));
        assertThat(tickets.get(2).getSequenceNumber()).isEqualTo(2);
        assertThat(tickets.get(2).getFirstName()).isEqualTo("Jane");
        assertThat(tickets.get(2).getLastName()).isEqualTo("Smith");
        assertThat(tickets.get(2).getPrintDateTime()).isNotNull();
        assertThat(tickets.get(2).getExpiryDateTime()).isNotNull();
        assertThat(tickets.get(2).getCancellationDateTime()).isNull();
        assertThat(tickets.get(2).getStatus()).isEqualTo(TicketStatus.NEW);

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
                .andExpect(jsonPath("$.order.subTotalAmount", is(6000d)))
                .andExpect(jsonPath("$.order.totalFees", is(300d)))
                .andExpect(jsonPath("$.order.totalAmount", is(6300d)))
                .andExpect(jsonPath("$.order.orderDateTime", notNullValue()))
                .andExpect(jsonPath("$.order.siteId", is(1)))
                .andExpect(jsonPath("$.order.deviceUID", is("1234-1234")))
                .andExpect(jsonPath("$.order.languageCode", is("fr")))

                .andExpect(jsonPath("$.order.lines.length()", is(1)))
                .andExpect(jsonPath("$.order.lines[0].bookingId", is(5678)))
                .andExpect(jsonPath("$.order.lines[0].type", is("CAR")))
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


    /* ===== CANCEL ======= */
    @Test
    public void shouldCancelOrder() throws Exception {
        final MobilePaymentDto mobile = new MobilePaymentDto();
        mobile.setProvider("MTN");
        mobile.setCountryCode("237");
        mobile.setNumber("99505678");
        final CancelOrderRequest request = new CancelOrderRequest();
        request.setMobilePayment(mobile);

        final MvcResult result = mockMvc
                .perform(post("/v1/orders/200/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andReturn()
        ;

        mapper.readValue(result.getResponse().getContentAsString(), CancelOrderResponse.class);

        final Order order = orderRepository.findOne(200);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationDateTime()).isNotNull();

        final Ticket ticket1 = ticketRepository.findOne(301);
        assertThat(ticket1.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        final Ticket ticket2 = ticketRepository.findOne(302);
        assertThat(ticket2.getStatus()).isEqualTo(TicketStatus.NEW);
    }

    /* ===== REFUND ======= */
    @Test
    public void shouldRefundOrder() throws Exception {

        final MvcResult result = mockMvc
                .perform(get("/v1/orders/400/refund"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andReturn()
                ;

        mapper.readValue(result.getResponse().getContentAsString(), CancelOrderResponse.class);

        final Order order = orderRepository.findOne(400);
        final Transaction tx = transactionRepository.findByOrderAndType(order, TransactionType.REFUND);

        assertThat(tx).isNotNull();
        assertThat(tx.getAmount()).isEqualTo(new BigDecimal(-10000).setScale(2));
        assertThat(tx.getCurrencyCode()).isEqualTo("XAF");
        assertThat(tx.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(tx.getGatewayTid()).isNotNull();
        assertThat(tx.getTransactionDateTime()).isNotNull();
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
        line.setType(OrderLineType.CAR);
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
