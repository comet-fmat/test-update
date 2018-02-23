package fi.helsinki.cs.tmc.stylerunner.configuration;

import java.util.Locale;

public final class TMCCheckstyleLocaleResolver {

    private static final Locale[] SUPPORTED_LOCALES = { new Locale("fi") };

    private TMCCheckstyleLocaleResolver() {}

    public static Locale get(final Locale locale) {

        for (Locale supported : SUPPORTED_LOCALES) {

            if (locale.equals(supported)) {
                return locale;
            }
        }

        return Locale.ROOT;
    }
}
