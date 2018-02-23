package fi.helsinki.cs.tmc.comet;

import fi.helsinki.cs.tmc.comet.acl.TmcSecurityPolicy;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import org.cometd.bayeux.server.BayeuxServer;

public class Initializer implements ServletContextAttributeListener {
    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        if (BayeuxServer.ATTRIBUTE.equals(event.getName())) {
            BayeuxServer server = (BayeuxServer)event.getValue();
            server.setSecurityPolicy(new TmcSecurityPolicy(Config.getDefault()));
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
    }
}
