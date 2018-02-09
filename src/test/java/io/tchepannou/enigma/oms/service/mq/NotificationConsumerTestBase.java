package io.tchepannou.enigma.oms.service.mq;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.tchepannou.core.test.jetty.StubHandler;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Value;

public abstract class NotificationConsumerTestBase {

    @Value("${enigma.service.ferari.port}")
    private int ferariPort;

    @Value("${enigma.service.profile.port}")
    private int profilePort;

    @Value("${enigma.service.refdata.port}")
    private int refdataPort;

    protected GreenMail mail;
    private Server ferari;
    private Server refdata;
    private Server profile;


    @Before
    public void setUp() throws Exception{
        mail = new GreenMail(ServerSetupTest.SMTP);
        mail.start();

        ferari = StubHandler.start(ferariPort, new StubHandler());
        refdata = StubHandler.start(refdataPort, new StubHandler());
        profile = StubHandler.start(profilePort, new StubHandler());
    }

    @After
    public void tearDown() throws Exception {
        StubHandler.stop(ferari, refdata, profile);
        mail.stop();
    }
}
