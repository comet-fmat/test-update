package fi.helsinki.cs.tmc.comet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import javax.servlet.ServletException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * Starts a Jetty server.
 *
 * <p>
 * Exists to avoid Jetty's XML hell.
 */
public class ServerMain {

    public static class TmcCometServerSettings {
        public int httpPort = 8080;
        public boolean httpsEnabled = false;
        public int httpsPort = 8443;
        public String keystorePath = "";
        public String keystorePassword = "";
        public String contextPath = "/";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isEmpty()) {
            throw new IllegalArgumentException("Expecting exactly 1 argument, which must be a path to a config file");
        }

        // Set the property so that the webapp can find it.
        System.setProperty("fi.helsinki.cs.tmc.comet.configFile", args[0]);

        // Read TmcCometServerSettings from config file
        TmcCometServerSettings settings = new TmcCometServerSettings();

        Properties props = new Properties();
        props.load(new BufferedReader(new FileReader(args[0])));

        String propPrefix = "fi.helsinki.cs.tmc.comet.server.";
        for (String prop : props.stringPropertyNames()) {
            if (prop.startsWith(propPrefix)) {
                String stringValue = props.getProperty(prop);
                String fieldName = prop.substring(propPrefix.length());
                try {
                    Field field = settings.getClass().getField(fieldName);
                    Object value = stringValue;
                    if (field.getType() == int.class) {
                        value = Integer.parseInt(stringValue);
                    } else if (field.getType() == boolean.class) {
                        value = Boolean.parseBoolean(stringValue);
                    }
                    field.set(settings, value);
                } catch (NoSuchFieldException ex) {
                    throw new IllegalArgumentException("Invalid configuration option: " + prop);
                }
            }
        }

        ServerMain main = new ServerMain(settings);
        main.start();
    }

    private TmcCometServerSettings settings;
    private Server server;

    public ServerMain(TmcCometServerSettings settings) throws Exception {
        this.settings = settings;

        String warFile = findWarFile();

        server = new Server();
        setUpPlainHttpConnector();
        if (settings.httpsEnabled) {
            setUpHttpsConnector();
        }
        setUpWebapp(warFile);
    }

    public void start() throws Exception {
        server.start();
        server.join();
    }

    private void setUpPlainHttpConnector() {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(settings.httpPort);
        server.addConnector(connector);
    }

    private void setUpHttpsConnector() {
        SslContextFactory contextFactory = new SslContextFactory();
        contextFactory.setKeyStorePath(settings.keystorePath);
        contextFactory.setKeyStorePassword(settings.keystorePassword);

        ServerConnector connector = new ServerConnector(server, contextFactory);
        connector.setPort(settings.httpsPort);
        server.addConnector(connector);
    }

    private void setUpWebapp(String warFile) throws IOException, ServletException {
        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath(settings.contextPath);
        ctx.setWar(warFile);
        ctx.setExtractWAR(false);
        ctx.setCopyWebInf(true);  // http://stackoverflow.com/questions/15231028/loading-war-in-embedded-jetty-with-setextractwarfalse-throws-illegalargumentex
        ctx.setServer(server);

        WebSocketServerContainerInitializer.configureContext(ctx);

        server.setHandler(ctx);
    }

    private String findWarFile() throws FileNotFoundException, DependencyCollectionException, DependencyResolutionException {
        ServiceLocator loc = MavenRepositorySystemUtils.newServiceLocator();
        RepositorySystem repoSystem = loc.getService(RepositorySystem.class);
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            throw new IllegalStateException("The system property 'user.home' is not set");
        }
        LocalRepository localRepo = new LocalRepository(homeDir + File.separator + ".m2" + File.separator + "repository");
        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));

        Artifact artifact = new DefaultArtifact("fi.helsinki.cs.tmc", "tmc-comet", "war", "1.0.0-SNAPSHOT");
        String result = session.getLocalRepositoryManager().getPathForLocalArtifact(artifact);

        if (result == null) {
            throw new FileNotFoundException(
                    "tmc-comet artifact not found in the local maven repository. "
                    + "Perhaps you need to `mvn install` it."
            );
        }
        String fullPath = localRepo.getBasedir() + File.separator + result;
        if (!new File(fullPath).exists()) {
            throw new FileNotFoundException(
                    fullPath + " does not exist. "
                    + "Perhaps you need to `mvn install` it."
            );
        }

        return fullPath;
    }
}
