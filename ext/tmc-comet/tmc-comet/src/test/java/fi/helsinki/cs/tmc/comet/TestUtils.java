package fi.helsinki.cs.tmc.comet;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.websocket.client.WebSocketTransport;
import static org.junit.Assert.*;

public class TestUtils {
    public static final String TEST_USER = "test_user";
    public static final String TEST_PASSWORD = "test_password";
    public static final String TEST_SESSION_ID = "test_session_id";
    
    public static String getJettyPort() {
        return System.getProperty("jetty.port");
    }
    
    public static String getBackendKey() {
        return System.getProperty("fi.helsinki.cs.tmc.comet.backendKey.test");
    }

    public static String getCometUrl() {
        return "http://localhost:" + getJettyPort() + "/comet";
    }
    
    public static String getSynchronousPublishUrl() {
        return "http://localhost:" + getJettyPort() + "/synchronous/publish";
    }
    
    public static String getAdminMsgChannel() {
        return "/broadcast/tmc/global/admin-msg";
    }
    
    public static String getAuthServerBaseUrl() {
        return "http://localhost:" + getAuthServerPort() + "/foo";
    }
    public static String getAnotherAuthServerBaseUrl() {
        return "http://localhost:" + getAuthServerPort() + "/bar";
    }
    
    public static String getJavascriptBaseUrl() {
        return "http://localhost:" + getJettyPort() + "/js";
    }
    
    public static int getAuthServerPort() {
        return 8089;
    }
    

    public static BayeuxClient connectAsFrontend() {
        return connectAsFrontend(getFrontendAuthFields());
    }
    
    public static BayeuxClient connectAsFrontend(Map<String, Object> authFields) {
        BayeuxClient client = createClient();
        client.addExtension(getAuthenticationExtension(authFields));
        doHandshake(client);
        return client;
    }
    
    public static BayeuxClient connectAsFrontend(String username, String password) {
        return connectAsFrontend(username, password, TestUtils.getAuthServerBaseUrl());
    }
    
    public static BayeuxClient connectAsFrontend(String username, String password, String authServerBaseUrl) {
        Map<String, Object> authFields = ImmutableMap.of(
                "username", (Object)username,
                "password", password,
                "serverBaseUrl", authServerBaseUrl
                );
        
        BayeuxClient client = createClient();
        client.addExtension(getAuthenticationExtension(authFields));
        doHandshake(client);
        return client;
    }
    
    public static BayeuxClient connectAsBackend() {
        BayeuxClient client = createClient();
        client.addExtension(getAuthenticationExtension(getBackendAuthFields()));
        doHandshake(client);
        return client;
    }
    
    public static BayeuxClient createClient() {
        Map<String, Object> transportOpts = new HashMap<String, Object>();
        ClientTransport transport = new WebSocketTransport.Factory().newClientTransport(getCometUrl(), transportOpts);
        return new BayeuxClient(getCometUrl(), transport);
    }

    private static void doHandshake(BayeuxClient client) {
        client.handshake();
        if (!client.waitFor(3000, BayeuxClient.State.CONNECTED)) {
            fail("Handshake failed.");
        }
    }
    
    public static ClientSession.Extension getAuthenticationExtension(final Map<String, Object> fields) {
        return new ClientSession.Extension() {
            public boolean rcv(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean rcvMeta(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean send(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean sendMeta(ClientSession session, Message.Mutable message) {
                message.getExt(true).put("authentication", fields);
                return true;
            }
        };
    }
    
    private static Map<String, Object> getFrontendAuthFields() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("username", TEST_USER);
        result.put("password", TEST_PASSWORD);
        result.put("serverBaseUrl", getAuthServerBaseUrl());
        return result;
    }
    
    private static Map<String, Object> getBackendAuthFields() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("backendKey", getBackendKey());
        result.put("serverBaseUrl", getAuthServerBaseUrl());
        return result;
    }
    
    public static void addEnqueueingListener(final BlockingQueue<Message> queue, ClientSessionChannel channel) {
        channel.addListener(new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                queue.add(message);
            }
        });
    }
    
    public static void addEnqueueingSubscription(final BlockingQueue<Message> queue, ClientSessionChannel channel) {
        channel.subscribe(new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                queue.add(message);
            }
        });
    }
    
    public static void addAndCheckEnqueueingSubscription(final BlockingQueue<Message> queue, ClientSessionChannel channel) throws InterruptedException {
        ClientSessionChannel subscriptionChannel = channel.getSession().getChannel(Channel.META_SUBSCRIBE);
        BlockingQueue<Message> internalQueue = new LinkedBlockingDeque<Message>();
        addEnqueueingListener(internalQueue, subscriptionChannel);
        
        addEnqueueingSubscription(queue, channel);
        
        Message msg = internalQueue.poll(3, TimeUnit.SECONDS);
        if (msg == null) {
            fail("No subscription success notification");
        }
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertTrue(msg.isSuccessful());
    }
}
