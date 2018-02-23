package fi.helsinki.cs.tmc.comet;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.client.BayeuxClient;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthenticateBySessionIdIT extends AbstractTmcCometTest {

    @Test
    public void testValidSessionId() {
        BayeuxClient client = TestUtils.createClient();
        
        HashMap<String, Object> authFields = new HashMap<String, Object>();
        authFields.put("username", TestUtils.TEST_USER);
        authFields.put("sessionId", TestUtils.TEST_SESSION_ID);
        authFields.put("serverBaseUrl", TestUtils.getAuthServerBaseUrl());
        client.addExtension(TestUtils.getAuthenticationExtension(authFields));
        
        client.handshake();
        if (!client.waitFor(3000, BayeuxClient.State.CONNECTED)) {
            fail("Handshake failed.");
        }
    }
    
    @Test
    public void testInvalidSessionId() throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        BayeuxClient client = TestUtils.createClient();
        
        HashMap<String, Object> authFields = new HashMap<String, Object>();
        authFields.put("username", TestUtils.TEST_USER);
        authFields.put("sessionId", "invalid_session_id");
        authFields.put("serverBaseUrl", TestUtils.getAuthServerBaseUrl());
        client.addExtension(TestUtils.getAuthenticationExtension(authFields));
        
        TestUtils.addEnqueueingListener(queue, client.getChannel(Channel.META_HANDSHAKE));
        
        client.handshake();
        
        Message msg = notNull("No handshake response.", queue.poll(3, TimeUnit.SECONDS));
        assertFalse(msg.isSuccessful());
        assertTrue(msg.get(Message.ERROR_FIELD).toString().startsWith("403:"));
    }
}
