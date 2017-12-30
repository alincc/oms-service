package io.tchepannou.enigma.oms.controller.support.stub;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {"stub"})
public abstract class StubSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(StubSupport.class);

    protected HttpServletRequest request;

    public Server startServer(final int port, final Handler handler) throws Exception{
        LOGGER.info("Starting HTTP server on port {}", port);

        final Server server = new Server(port);
        server.setHandler(handler);
        server.start();

        request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("guid", "12345678901234567890123456789012")
        });

        return server;
    }

    public void stopServers(Server...servers) {
        for (Server s : servers){
            try{
                s.stop();
            } catch (Exception e){
                LOGGER.error("Unable to stop server. {}", s, e);
            }
        }
    }

}
