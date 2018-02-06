package io.tchepannou.enigma.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.tchepannou.core.test.jetty.StubHandler;
import io.tchepannou.enigma.ferari.client.CarOfferToken;
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
import io.tchepannou.enigma.oms.domain.Account;
import io.tchepannou.enigma.oms.domain.AccountType;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.TransactionType;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.AccountRepository;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
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

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private DateFormat dateFormat;




    @Value("${enigma.service.ferari.port}")
    private int ferariPort;

    @Value("${enigma.service.profile.port}")
    private int profilePort;

    @Value("${enigma.service.refdata.port}")
    private int refdataPort;

    @Value("${spring.mail.port}")
    private int smtpPort;

    private Server ferari;
    private StubHandler ferariHanderStub;

    private Server refdata;
    private Server profile;


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

        mockMvc
                .perform(
                        post("/v1/orders/100/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )


                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andReturn();

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
    }

//    @Test
//    public void shouldNotCheckoutOrderWhenPaymentFailed() throws Exception {
//        tontineHanderStub.setStatus(HttpStatus.CONFLICT.value());
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


    /* ==== TRANSACTIONS ==== */
    @Test
    public void shouldRecordTransactionOnCheckout() throws Exception {
        final CheckoutOrderRequest request = createCheckoutOrderRequest();

        mockMvc
                .perform(
                        post("/v1/orders/110/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )


                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andReturn();

        // Then
        final Order order = orderRepository.findOne(110);

        final Account account1101 = accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, 1101);
        assertThat(account1101.getBalance()).isEqualTo(new BigDecimal(5300.00).setScale(2));
        assertThat(account1101.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account1101.getReferenceId()).isEqualTo(1101);
        assertThat(account1101.getType()).isEqualTo(AccountType.MERCHANT);
        assertThat(account1101.getSiteId()).isEqualTo(11);

        final List<Transaction> tx1101 = transactionRepository.findByAccount(account1101);
        assertThat(tx1101).hasSize(1);
        assertThat(tx1101.get(0).getAmount()).isEqualTo(new BigDecimal(6000.00).setScale(2));
        assertThat(tx1101.get(0).getFees()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx1101.get(0).getNet()).isEqualTo(new BigDecimal(5300.00).setScale(2));
        assertThat(tx1101.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1101.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1101.get(0).getReferenceId()).isEqualTo(1000);


        final Account account1102 = accountRepository.findByTypeAndReferenceId(AccountType.MERCHANT, 1102);
        assertThat(account1102.getBalance()).isEqualTo(new BigDecimal((10000.00 + 10700.00)).setScale(2));
        assertThat(account1102.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account1102.getReferenceId()).isEqualTo(1102);
        assertThat(account1102.getType()).isEqualTo(AccountType.MERCHANT);
        assertThat(account1102.getSiteId()).isEqualTo(11);

        final List<Transaction> tx1102 = transactionRepository.findByAccount(account1102);
        assertThat(tx1102).hasSize(1);
        assertThat(tx1102.get(0).getAmount()).isEqualTo(new BigDecimal(12000.00).setScale(2));
        assertThat(tx1102.get(0).getFees()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx1102.get(0).getNet()).isEqualTo(new BigDecimal(10700.00).setScale(2));
        assertThat(tx1102.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx1102.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx1102.get(0).getReferenceId()).isEqualTo(1001);

        final Account account11 = accountRepository.findByTypeAndReferenceId(AccountType.SITE, 11);
        assertThat(account11.getBalance()).isEqualTo(new BigDecimal((2000)).setScale(2));
        assertThat(account11.getCurrencyCode()).isEqualTo("XAF");
        assertThat(account11.getReferenceId()).isEqualTo(11);
        assertThat(account11.getType()).isEqualTo(AccountType.SITE);
        assertThat(account11.getSiteId()).isEqualTo(11);

        final List<Transaction> tx11 = transactionRepository.findByAccount(account11);
        assertThat(tx11).hasSize(2);

        assertThat(tx11.get(0).getAmount()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx11.get(0).getFees()).isEqualTo(new BigDecimal(0.00).setScale(2));
        assertThat(tx11.get(0).getNet()).isEqualTo(new BigDecimal(700.00).setScale(2));
        assertThat(tx11.get(0).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx11.get(0).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx11.get(0).getReferenceId()).isEqualTo(1000);

        assertThat(tx11.get(1).getAmount()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx11.get(1).getFees()).isEqualTo(new BigDecimal(0.00).setScale(2));
        assertThat(tx11.get(1).getNet()).isEqualTo(new BigDecimal(1300.00).setScale(2));
        assertThat(tx11.get(1).getTransactionDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(tx11.get(1).getType()).isEqualTo(TransactionType.BOOKING);
        assertThat(tx11.get(1).getReferenceId()).isEqualTo(1001);
    }


    /* ==== SEND EMAIL CUSTOMER ==== */
    @Test
    public void shouldSendEmailToCustomer() throws Exception {
        GreenMail mail = new GreenMail(ServerSetupTest.SMTP);
        mail.start();
        try{

            // Given
            mockMvc
                    .perform(get("/v1/orders/300/notify/customer"))

                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());

            // When
            mail.waitForIncomingEmail(5000, 1);
            final MimeMessage[] msgs = mail.getReceivedMessages();

            // Then
            assertThat(msgs).hasSize(1);
            assertThat(Arrays.asList(msgs[0].getRecipients(Message.RecipientType.TO))).containsExactly(new InternetAddress("ray@gmail.com"));
            assertThat(Arrays.asList(msgs[0].getSubject())).contains("[Enigma-Voyages] Travel Confirmation - Order #300");
        }finally {
            mail.stop();
        }
    }

    @Test
    public void shouldNotSendEmailToCustomerForInvalidOrder() throws Exception {

        // Given
        mockMvc
                .perform(get("/v1/orders/999/notify/customer"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

    }

    @Test
    public void shouldNotSendEmailToCustomerIfNoEmail() throws Exception {

        // Given
        mockMvc
                .perform(get("/v1/orders/150/notify/customer"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

    }


    /* ==== SEND EMAIL CUSTOMER ==== */
    @Test
    public void shouldSendEmailToMerchant() throws Exception {
        GreenMail mail = new GreenMail(ServerSetupTest.SMTP);
        mail.start();
        try{

            // Given
            mockMvc
                    .perform(get("/v1/orders/300/notify/merchants"))

                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());

            // When
            mail.waitForIncomingEmail(5000, 1);
            final MimeMessage[] msgs = mail.getReceivedMessages();

            // Then
            assertThat(msgs).hasSize(2);

            assertThat(Arrays.asList(msgs[0].getRecipients(Message.RecipientType.TO))).containsExactly(new InternetAddress("merchant2001@gmail.com"));
            assertThat(Arrays.asList(msgs[0].getSubject())).contains("[Enigma-Voyages] Travel Confirmation - Order #300");

            assertThat(Arrays.asList(msgs[1].getRecipients(Message.RecipientType.TO))).containsExactly(new InternetAddress("merchant2002@gmail.com"));
            assertThat(Arrays.asList(msgs[1].getSubject())).contains("[Enigma-Voyages] Travel Confirmation - Order #300");
        }finally {
            mail.stop();
        }
    }

    @Test
    public void shouldNotSendEmailToMerchantForInvalidOrder() throws Exception {

        // Given
        mockMvc
                .perform(get("/v1/orders/999/notify/merchants"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

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

        return request;
    }

    private CreateOrderRequest createCreateOrderRequest(int travellerCount) throws Exception {
        final Date departureDate = dateFormat.parse("2030-04-15T05:00:00+0000");
        final CarOfferToken carOfferToken = createCarOfferToken(100, 1000 , departureDate, travellerCount);

        final List<OfferLineDto> lines = new ArrayList();
        final OfferLineDto line = new OfferLineDto();
        line.setToken(carOfferToken.toString());
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

    private CarOfferToken createCarOfferToken(Integer productId, Integer priceId, Date departureDate, int travellerCount){
        final CarOfferToken token = new CarOfferToken();
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
