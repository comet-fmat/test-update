package fi.helsinki.cs.tmc.comet.acl;

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

/**
 * Denies all if the channel is not a known global channel name, ignores otherwise.
 */
public class RequireWellKnownGlobalChannelName implements Authorizer {
    private static final String[] GLOBAL_CHANNELS = {
        "/broadcast/tmc/global/admin-msg",
        "/broadcast/tmc/global/course-updated"
    };
    
    public Result authorize(Operation operation, ChannelId channel, ServerSession session, ServerMessage message) {
        if (isWellKnown(channel, session)) {
            return Result.ignore();
        } else {
            return Result.deny("unknown channel");
        }
    }

    protected boolean isWellKnown(ChannelId channelId, ServerSession session) {
        String channel = channelId.toString();
        for (int i = 0; i < GLOBAL_CHANNELS.length; ++i) {
            if (channel.equals(GLOBAL_CHANNELS[i])) {
                return true;
            }
        }
        return false;
    }
}
