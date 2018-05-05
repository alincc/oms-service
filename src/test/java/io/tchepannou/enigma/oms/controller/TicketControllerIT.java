package io.tchepannou.enigma.oms.controller;

import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.TicketStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@Sql({"classpath:/sql/clean.sql", "classpath:/sql/TicketController.sql"})
@ActiveProfiles(profiles = {"stub"})
@SuppressWarnings("CPD-START")
public class TicketControllerIT {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() throws Exception{
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }


    /* =========== CREATE ============ */
    @Test
    public void shouldReturnTicket() throws Exception {
        mockMvc
                .perform(
                        get("/v1/tickets/100")
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.ticket.id", is(100)))
                .andExpect(jsonPath("$.ticket.orderId", is(100)))
                .andExpect(jsonPath("$.ticket.orderLineId", is(100)))
                .andExpect(jsonPath("$.ticket.firstName", is("Ray")))
                .andExpect(jsonPath("$.ticket.lastName", is("Sponsible")))
                .andExpect(jsonPath("$.ticket.productId", is(1001)))
                .andExpect(jsonPath("$.ticket.merchantId", is(1)))
                .andExpect(jsonPath("$.ticket.sequenceNumber", is(0)))
                .andExpect(jsonPath("$.ticket.originId", is(2370001)))
                .andExpect(jsonPath("$.ticket.destinationId", is(2370002)))
                .andExpect(jsonPath("$.ticket.departureDateTime", notNullValue()))
                .andExpect(jsonPath("$.ticket.status", is(TicketStatus.NEW.name())))
                .andExpect(jsonPath("$.ticket.token", is("100;100;100;0;1001;100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060;Ray;Sponsible")))

                .andReturn();
    }

    @Test
    public void shouldReturn404ForInvalidTicket() throws Exception {
        mockMvc
                .perform(
                        get("/v1/tickets/999999")
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(OMSErrorCode.TICKET_NOT_FOUND.getCode())))
                .andExpect(jsonPath("$.errors[0].text", is(OMSErrorCode.TICKET_NOT_FOUND.getText())))
        ;

    }

    /* =========== FIND BY CUSTOMER ============ */
    @Test
    public void shouldReturnTicketForCustomer() throws Exception {
        mockMvc
                .perform(
                        get("/v1/tickets/users/11")
                )

                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.tickets.length()", is(3)))
                .andExpect(jsonPath("$.tickets[0].id", is(200)))
                .andExpect(jsonPath("$.tickets[1].id", is(201)))
                .andExpect(jsonPath("$.tickets[2].id", is(210)))

                .andReturn();
    }
}
