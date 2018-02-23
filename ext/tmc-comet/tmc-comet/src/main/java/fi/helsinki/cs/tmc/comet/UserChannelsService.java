package fi.helsinki.cs.tmc.comet;

import fi.helsinki.cs.tmc.comet.acl.DenyPublishIfNotBackend;
import fi.helsinki.cs.tmc.comet.acl.RequireUserOwnsChannel;
import org.cometd.annotation.Configure;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.server.authorizer.GrantAuthorizer;

/**
 * Configures per-user channels.
 */
@Service
public class UserChannelsService {
    @Configure("/broadcast/tmc/user/**")
    public void confUserChannels(ConfigurableServerChannel channel) {
        channel.setPersistent(false);
        channel.addAuthorizer(new RequireUserOwnsChannel());
        channel.addAuthorizer(new DenyPublishIfNotBackend());
        channel.addAuthorizer(GrantAuthorizer.GRANT_ALL);
    }
}
