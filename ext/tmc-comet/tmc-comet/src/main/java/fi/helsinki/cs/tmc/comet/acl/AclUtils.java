package fi.helsinki.cs.tmc.comet.acl;

import fi.helsinki.cs.tmc.comet.SessionAttributes;
import org.cometd.bayeux.server.ServerSession;

public class AclUtils {
    public static boolean isBackendSession(ServerSession session) {
        Object obj = session.getAttribute(SessionAttributes.IS_BACKEND);
        return obj instanceof Boolean && (Boolean) obj == true;
    }
}
