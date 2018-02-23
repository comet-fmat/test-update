package fi.helsinki.cs.tmc.stylerunner.configuration;

import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import static org.mockito.Mockito.verify;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TMCCheckstyleConfigurationBuilder.class, LoggerFactory.class })
public class TMCCheckstyleConfigurationBuilderTest {

    private Logger logger;

    @Before
    public void setUp() {

        logger = mock(Logger.class);
        Whitebox.setInternalState(TMCCheckstyleConfigurationBuilder.class, logger);
    }

    @Test
    public void shouldReturnDefaultTMCConfigurationOnNonexistentConfiguration() throws TMCCheckstyleException {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/valid/ant-without-configuration/"));

        assertNotNull(config);
    }

    @Test
    public void shouldReturnTMCConfiguration() throws TMCCheckstyleException, FileNotFoundException {

        final File projectDirectory = new File("test-projects/valid/ant/");
        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(projectDirectory);

        assertNotNull(config);
        assertEquals("mooc-checkstyle.xml", config.getRule());
        assertFalse(config.isEnabled());
        assertNotNull(config.getInputSource(projectDirectory));
    }

    @Test
    public void shouldNotFailOnAdditionalAndInvalidJSONProperties() throws TMCCheckstyleException {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/invalid/ant/"));

        assertNotNull(config);
        assertEquals("default-checkstyle.xml", config.getRule());
        assertTrue(config.isEnabled());
    }

    @Test
    public void shouldNotFailOnAdditionalJSONRootProperties() throws TMCCheckstyleException {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/invalid/maven/"));

        assertNotNull(config);
        assertEquals("mooc-checkstyle.xml", config.getRule());
        assertTrue(config.isEnabled());
    }

    @Test
    public void shouldReturnDefaultTMCConfigurationIfConfigurationIsInvalid() throws TMCCheckstyleException {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/invalid/maven-2/"));

        assertEquals("default-checkstyle.xml", config.getRule());
        assertFalse(config.isEnabled());
    }

    @Test
    public void shouldReturnDefaultConfigurationOnInvalidJSONPropertyValue() throws TMCCheckstyleException, IllegalAccessException {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/invalid/ant-2/"));

        verify(logger).error("Exception while deserialising TMCConfiguration.");
    }

    @Test
    public void shouldUseYAMLConfiguration() {

        final TMCCheckstyleConfiguration config = TMCCheckstyleConfigurationBuilder.build(new File("test-projects/valid/ant-2/"));

        assertEquals("mooc-checkstyle.xml", config.getRule());
        assertEquals(Strategy.WARN, config.getStrategy());

        verify(logger).info("JSON configuration not found, using YAML configuration.");
    }
}
