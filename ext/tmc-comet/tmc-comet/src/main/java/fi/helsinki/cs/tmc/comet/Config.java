package fi.helsinki.cs.tmc.comet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Properties;

public class Config {
    private static Config instance;
    
    public static Config getDefault() {
        if (instance == null) {
            Properties props;
            if (!System.getProperty("fi.helsinki.cs.tmc.comet.noConfigFile", "").isEmpty()) {
                props = System.getProperties();
            } else {
                props = new Properties();
                String configFile = System.getProperty("fi.helsinki.cs.tmc.comet.configFile", "");
                if (configFile.isEmpty()) {
                    throw new RuntimeException("Please run with -Dfi.helsinki.cs.tmc.comet.configFile=/path/to/config.properties");
                }
                try {
                    loadProperties(props, new File(configFile));
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to read configuration file: " + ex.toString(), ex);
                }
            }
            
            instance = new Config(props);
        }
        return instance;
    }
    
    
    private String[] allowedServers;
    private String backendKey;
    
    private Config(Properties props) {
        allowedServers = props.getProperty("fi.helsinki.cs.tmc.comet.allowedServers", "").split(";");
        for (int i = 0; i < allowedServers.length; ++i) {
            allowedServers[i] = normalizeServerUrl(allowedServers[i]);
        }
        
        backendKey = props.getProperty("fi.helsinki.cs.tmc.comet.backendKey");
        if (backendKey == null) {
            throw new IllegalArgumentException("backendKey configuration missing");
        }
    }
    
    public boolean isAllowedServer(String serverUrl) {
        serverUrl = normalizeServerUrl(serverUrl);
        for (int i = 0; i < allowedServers.length; ++i) {
            if (allowedServers[i].equals(serverUrl)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBackendKey(String candidate) {
        return candidate.equals(backendKey);
    }
    
    public final String normalizeServerUrl(String url) {
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return URI.create(url).toString();
    }
    
    private static void loadProperties(Properties props, File file) throws IOException {
        Reader reader = null;
        try {
            reader = new FileReader(file);
            props.load(reader);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
