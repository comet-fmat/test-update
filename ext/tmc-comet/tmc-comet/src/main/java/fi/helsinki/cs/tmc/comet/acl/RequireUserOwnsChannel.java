package fi.helsinki.cs.tmc.comet.acl;

import fi.helsinki.cs.tmc.comet.SessionAttributes;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

/**
 * Denies call for user sessions if the channel is unknown or does not belong to the user, ignores otherwise.
 */
public class RequireUserOwnsChannel implements Authorizer {
    private static String PREFIX = "/broadcast/tmc/user/";
    private static final String[] PER_USER_CHANNELS = {
        "review-available"
    };
    
    public Result authorize(Operation operation, ChannelId channel, ServerSession session, ServerMessage message) {
        if (AclUtils.isBackendSession(session)) {
            return Result.ignore();
        } else {
            return requireUsernameMatches(channel, session);
        }
    }
    
    private Result requireUsernameMatches(ChannelId channelId, ServerSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        if (username != null && username instanceof String) {
            String channel = channelId.toString();
            for (String perUser : PER_USER_CHANNELS) {
                String candidate = PREFIX + username + "/" + perUser;
                if (channel.equals(candidate)) {
                    return Result.ignore();
                }
            }
            return Result.deny("you don't have access to this channel");
        } else {
            return Result.deny("not an authenticated user's session");
        }
    }
}
