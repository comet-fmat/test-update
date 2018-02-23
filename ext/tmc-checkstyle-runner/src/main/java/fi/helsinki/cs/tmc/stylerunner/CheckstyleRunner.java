package fi.helsinki.cs.tmc.stylerunner;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;

import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;

import fi.helsinki.cs.tmc.stylerunner.configuration.TMCCheckstyleConfiguration;
import fi.helsinki.cs.tmc.stylerunner.configuration.TMCCheckstyleConfigurationBuilder;
import fi.helsinki.cs.tmc.stylerunner.configuration.TMCCheckstyleLocaleResolver;
import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;
import fi.helsinki.cs.tmc.stylerunner.listener.CheckstyleResultListener;
import fi.helsinki.cs.tmc.stylerunner.validation.CheckstyleResult;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import org.xml.sax.InputSource;

public final class CheckstyleRunner {

    private final TMCCheckstyleConfiguration checkstyleConfiguration;
    private final Checker checker;
    private final List<File> files;

    public CheckstyleRunner(final File projectDirectory, final Locale locale) throws TMCCheckstyleException {

        this(projectDirectory, TMCCheckstyleConfigurationBuilder.build(projectDirectory), locale);
    }

    public CheckstyleRunner(final File projectDirectory,
            final TMCCheckstyleConfiguration configuration,
            final Locale locale) throws TMCCheckstyleException {

        // Get source directory and check that the project is testable
        final File sourceDirectory = getSourceDirectory(projectDirectory);

        // TMC configuration
        this.checkstyleConfiguration = configuration;

        // Initialisation
        try {

            checker = new Checker();
            final InputSource inputSource = configuration.getInputSource(projectDirectory);

            // Configuration
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(ConfigurationLoader.loadConfiguration(inputSource,
                    new PropertiesExpander(System.getProperties()),
                    false));
        } catch (CheckstyleException exception) {
            throw new TMCCheckstyleException("Checkstyle failed.", exception);
        }

        // Get all .java files from project’s source directory
        files = (List<File>) FileUtils.listFiles(sourceDirectory, new String[]{"java"}, true);

        // Set base directory
        checker.setBasedir(sourceDirectory.getAbsolutePath());

        // Configure localisation locale
        LocalizedMessage.setLocale(TMCCheckstyleLocaleResolver.get(locale));
    }

    private File getSourceDirectory(final File projectDirectory) throws TMCCheckstyleException {

        // Ant-project
        File sourceDirectory = new File(projectDirectory, "src/");

        // Maven-project
        if (new File(projectDirectory, "pom.xml").exists()) {
            sourceDirectory = new File(projectDirectory, "src/main/java/");
        }

        // Invalid directory
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            throw new TMCCheckstyleException("Path does not contain a testable project.");
        }

        return sourceDirectory;
    }

    public ValidationResult run() {

        // Checkstyle disabled, return empty result
        if (!checkstyleConfiguration.isEnabled()) {
            return new CheckstyleResult();
        }

        // Listener
        final CheckstyleResultListener listener = new CheckstyleResultListener();
        checker.addListener(listener);

        try {
            // Process
            checker.process(files);
        } catch (CheckstyleException ex) {
            throw new RuntimeException(
                    new TMCCheckstyleException(
                            "This is a system error while checking code style.", ex));
        }

        // Clean up
        checker.destroy();

        final CheckstyleResult result = listener.getResult();
        result.setStrategy(checkstyleConfiguration.getStrategy());

        return result;
    }

    public void run(final File outputFile, final boolean overwrite) throws TMCCheckstyleException {

        final CheckstyleResult result = (CheckstyleResult) run();

        if (!overwrite && outputFile.exists()) {
            throw new TMCCheckstyleException("Output file already exists.");
        }

        try {
            result.writeToFile(outputFile);
        } catch (IOException exception) {
            throw new TMCCheckstyleException("Exception while writing to output file.", exception);
        }
    }
}
