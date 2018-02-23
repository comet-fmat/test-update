package fi.helsinki.cs.tmc.comet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import fi.helsinki.cs.tmc.comet.acl.TmcSecurityPolicy;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;
import org.cometd.annotation.Configure;
import org.cometd.annotation.Service;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@code /broadcast/page-presence/...} service.
 */
@Service
public class PagePresenceService {
    private static final Logger log = LoggerFactory.getLogger(TmcSecurityPolicy.class);
    
    @Inject
    private BayeuxServer server;
    
    private Registry registry = new Registry();
    
    @Configure("/broadcast/page-presence/**")
    public void confGlobalChannels(ConfigurableServerChannel channel) {
        channel.setPersistent(false);
        // Need to add the listener to the server.
        // Apparently adding a channel subscription listener to the wild card channel doesn't work.
        server.addListener(subListener);
    }
    
    private static class Registry {
        private static class Key {
            public final String pageName;
            public final String username;

            public Key(String pageName, String username) {
                this.pageName = pageName;
                this.username = username;
            }

            @Override
            public int hashCode() {
                return (pageName.hashCode() << 16) + username.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Key) {
                    Key that = (Key)obj;
                    return this.pageName.equals(that.pageName) && this.username.equals(that.username);
                } else {
                    return false;
                }
            }
        }
        
        private ReadWriteLock lock = new ReentrantReadWriteLock(true);
        private HashMultiset<Key> userInstances = HashMultiset.create();
        private HashMultimap<String, String> usersByPage = HashMultimap.create();
        
        public void put(String pageName, String username) {
            lock.writeLock().lock();
            try {
                userInstances.add(new Key(pageName, username));
                usersByPage.put(pageName, username);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public void remove(String pageName, String username) {
            lock.writeLock().lock();
            try {
                int amtBeforeRemove = userInstances.remove(new Key(pageName, username), 1);
                if (amtBeforeRemove <= 1) {
                    usersByPage.remove(pageName, username);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public Set<String> get(String pageName) {
            lock.readLock().lock();
            try {
                return usersByPage.get(pageName);
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    private BayeuxServer.SubscriptionListener subListener = new BayeuxServer.SubscriptionListener() {
        @Override
        public void subscribed(ServerSession session, ServerChannel channel, ServerMessage message) {
            if (!channelMatches(channel)) {
                return;
            }
            
            String pageName = getPageName(session, channel);
            String user = getUsername(session);
            if (pageName != null && user != null) {
                registry.put(pageName, user);
                notifySubscribers(pageName, session, channel);
            } else {
                log.warn("Unexpected subscription to channel " + channel.getId());
            }
        }

        @Override
        public void unsubscribed(ServerSession session, ServerChannel channel, ServerMessage message) {
            if (!channelMatches(channel)) {
                return;
            }
            
            String pageName = getPageName(session, channel);
            String user = getUsername(session);
            if (pageName != null && user != null) {
                registry.remove(pageName, user);
                notifySubscribers(pageName, session, channel);
            }
        }
        
        private boolean channelMatches(ServerChannel channel) {
            return channel.getId().startsWith("/broadcast/page-presence/");
        }
        
        private String getPageName(ServerSession session, ServerChannel channel) {
            ChannelId channelId = channel.getChannelId();
            if (channelId.depth() < 3 && !channelId.isWild()) {
                return null;
            }
            
            StringBuilder result = new StringBuilder(128);
            
            result.append(session.getAttribute(SessionAttributes.SERVER_BASE_URL));
            
            for (int i = 2; i < channelId.depth(); ++i) {
                result.append('/').append(channelId.getSegment(i));
            }
            
            return result.toString();
        }

        private String getUsername(ServerSession session) {
            return (String)session.getAttribute(SessionAttributes.USERNAME);
        }

        private void notifySubscribers(String pageName, ServerSession session, ServerChannel channel) {
            Set<String> users = registry.get(pageName);
            Object data = ImmutableMap.of("users", users);
            channel.publish(session, data);
        }
    };
}
