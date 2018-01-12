package io.tchepannou.enigma.oms.service;

import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.controller.support.stub.HandlerStub;
import io.tchepannou.enigma.oms.controller.support.stub.StubSupport;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@Sql({"classpath:/sql/clean.sql", "classpath:/sql/OrderExpirerService.sql"})
@ActiveProfiles(profiles = {"stub"})
public class OrderExpirerServiceIT extends StubSupport {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderExpirerService service;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${enigma.service.ferari.port}")
    private int ferariPort;

    private Server ferari;
    private HandlerStub ferariHanderStub;


    @Before
    public void setUp() throws Exception{
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        ferariHanderStub = new HandlerStub();
        ferari = startServer(ferariPort, ferariHanderStub);
    }

    @After
    public void tearDown() throws Exception {
        stopServers(ferari);
    }


    @Test
    public void run() throws Exception {
        service.run();
        Thread.sleep(2000);

        final Order order100 = orderRepository.findOne(100);
        assertThat(order100.getStatus()).isEqualTo(OrderStatus.PENDING);

        final Order order103 = orderRepository.findOne(103);
        assertThat(order103.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        final Order order104 = orderRepository.findOne(104);
        assertThat(order104.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

}
