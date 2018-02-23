package fi.helsinki.cs.tmc.comet;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;
import static org.junit.Assert.*;
import us.monoid.web.Resty;
import static us.monoid.web.Resty.*;

public class PublishServletIT extends AbstractTmcCometTest {
    @Test
    public void testPublishServlet() throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        
        BayeuxClient frontClient = TestUtils.connectAsFrontend();
        
        String channel = "/broadcast/tmc/global/admin-msg";
        TestUtils.addAndCheckEnqueueingSubscription(queue, frontClient.getChannel(channel));
        
        String data =
                "channel=" + Resty.enc(channel) + "&" +
                "serverBaseUrl=" + Resty.enc(TestUtils.getAuthServerBaseUrl()) + "&" +
                "backendKey=" + Resty.enc(TestUtils.getBackendKey()) + "&" +
                "data=" + Resty.enc(JSON.toString("Hello World!"));
        String response = new Resty().text(TestUtils.getSynchronousPublishUrl(), form(data)).toString();
        
        assertEquals("OK", response);
        
        Message msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(channel), msg.getChannelId());
        assertEquals("Hello World!", msg.getData());
    }
    
    @Test
    public void testPublishServletWithInvalidBackendKey() throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        
        BayeuxClient frontClient = TestUtils.connectAsFrontend();
        
        String channel = "/broadcast/tmc/global/admin-msg";
        TestUtils.addAndCheckEnqueueingSubscription(queue, frontClient.getChannel(channel));
        
        String data =
                "channel=" + Resty.enc(channel) + "&" +
                "serverBaseUrl=" + Resty.enc(TestUtils.getAuthServerBaseUrl()) + "&" +
                "backendKey=" + Resty.enc("incorrect key") + "&" +
                "data=" + Resty.enc(JSON.toString("Hello World!"));
        try {
            new Resty().text(TestUtils.getSynchronousPublishUrl(), form(data));
            fail("Expected an error");
        } catch (IOException e) {
            assertTrue("Expected a 403 error, got " + e.getMessage(), e.getMessage().contains("403"));
        }
        
        assertNull(queue.poll(1, TimeUnit.SECONDS));
    }
}
