package io.tchepannou.enigma.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.enigma.ferari.client.CarOfferToken;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.Sex;
import io.tchepannou.enigma.oms.client.dto.CustomerDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import io.tchepannou.enigma.refdata.client.exception.ErrorCode;
import io.tchepannou.enigma.tontine.client.dto.MobilePaymentInfoDto;
import io.tchepannou.enigma.tontine.client.dto.PaymentInfoDto;
import org.apache.commons.lang.time.DateUtils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;
import support.stub.HandlerStub;
import support.stub.StubSupport;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
public class OrderControllerIT extends StubSupport {
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

    private DateFormat dateFormat;

    @Value("${enigma.service.tontine.port}")
    private int tontinePort;

    private Server tontine;
    private HandlerStub tontineHanderStub;

    @Value("${enigma.service.ferari.port}")
    private int ferariPort;

    private Server ferari;
    private HandlerStub ferariHanderStub;

    @Before
    public void setUp() throws Exception{
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        this.dateFormat = DateHelper.createDateFormat();

        ferariHanderStub = new HandlerStub();
        ferari = startServer(ferariPort, ferariHanderStub);

        tontineHanderStub = new HandlerStub();
        tontine = startServer(tontinePort, tontineHanderStub);
    }

    @After
    public void tearDown() throws Exception {
        stopServers(ferari, tontine);
    }


    /* =========== CREATE ============ */
    @Test
    public void shouldCreateOrder() throws Exception {
        final CreateOrderRequest request = createCreateOrderRequest();

        final MvcResult result = mockMvc
                .perform(
                        post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
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
        assertThat(order.getMerchantId()).isEqualTo(1);
        assertThat(order.getPaymentId()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal(12000).setScale(2));

        final List<Traveller> travellers = travellerRepository.findByOrder(order);
        assertThat(travellers).hasSize(2);
        assertThat(travellers.get(0).getFirstName()).isEqualTo("John");
        assertThat(travellers.get(0).getLastName()).isEqualTo("Doe");
        assertThat(travellers.get(0).getSex()).isEqualTo(Sex.M);
        assertThat(travellers.get(1).getFirstName()).isEqualTo("Jane");
        assertThat(travellers.get(1).getLastName()).isEqualTo("Smith");
        assertThat(travellers.get(1).getSex()).isEqualTo(Sex.F);

        final List<OrderLine> lines = orderLineRepository.findByOrder(order);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getAmount()).isEqualTo(new BigDecimal(12000).setScale(2));
        assertThat(lines.get(0).getBookingId()).isNull();
        assertThat(lines.get(0).getDescription()).isEqualTo(request.getOfferLines().get(0).getDescription());
        assertThat(lines.get(0).getOfferToken()).isEqualTo(request.getOfferLines().get(0).getToken());
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
                .andExpect(jsonPath("$.errors[0].code", is(ErrorCode.MALFORMED_OFFER_TOKEN.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(ErrorCode.MALFORMED_OFFER_TOKEN.getText())))
        ;

    }

    @Test
    public void shouldNotCreateOrderWithNoMerchantId() throws Exception {
        final CreateOrderRequest request = createCreateOrderRequest();
        request.setMerchantId(null);

        mockMvc
                .perform(
                        post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())

                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(ErrorCode.VALIDATION_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is("may not be null")))
                .andExpect(jsonPath("$.errors[0].field", is("merchantId")))
        ;
    }

    @Test
    public void shouldNotCreateOrderWithNoPaymentInfo() throws Exception {
        final CreateOrderRequest request = createCreateOrderRequest();
        request.setPaymentInfo(null);

        mockMvc
                .perform(
                        post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())

                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(ErrorCode.VALIDATION_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is("may not be null")))
                .andExpect(jsonPath("$.errors[0].field", is("paymentInfo")))
        ;

    }

    /* =========== CHECKOUT ============ */
    @Test
    public void shouldCheckoutOrder() throws Exception {
        mockMvc
                .perform(get("/v1/orders/100/checkout"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        // Then
        final Order order = orderRepository.findOne(100);
        assertThat(order).isNotNull();

        assertThat(order.getPaymentId()).isEqualTo(100);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        final List<OrderLine> lines = orderLineRepository.findByOrder(order);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getAmount()).isEqualTo(new BigDecimal(6000).setScale(2));
        assertThat(lines.get(0).getBookingId()).isEqualTo(1000);
        assertThat(lines.get(1).getAmount()).isEqualTo(new BigDecimal(6000).setScale(2));
        assertThat(lines.get(1).getBookingId()).isEqualTo(1001);
    }

    @Test
    public void shouldNotCheckoutOrderWhenPaymentFailed() throws Exception {
        tontineHanderStub.setStatus(HttpStatus.CONFLICT.value());
        mockMvc
                .perform(get("/v1/orders/100/checkout"))

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isConflict())
                .andReturn()
        ;

        // Then
        final Order order = orderRepository.findOne(100);
        assertThat(order).isNotNull();

        assertThat(order.getPaymentId()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }


    private CreateOrderRequest createCreateOrderRequest() throws Exception {
        final Date departureDate = dateFormat.parse("2030-04-15T05:00:00+0000");
        final CarOfferToken carOfferToken = createCarOfferToken(100, 1000 , departureDate, 2);

        final List<OfferLineDto> lines = new ArrayList();
        final OfferLineDto line = new OfferLineDto();
        line.setToken(carOfferToken.toString());
        line.setType(OfferType.CAR);
        line.setDescription("description #1");

        lines.add(line);

        final CustomerDto customer = new CustomerDto();
        customer.setId(1);

        final List<TravellerDto> travellers = Arrays.asList(
                createTraveller("John", "Doe", Sex.M),
                createTraveller("Jane", "Smith", Sex.F)
        );

        final MobilePaymentInfoDto mobile = new MobilePaymentInfoDto();
        mobile.setCountryCode("237");
        mobile.setNumber("99505678");

        final PaymentInfoDto payment = new PaymentInfoDto();
        payment.setMobile(mobile);

        final CreateOrderRequest request = new CreateOrderRequest();
        request.setOfferLines(lines);
        request.setMerchantId(1);
        request.setCustomer(customer);
        request.setTravellers(travellers);
        request.setPaymentInfo(payment);

        return request;
    }

    private CarOfferToken createCarOfferToken(Integer productId, Integer priceId, Date departureDate, int travellerCount){
        final CarOfferToken token = new CarOfferToken();
        token.setProductId(productId);
        token.setPriceId(priceId);
        token.setAmount(BigDecimal.valueOf(6000*travellerCount));
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(departureDate);
        token.setArrivalDateTime(DateUtils.addHours(departureDate, 2));
        token.setOriginId(2370001);
        token.setDestinationId(2370002);
        token.setExpiryDateTime(new Date(10*System.currentTimeMillis()));
        return token;
    }

    private TravellerDto createTraveller(String firstName, String lastName, Sex sex){
        TravellerDto dto = new TravellerDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setSex(sex);
        return dto;
    }

}
