package fi.helsinki.cs.tmc.comet;

import fi.helsinki.cs.tmc.comet.acl.DenyPublishIfNotBackend;
import fi.helsinki.cs.tmc.comet.acl.RequireWellKnownGlobalChannelName;
import org.cometd.annotation.Configure;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.server.authorizer.GrantAuthorizer;

/**
 * Configures global channels.
 */
@Service
public class GlobalChannelsService {
    @Configure("/broadcast/tmc/global/**")
    public void confGlobalChannels(ConfigurableServerChannel channel) {
        channel.setPersistent(true);
        channel.addAuthorizer(new RequireWellKnownGlobalChannelName());
        channel.addAuthorizer(new DenyPublishIfNotBackend());
        channel.addAuthorizer(GrantAuthorizer.GRANT_ALL);
    }
}
