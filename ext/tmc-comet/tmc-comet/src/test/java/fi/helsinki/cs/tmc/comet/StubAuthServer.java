package fi.helsinki.cs.tmc.comet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.LoggerFactory;

public class StubAuthServer {
    private final int port;
    private final Set<StubUser> users;
    private final Map<String, StubUser> sessionIdToUser;
    private Server server;
    
    private static class StubUser {
        public final String username;
        public final String password;
        public StubUser(String username, String password) {
            if (username == null) {
                username = "";
            }
            if (password == null) {
                password = "";
            }
            this.username = username;
            this.password = password;
        }

        @Override
        public int hashCode() {
            return username.hashCode() + password.hashCode() << 16;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof StubUser) {
                StubUser that = (StubUser)obj;
                return this.username.equals(that.username) && this.password.equals(that.password);
            } else {
                return false;
            }
        }
    }

    public StubAuthServer(int port) throws Exception {
        this.port = port;
        this.users = new HashSet<StubUser>();
        this.sessionIdToUser = new HashMap<String, StubUser>();
        this.server = new Server(port);
        server.setHandler(handler);
        server.start();
    }
    
    public synchronized void addUser(String username, String password) {
        users.add(new StubUser(username, password));
    }
    
    public synchronized void addSession(String username, String sessionId) {
        for (StubUser user : users) {
            if (user.username.equals(username)) {
                sessionIdToUser.put(sessionId, user);
                break;
            }
        }
    }
    
    public synchronized boolean userExists(String username, String password) {
        return users.contains(new StubUser(username, password));
    }
    
    public synchronized boolean sessionIsValid(String username, String sessionId) {
        StubUser user = null;
        for (StubUser candidate : users) {
            if (candidate.username.equals(username)) {
                user = candidate;
                break;
            }
        }
        
        return user.equals(sessionIdToUser.get(sessionId));
    }
    
    private Handler handler = new AbstractHandler() {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String pathInfo = baseRequest.getPathInfo();
            if (pathInfo != null && pathInfo.endsWith("/auth.text")) {
                boolean userOk = false;
                
                String username = request.getParameter("username");
                String password = request.getParameter("password");
                String sessionId = request.getParameter("session_id");
                
                if (password != null) {
                    userOk = userExists(username, password);
                } else if (sessionId != null) {
                    userOk = sessionIsValid(username, sessionId);
                }
                
                response.setContentType("text/plain; charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.setStatus(HttpServletResponse.SC_OK);
                PrintWriter w = response.getWriter();
                if (userOk) {
                    w.println("OK");
                } else {
                    w.println("FAIL");
                }
                w.close();
                baseRequest.setHandled(true);
            }
        }
    };

    public int getPort() {
        return port;
    }
    
    public void close() {
        try {
            server.stop();
            server.join();
        } catch (Exception ex) {
            LoggerFactory.getLogger(StubAuthServer.class).error("While shutting down StubAuthServer: " + ex.getMessage());
        }
    }
}
