package fi.helsinki.cs.tmc.stylerunner.configuration;

import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.InputSource;

public final class TMCCheckstyleConfiguration {

    private static final String DEFAULT_CHECKSTYLE_CONFIGURATION = "default-checkstyle.xml";
    private static final String CUSTOM_DEFAULT_CHECKSTYLE_CONFIGURATION = ".checkstyle.xml";

    private final Logger logger = LoggerFactory.getLogger(TMCCheckstyleConfiguration.class);

    private final String rule;
    private final Strategy strategy;

    public TMCCheckstyleConfiguration() {

        // Default configuration
        rule = DEFAULT_CHECKSTYLE_CONFIGURATION;
        strategy = Strategy.DISABLED;
    }

    public String getRule() {

        return rule;
    }

    public Strategy getStrategy() {

        return strategy;
    }

    public boolean isEnabled() {

        return strategy != Strategy.DISABLED;
    }

    public InputSource getInputSource(final File projectDirectory) throws TMCCheckstyleException {

        // Find Checkstyle-configuration from project
        final File customDefaultConfiguration = new File(projectDirectory, CUSTOM_DEFAULT_CHECKSTYLE_CONFIGURATION);

        File configuration = new File(projectDirectory, rule);

        if (!configuration.exists()) {

            logger.info("Configuration file not found, searching for custom default configuration.");

            if (customDefaultConfiguration.exists()) {
                configuration = customDefaultConfiguration;
            }
        }

        // Use default Checkstyle-configuration
        if (!configuration.exists() || !rule.endsWith("checkstyle.xml")) {

            logger.info("Custom configuration not found, using default configuration.");

            // Default configuration
            return new InputSource(this.getClass()
                                       .getClassLoader()
                                       .getResourceAsStream(DEFAULT_CHECKSTYLE_CONFIGURATION));
        }

        try {
            return new InputSource(new FileInputStream(configuration));
        } catch (FileNotFoundException exception) {
            throw new TMCCheckstyleException("Exception while loading Checkstyle-configuration.", exception);
        }
    }
}
