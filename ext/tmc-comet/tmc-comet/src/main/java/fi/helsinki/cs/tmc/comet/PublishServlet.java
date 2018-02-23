package fi.helsinki.cs.tmc.comet;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a simple synchronous HTTP interface for publishing messages.
 * This way, the backend doesn't need to implement Bayeux.
 */
public class PublishServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PublishServlet.class);
    
    private BayeuxServer server;

    @Override
    public void init() throws ServletException {
        super.init();
        server = (BayeuxServer)getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doPostImpl(req, resp);
        } catch (Exception e) {
            log.error("POST request failed", e);
        }
    }
    
    private void doPostImpl(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String channel = req.getParameter("channel");
        String data = req.getParameter("data");
        
        if (!allExist(channel, data)) {
            respondWith(resp, SC_NOT_FOUND, "Invalid parameters.");
            return;
        }
        
        Object dataJson;
        try {
            dataJson = JSON.parse(data);
        } catch (Exception e) {
            respondWith(resp, SC_NOT_FOUND, "Data parameter is not a valid JSON value.");
            return;
        }
        
        LocalSession session = server.newLocalSession("publish");
        session.setAttribute(SessionAttributes.AUTHENTICATE_THIS_LOCAL_SESSION, true);
        
        Map<String, Object> authParams = new HashMap<String, Object>();
        authParams.put("serverBaseUrl", req.getParameter("serverBaseUrl"));
        authParams.put("backendKey", req.getParameter("backendKey"));
        session.addExtension(getAuthenticationExtension(authParams));
        
        session.handshake();
        if (!session.isHandshook()) {
            respondWith(resp, SC_FORBIDDEN, "Access denied.");
            return;
        }
        
        final Semaphore sem = new Semaphore(0);
        final AtomicBoolean success = new AtomicBoolean();
        final AtomicReference<String> error = new AtomicReference<String>("");
        session.getChannel(channel).publish(dataJson, new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                try { // Being defensive. Nothing should go wrong.
                    success.set(message.isSuccessful());
                    Object e = message.get(Message.ERROR_FIELD);
                    if (!(e instanceof String)) {
                        error.set((String)e);
                    }
                } finally {
                    sem.release();
                }
            }
        });
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        
        if (success.get()) {
            respondWith(resp, SC_OK, "OK");
        } else {
            String msg = "Failed to publish message: " + error.get();
            log.info(msg);
            respondWith(resp, SC_INTERNAL_SERVER_ERROR, msg);
        }
    }
    
    private boolean allExist(String ... values) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == null) {
                return false;
            }
        }
        return true;
    }
    
    private void respondWith(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType("text/plain; charset=utf-8");
        Writer w = resp.getWriter();
        w.append(msg);
        w.close();
    }
    
    private LocalSession.Extension getAuthenticationExtension(final Map<String, Object> fields) {
        return new LocalSession.Extension() {
            public boolean rcv(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean rcvMeta(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean send(ClientSession session, Message.Mutable message) {
                return true;
            }

            public boolean sendMeta(ClientSession session, Message.Mutable message) {
                message.getExt(true).put("authentication", fields);
                return true;
            }
        };
    }
}
