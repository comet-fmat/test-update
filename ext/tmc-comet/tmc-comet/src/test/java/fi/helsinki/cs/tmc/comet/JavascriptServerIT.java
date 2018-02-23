package fi.helsinki.cs.tmc.comet;

import org.junit.Test;
import us.monoid.web.Resty;
import static org.junit.Assert.*;

public class JavascriptServerIT extends AbstractTmcCometTest {
    @Test
    public void servesTheCometdJavascriptLibraries() throws Exception {
        String js = new Resty().text(TestUtils.getJavascriptBaseUrl() + "/org/cometd.js").toString();
        assertTrue(js.contains("org.cometd"));
    }
}
