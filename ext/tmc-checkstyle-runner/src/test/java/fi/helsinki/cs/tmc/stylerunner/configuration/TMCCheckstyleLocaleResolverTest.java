package fi.helsinki.cs.tmc.stylerunner.configuration;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.*;

public class TMCCheckstyleLocaleResolverTest {

    @Test
    public void shouldReturnDefaultLocaleWhenGivenUnsupportedLocale() {

        assertEquals(Locale.ROOT, TMCCheckstyleLocaleResolver.get(Locale.FRENCH));
    }

    @Test
    public void shouldReturnFinnishLocale() {

        assertEquals(new Locale("fi"), TMCCheckstyleLocaleResolver.get(new Locale("fi")));
    }
}
