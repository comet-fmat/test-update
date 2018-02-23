package fi.helsinki.cs.tmc.comet;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PagePresenceIT extends AbstractTmcCometTest {
    private static final String USER1 = "user1";
    private static final String PASSWORD1 = "password1";
    private static final String USER2 = "user2";
    private static final String PASSWORD2 = "password2";
    
    @Before
    public void setUp() {
        authServer.addUser(USER1, PASSWORD1);
        authServer.addUser(USER2, PASSWORD2);
    }
    
    @Test
    public void testNotificationOnSubscription() throws Exception {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        
        BayeuxClient client1 = TestUtils.connectAsFrontend(USER1, PASSWORD1);
        ClientSessionChannel channel1 = client1.getChannel("/broadcast/page-presence/lets/subscribe/here");
        TestUtils.addEnqueueingSubscription(queue1, channel1);
        
        Message msg;
        msg = notNull("No message received", queue1.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER1);
        
        BayeuxClient client2 = TestUtils.connectAsFrontend(USER2, PASSWORD2);
        ClientSessionChannel channel2 = client2.getChannel("/broadcast/page-presence/lets/subscribe/here");
        TestUtils.addEnqueueingSubscription(queue2, channel2);
        
        msg = notNull("No message received", queue1.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER1);
        assertHasUser(msg, USER2);
        
        msg = notNull("No message received", queue2.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER1);
        assertHasUser(msg, USER2);
    }
    
    @Test
    public void testNotificationOnUnsubscribe() throws Exception {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        
        BayeuxClient client1 = TestUtils.connectAsFrontend(USER1, PASSWORD1);
        ClientSessionChannel channel1 = client1.getChannel("/broadcast/page-presence/lets/unsubscribe");
        TestUtils.addEnqueueingSubscription(queue1, channel1);
        
        assertNotNull(queue1.poll(3, TimeUnit.SECONDS));
        
        BayeuxClient client2 = TestUtils.connectAsFrontend(USER2, PASSWORD2);
        ClientSessionChannel channel2 = client2.getChannel("/broadcast/page-presence/lets/unsubscribe");
        TestUtils.addEnqueueingSubscription(queue2, channel2);
        
        assertNotNull(queue1.poll(3, TimeUnit.SECONDS));
        assertNotNull(queue2.poll(3, TimeUnit.SECONDS));
        
        channel1.unsubscribe();
        Message msg = notNull(queue2.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER2);
        assertDoesntHaveUser(msg, USER1);
        
        Thread.sleep(100);
        assertTrue(queue1.isEmpty());
    }
    
    @Test
    public void testSeparateChannels() throws Exception {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        
        BayeuxClient client1 = TestUtils.connectAsFrontend(USER1, PASSWORD1);
        ClientSessionChannel channel1 = client1.getChannel("/broadcast/page-presence/page1");
        TestUtils.addEnqueueingSubscription(queue1, channel1);
        
        Message msg;
        msg = notNull(queue1.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER1);
        assertDoesntHaveUser(msg, USER2);
        
        BayeuxClient client2 = TestUtils.connectAsFrontend(USER2, PASSWORD2);
        ClientSessionChannel channel2 = client2.getChannel("/broadcast/page-presence/page2");
        TestUtils.addEnqueueingSubscription(queue2, channel2);
        
        msg = notNull(queue2.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER2);
        assertDoesntHaveUser(msg, USER1);
        
        channel2.unsubscribe();
        
        Thread.sleep(100);
        assertTrue(queue1.isEmpty());
        assertTrue(queue2.isEmpty());
    }
    
    @Test
    public void testSeparateBaseUrls() throws Exception {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        
        BayeuxClient client1 = TestUtils.connectAsFrontend(USER1, PASSWORD1, TestUtils.getAuthServerBaseUrl());
        ClientSessionChannel channel1 = client1.getChannel("/broadcast/page-presence/my/page");
        TestUtils.addEnqueueingSubscription(queue1, channel1);
        
        Message msg;
        msg = notNull(queue1.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER1);
        assertDoesntHaveUser(msg, USER2);
        
        BayeuxClient client2 = TestUtils.connectAsFrontend(USER2, PASSWORD2, TestUtils.getAnotherAuthServerBaseUrl());
        ClientSessionChannel channel2 = client2.getChannel("/broadcast/page-presence/my/page");
        TestUtils.addEnqueueingSubscription(queue2, channel2);
        
        msg = notNull(queue2.poll(3, TimeUnit.SECONDS));
        assertHasUser(msg, USER2);
        assertDoesntHaveUser(msg, USER1);
    }
    
    @Test
    public void testDoubleSubscription() throws Exception {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        BlockingQueue<Message> queue3 = new LinkedBlockingQueue<Message>();
        
        BayeuxClient client1 = TestUtils.connectAsFrontend(USER1, PASSWORD1, TestUtils.getAuthServerBaseUrl());
        ClientSessionChannel channel1 = client1.getChannel("/broadcast/page-presence/lets/sub/twice");
        TestUtils.addEnqueueingSubscription(queue1, channel1);
        
        BayeuxClient client2 = TestUtils.connectAsFrontend(USER1, PASSWORD1, TestUtils.getAuthServerBaseUrl());
        ClientSessionChannel channel2 = client2.getChannel("/broadcast/page-presence/lets/sub/twice");
        TestUtils.addEnqueueingSubscription(queue2, channel2);
        
        BayeuxClient client3 = TestUtils.connectAsFrontend(USER2, PASSWORD2, TestUtils.getAuthServerBaseUrl());
        ClientSessionChannel channel3 = client3.getChannel("/broadcast/page-presence/lets/sub/twice");
        TestUtils.addEnqueueingSubscription(queue3, channel3);
        
        Message msg;
        msg = notNull(queue3.poll(3, TimeUnit.SECONDS));
        assertHasUserOnce(msg, USER1);
        
        channel1.unsubscribe();
        msg = notNull(queue3.poll(3, TimeUnit.SECONDS));
        assertHasUserOnce(msg, USER1);
        
        channel2.unsubscribe();
        msg = notNull(queue3.poll(3, TimeUnit.SECONDS));
        assertDoesntHaveUser(msg, USER1);
        assertHasUserOnce(msg, USER2);
    }
    
    private void assertHasUser(Message msg, String user) {
        Object[] users = ((Object[])msg.getDataAsMap().get("users"));
        assertNotNull(users);
        assertTrue("Missing user " + user + " in [" + Joiner.on(", ").join(users) + "]", Arrays.asList(users).contains(user));
    }
    
    private void assertDoesntHaveUser(Message msg, String user) {
        Object[] users = ((Object[])msg.getDataAsMap().get("users"));
        assertNotNull(users);
        assertFalse("Unexpected user " + user + " in [" + Joiner.on(", ").join(users) + "]", Arrays.asList(users).contains(user));
    }
    
    private void assertHasUserOnce(Message msg, String user) {
        Object[] users = ((Object[])msg.getDataAsMap().get("users"));
        assertNotNull(users);
        int count = 0;
        for (int i = 0; i < users.length; ++i) {
            if (users[i].equals(user)) {
                count++;
            }
        }
        assertEquals(1, count);
    }
}
