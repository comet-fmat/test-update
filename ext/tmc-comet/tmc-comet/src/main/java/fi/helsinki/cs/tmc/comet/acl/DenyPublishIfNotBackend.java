package fi.helsinki.cs.tmc.comet.acl;

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

/**
 * Denies publishing for non-backend users, ignores otherwise.
 */
public class DenyPublishIfNotBackend implements Authorizer {
    public Result authorize(Operation operation, ChannelId channel, ServerSession session, ServerMessage message) {
        if (operation == Operation.PUBLISH) {
            if (AclUtils.isBackendSession(session)) {
                return Result.ignore();
            } else {
                return Result.deny("publishing not allowed");
            }
        } else {
            return Result.ignore();
        }
    }
}
