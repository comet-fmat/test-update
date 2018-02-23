package fi.helsinki.cs.tmc.comet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;

public class AbstractTmcCometTest {
    protected StubAuthServer authServer;
    
    @Before
    public void setUpAuthServer() throws Exception {
        authServer = new StubAuthServer(TestUtils.getAuthServerPort());
        authServer.addUser(TestUtils.TEST_USER, TestUtils.TEST_PASSWORD);
        authServer.addSession("test_user", TestUtils.TEST_SESSION_ID);
    }
    
    @After
    public void cleanAuthServer() {
        authServer.close();
        authServer = null;
    }
    
    protected <T> T notNull(T x) {
        assertNotNull(x);
        return x;
    }
    
    protected <T> T notNull(String msg, T x) {
        assertNotNull(msg, x);
        return x;
    }
    
    protected void testSimplePubSub(String channel) throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        
        BayeuxClient frontClient = TestUtils.connectAsFrontend();
        BayeuxClient backClient = TestUtils.connectAsBackend();
        Message msg;
        
        TestUtils.addEnqueueingListener(queue, frontClient.getChannel(Channel.META_SUBSCRIBE));
        TestUtils.addEnqueueingListener(queue, frontClient.getChannel(Channel.META_UNSUBSCRIBE));
        
        TestUtils.addEnqueueingSubscription(queue, frontClient.getChannel(channel));
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertTrue("Subscription failed", msg.isSuccessful());
        
        backClient.getChannel(channel).publish("Hello World!");
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(channel), msg.getChannelId());
        assertEquals("Hello World!", msg.getData());
        
        frontClient.getChannel(channel).unsubscribe();
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_UNSUBSCRIBE), msg.getChannelId());
        assertTrue("Unsubscription failed", msg.isSuccessful());
        
        frontClient.disconnect();
        frontClient.waitFor(3000, BayeuxClient.State.DISCONNECTED);
        backClient.disconnect();
        backClient.waitFor(3000, BayeuxClient.State.DISCONNECTED);
    }
    
    protected void testCannotCreateAsBackend(String channel) throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        Message msg;
        
        BayeuxClient backClient = TestUtils.connectAsBackend();
        
        TestUtils.addEnqueueingListener(queue, backClient.getChannel(Channel.META_SUBSCRIBE));
        
        backClient.getChannel(channel).subscribe(emptyMessageListener);
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertFalse(msg.isSuccessful());
        assertTrue(msg.get(Message.ERROR_FIELD).toString().startsWith("403:"));
        assertTrue(msg.get(Message.ERROR_FIELD).toString().endsWith(":create_denied"));
    }
    
    protected void testCannotCreateAsFrontend(String channel) throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        Message msg;
        
        BayeuxClient frontClient = TestUtils.connectAsFrontend();
        
        TestUtils.addEnqueueingListener(queue, frontClient.getChannel(Channel.META_SUBSCRIBE));
        
        frontClient.getChannel(channel).subscribe(emptyMessageListener);
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertFalse(msg.isSuccessful());
        assertTrue(msg.get(Message.ERROR_FIELD).toString().startsWith("403:"));
        assertTrue(msg.get(Message.ERROR_FIELD).toString().endsWith(":create_denied"));
    }
    
    protected void testCannotSub(String channel) throws Exception {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        Message msg;
        
        BayeuxClient frontClient = TestUtils.connectAsFrontend();
        BayeuxClient backClient = TestUtils.connectAsBackend();
        
        TestUtils.addEnqueueingListener(queue, frontClient.getChannel(Channel.META_SUBSCRIBE));
        TestUtils.addEnqueueingListener(queue, backClient.getChannel(Channel.META_SUBSCRIBE));
        
        frontClient.getChannel(channel).subscribe(emptyMessageListener);
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertFalse(msg.isSuccessful());
        assertTrue(msg.get(Message.ERROR_FIELD).toString().startsWith("403:"));
        assertTrue(msg.get(Message.ERROR_FIELD).toString().endsWith(":create_denied"));
        
        // Ensure the channel is created
        backClient.getChannel(channel).subscribe(emptyMessageListener);
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertTrue(msg.isSuccessful());
        
        // TODO: why is reconnecting necessary here? We get "No message received" otherwise.
        frontClient.disconnect();
        frontClient = TestUtils.connectAsFrontend();
        TestUtils.addEnqueueingListener(queue, frontClient.getChannel(Channel.META_SUBSCRIBE));
        
        frontClient.getChannel(channel).subscribe(emptyMessageListener);
        msg = notNull("No message received", queue.poll(3, TimeUnit.SECONDS));
        assertEquals(new ChannelId(Channel.META_SUBSCRIBE), msg.getChannelId());
        assertFalse(msg.isSuccessful());
        assertTrue(msg.get(Message.ERROR_FIELD).toString().startsWith("403:"));
        assertTrue(msg.get(Message.ERROR_FIELD).toString().endsWith(":subscribe_denied"));
    }
    
    protected ClientSessionChannel.MessageListener emptyMessageListener = new ClientSessionChannel.MessageListener() {
        public void onMessage(ClientSessionChannel channel, Message message) {
        }
    };
}
