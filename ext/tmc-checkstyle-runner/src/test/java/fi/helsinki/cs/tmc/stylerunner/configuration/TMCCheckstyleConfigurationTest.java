package fi.helsinki.cs.tmc.stylerunner.configuration;

import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;
import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TMCCheckstyleConfigurationBuilder.class, LoggerFactory.class })
public class TMCCheckstyleConfigurationTest {

    private Logger logger;

    @Before
    public void setUp() {

        logger = mock(Logger.class);
        Whitebox.setInternalState(TMCCheckstyleConfigurationBuilder.class, logger);
    }

    @Test
    public void shouldReturnDefaultConfigurationOnNonExistentConfigurationFile() throws TMCCheckstyleException {

        mockStatic(LoggerFactory.class);
        final Logger configurationLogger = mock(Logger.class);
        when(LoggerFactory.getLogger(TMCCheckstyleConfiguration.class)).thenReturn(configurationLogger);

        final File projectDirectory = new File("test-projects/invalid/ant-3/");

        final TMCCheckstyleConfiguration configuration = TMCCheckstyleConfigurationBuilder.build(projectDirectory);

        assertEquals("unknown-checkstyle.xml", configuration.getRule());

        configuration.getInputSource(projectDirectory);

        verify(configurationLogger).info("Configuration file not found, searching for custom default configuration.");
        verify(configurationLogger).info("Custom configuration not found, using default configuration.");
    }

    @Test
    public void shouldReturnCustomDefaultConfigurationOnNonExistentConfigurationFile() throws TMCCheckstyleException {

        mockStatic(LoggerFactory.class);
        final Logger configurationLogger = mock(Logger.class);
        when(LoggerFactory.getLogger(TMCCheckstyleConfiguration.class)).thenReturn(configurationLogger);

        final File projectDirectory = new File("test-projects/valid/ant-2/");

        final TMCCheckstyleConfiguration configuration = TMCCheckstyleConfigurationBuilder.build(projectDirectory);

        configuration.getInputSource(projectDirectory);

        verify(configurationLogger).info("Configuration file not found, searching for custom default configuration.");
        verify(configurationLogger, never()).info("Custom configuration not found, using default configuration.");
    }
}
