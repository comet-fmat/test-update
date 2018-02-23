package fi.helsinki.cs.tmc.stylerunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.junit.Assert.*;

public final class MainTest {

    private static final String PROJECT_DIRECTORY_PROPERTY = "tmc.project_dir";
    private static final String VALIDATIONS_FILE_PROPERTY = "tmc.validations_file";
    private static final String LOCALE_PROPERTY = "tmc.locale";
    private static final String OVERWRITE_PROPERTY = "tmc.overwrite_validations_file";
    private static final String OUTPUT_FILE = "target/output.txt";

    @Rule
    public final ExpectedSystemExit publicExit = ExpectedSystemExit.none();

    @Rule
    public final RestoreSystemProperties publicDirectoryProperty = new RestoreSystemProperties(PROJECT_DIRECTORY_PROPERTY);

    @Rule
    public final RestoreSystemProperties publicValidationsProperty = new RestoreSystemProperties(VALIDATIONS_FILE_PROPERTY);

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private PrintStream outputStream;
    private PrintStream errorStream;

    @Before
    public void setUp() {

        outputStream = System.out;
        errorStream = System.err;

        System.setOut(new PrintStream(stdout));
        System.setErr(new PrintStream(stderr));
    }

    @After
    public void tearDown() {

        System.setOut(outputStream);
        System.setErr(errorStream);

        System.setProperty(OVERWRITE_PROPERTY, "");

        final File output = new File(OUTPUT_FILE);
        output.delete();
    }

    private Assertion createAssertionForUsage() {

        return new Assertion() {

            @Override
            public void checkAssertion() {

                final String expected = "Usage:\n" +
                                        "Properties (java -Dproperty=value)\n" +
                                        "  tmc.project_dir — The path for the project directory.\n" +
                                        "  tmc.validations_file — A path to a file to write the validation results.\n" +
                                        "  tmc.locale — Locale for validation messages (ISO 639 standard).\n" +
                                        "  tmc.overwrite_validations_file — Overwrite an existing validation results file (optional).\n";

                assertEquals(expected, stdout.toString());
            }
        };
    }

    private void readAndDeleteOutputFile() throws FileNotFoundException {

        final File file = new File(OUTPUT_FILE);

        assertTrue(file.exists());

        final Scanner scanner = new Scanner(file);
        final String line = scanner.nextLine();

        try {
            assertEquals("{\"strategy\":\"DISABLED\",\"validationErrors\":{}}", line);
        } finally {
            scanner.close();
            file.delete();
        }
    }

    @Test
    public void shouldNotThrowAnyExceptionsWhenRunningMain() throws NoSuchMethodException,
                                                                    IllegalAccessException,
                                                                    InvocationTargetException,
                                                                    InstantiationException {

        final Constructor constructor = Main.class.getDeclaredConstructor((Class<?>[]) null);
        constructor.setAccessible(true);
        constructor.newInstance(null);
    }

    @Test
    public void shouldPrintUsageOnNoArguments() {

        publicExit.expectSystemExitWithStatus(0);
        publicExit.checkAssertionAfterwards(createAssertionForUsage());

        Main.main(new String[0]);
    }

    @Test
    public void shouldPrintUsageOnInvalidProperties() {

        publicExit.expectSystemExitWithStatus(0);
        publicExit.checkAssertionAfterwards(createAssertionForUsage());

        System.setProperty("tmc.invalid", "valid");

        Main.main(new String[0]);
    }

    @Test
    public void shouldCreateJsonFileWithCorrectProperties() throws FileNotFoundException {

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, "test-projects/valid/ant-without-configuration/");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, OUTPUT_FILE);
        System.setProperty(LOCALE_PROPERTY, "fi");

        Main.main(new String[0]);

        readAndDeleteOutputFile();
    }

    @Test
    public void shouldCreateJsonFileOnAdditionalInvalidProperties() throws FileNotFoundException {

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, "test-projects/valid/ant-without-configuration/");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, OUTPUT_FILE);
        System.setProperty(LOCALE_PROPERTY, "en");
        System.setProperty("tmc.invalid", "valid");

        Main.main(new String[0]);

        readAndDeleteOutputFile();
    }

    @Test
    public void shouldExitWithExceptionWithIncorrectProjectDirectory() {

        publicExit.expectSystemExitWithStatus(1);

        publicExit.checkAssertionAfterwards(new Assertion() {

            @Override
            public void checkAssertion() {

                final String expected = "fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException: Path does not contain a testable project.\n";

                assertEquals(expected, stderr.toString());
            }
        });

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, "nonexistent");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, OUTPUT_FILE);
        System.setProperty(LOCALE_PROPERTY, "en");

        Main.main(new String[0]);
    }

    @Test
    public void shouldNotThrowExceptionWithExistingValidationsFilePathWithOverwriteEnabled() {

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, ".");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, OUTPUT_FILE);
        System.setProperty(LOCALE_PROPERTY, "en");

        // Run
        Main.main(new String[0]);

        System.setProperty(OVERWRITE_PROPERTY, "true");

        // Overwrite
        Main.main(new String[0]);
    }

    @Test
    public void shouldThrowExceptionWithExistingValidationsFilePath() {

        publicExit.expectSystemExitWithStatus(1);

        publicExit.checkAssertionAfterwards(new Assertion() {

            @Override
            public void checkAssertion() {

                final String expected = "fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException: Output file already exists.\n";

                assertEquals(expected, stderr.toString());
            }
        });

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, ".");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, "test-projects/valid/maven-without-configuration/pom.xml");
        System.setProperty(LOCALE_PROPERTY, "en");

        Main.main(new String[0]);
    }

    @Test
    public void shouldThrowExceptionWithIncorrectValidationsFilePath() {

        publicExit.expectSystemExitWithStatus(1);

        publicExit.checkAssertionAfterwards(new Assertion() {

            @Override
            public void checkAssertion() {

                final String expected = "fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException: Exception while writing to output file.\n";

                assertTrue(stderr.toString().startsWith(expected));
            }
        });

        System.setProperty(PROJECT_DIRECTORY_PROPERTY, ".");
        System.setProperty(VALIDATIONS_FILE_PROPERTY, "nonexistent/output.txt");
        System.setProperty(LOCALE_PROPERTY, "en");

        Main.main(new String[0]);
    }
}
