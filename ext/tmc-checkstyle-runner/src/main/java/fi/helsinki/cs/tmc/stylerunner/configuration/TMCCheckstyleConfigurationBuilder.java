package fi.helsinki.cs.tmc.stylerunner.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TMCCheckstyleConfigurationBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMCCheckstyleConfigurationBuilder.class);

    // Create string as object instead of literal for testing purposes
    private static final String TMC_CONFIGURATION_JSON = new String(".tmcproject.json");
    private static final String TMC_CONFIGURATION_YAML = new String(".tmcproject.yml");

    private TMCCheckstyleConfigurationBuilder() {}

    private static File getConfigurationFile(final File projectDirectory, final String configuration) {

        // Find TMC-configuration from project
        final File matchingFile = new File(projectDirectory, configuration);

        // No TMC-configuration file found
        if (!matchingFile.exists()) {
            return null;
        }

        return matchingFile;
    }

    public static TMCCheckstyleConfiguration build(final File projectDirectory) {

        final File configurationFileJson = getConfigurationFile(projectDirectory, TMC_CONFIGURATION_JSON);
        final File configurationFileYaml = getConfigurationFile(projectDirectory, TMC_CONFIGURATION_YAML);

        // No TMC-configuration found, use default
        if (configurationFileJson == null && configurationFileYaml == null) {
            return new TMCCheckstyleConfiguration();
        }

        try {

            ObjectMapper mapper = new ObjectMapper();

            if (configurationFileJson == null) {

                mapper = new ObjectMapper(new YAMLFactory());
                LOGGER.info("JSON configuration not found, using YAML configuration.");
            }

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            final File configuration = configurationFileJson != null ? configurationFileJson : configurationFileYaml;
            final JsonNode rootNode = mapper.readTree(configuration);

            // No Checkstyle-configuration found, use default
            if (rootNode.findValue("checkstyle") == null) {
                return new TMCCheckstyleConfiguration();
            }

            return mapper.treeToValue(rootNode.path("checkstyle"), TMCCheckstyleConfiguration.class);

        } catch (IOException exception) {

            LOGGER.error("Exception while deserialising TMCConfiguration.");
            return new TMCCheckstyleConfiguration();
        }
    }
}
