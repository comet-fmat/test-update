package fi.helsinki.cs.tmc.stylerunner;

import com.google.common.io.Files;

import fi.helsinki.cs.tmc.langs.abstraction.ValidationError;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;
import fi.helsinki.cs.tmc.stylerunner.configuration.TMCCheckstyleConfigurationBuilder;
import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class CheckstyleRunnerTest {

    @Rule
    public ExpectedException publicThrown = ExpectedException.none();

    private static void setFinalStatic(final Field field, final Object newValue) throws IllegalAccessException,
                                                                                        NoSuchFieldException {

        field.setAccessible(true);

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    private File createAntProjectMock() {

        try {

            final File tmp = Files.createTempDir();
            final File srcFile = new File(tmp, "src/Main.java");
            Files.createParentDirs(srcFile);

            return tmp;

        } catch (IOException exception) {
            fail(exception.getMessage());
        }

        return null;
    }

    private File createMavenProjectMock() {

        try {

            final File tmp = Files.createTempDir();
            final File srcFile = new File(tmp, "src/Main/Main.java");
            final File testFile = new File(tmp, "src/Test/MainTest.java");

            Files.createParentDirs(srcFile);
            Files.createParentDirs(testFile);

            return tmp;

        } catch (IOException exception) {
            fail(exception.getMessage());
        }

        return null;
    }

    @Test
    public void shouldWorkWithAntProjects() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final File tmp = createAntProjectMock();

        final Method method = CheckstyleRunner.class.getDeclaredMethod("getSourceDirectory", File.class);
        method.setAccessible(true);

        try {
            method.invoke(new CheckstyleRunner(tmp, Locale.ROOT), tmp);
        } catch (TMCCheckstyleException exception) {
            fail();
        }
    }

    @Test
    public void shouldWorkWithMavenProjects() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final File tmp = createMavenProjectMock();

        final Method method = CheckstyleRunner.class.getDeclaredMethod("getSourceDirectory", File.class);
        method.setAccessible(true);

        try {
            method.invoke(new CheckstyleRunner(tmp, Locale.ROOT), tmp);
        } catch (TMCCheckstyleException exception) {
            fail();
        }
    }

    @Test
    public void shouldNotWorkWhenDirectoryNotInCorrectFormat() throws IllegalAccessException,
                                                                      InvocationTargetException,
                                                                      NoSuchMethodException,
                                                                      TMCCheckstyleException {

        final File tmp = Files.createTempDir();

        final Method method = CheckstyleRunner.class.getDeclaredMethod("getSourceDirectory", File.class);
        method.setAccessible(true);

        publicThrown.expect(TMCCheckstyleException.class);
        publicThrown.expectMessage("Path does not contain a testable project.");

        method.invoke(new CheckstyleRunner(tmp, Locale.ROOT), tmp);
    }

    @Test
    public void shouldThrowExceptionOnInvalidProjectDirectory() throws TMCCheckstyleException {

        publicThrown.expect(TMCCheckstyleException.class);
        publicThrown.expectMessage("Path does not contain a testable project.");

        new CheckstyleRunner(new File("src/"), Locale.ROOT);
    }

    @Test
    public void shouldRunOnValidProjectDirectory() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("."), Locale.ROOT).run();
        assertNotNull(result);
    }

    @Test
    public void shouldNotHaveValidationErrors() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/valid/ant-without-configuration/"), Locale.ROOT).run();
        assertTrue(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldNotHaveValidationErrorsOnAntTestProject() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/valid/ant-without-configuration/"), Locale.ROOT).run();
        assertTrue(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldHaveValidationErrorsOnAntTestProject() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/invalid/ant/"), Locale.ROOT).run();
        assertFalse(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldHaveErroneousClassOnAntTestProject() throws TMCCheckstyleException {

        final File testProject = new File("test-projects/invalid/ant/");
        final ValidationResult result = new CheckstyleRunner(testProject, Locale.ROOT).run();

        final List<ValidationError> errors = result.getValidationErrors().get(new File("Trivial.java"));

        assertFalse(result.getValidationErrors().isEmpty());
        assertEquals(3, errors.size());

        String expected = "Indentation incorrect. Expected 0, but was 1.";

        assertEquals("com.puppycrawl.tools.checkstyle.checks.indentation.IndentationCheck", errors.get(0).getSourceName());
        assertEquals(2, errors.get(0).getLine());
        assertEquals(0, errors.get(0).getColumn());
        assertEquals(expected, errors.get(0).getMessage());

        expected = "'{' is not preceded with whitespace.";

        assertEquals("com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAroundCheck", errors.get(1).getSourceName());
        assertEquals(2, errors.get(1).getLine());
        assertEquals(22, errors.get(1).getColumn());
        assertEquals(expected, errors.get(1).getMessage());
    }

    @Test
    public void shouldNotHaveValidationErrorsOnMavenTestProject() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/valid/maven-without-configuration/"), Locale.ROOT).run();
        assertTrue(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldHaveValidationErrorsOnMavenTestProject() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/invalid/maven/"), Locale.ROOT).run();
        assertFalse(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldHaveErroneousClassOnMavenTestProject() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/invalid/maven/"), Locale.ROOT).run();
        final List<ValidationError> errors = result.getValidationErrors().get(new File("fi/helsinki/cs/maventest/App.java"));

        assertFalse(result.getValidationErrors().isEmpty());
        assertEquals(4, errors.size());

        final String expected = "Utility classes should not have a public or default constructor. Declare the constructor as private.";

        assertEquals("com.puppycrawl.tools.checkstyle.checks.design.HideUtilityClassConstructorCheck", errors.get(0).getSourceName());
        assertEquals(3, errors.get(0).getLine());
        assertEquals(1, errors.get(0).getColumn());
        assertEquals(expected, errors.get(0).getMessage());
    }

    @Test
    public void shouldReturnEmptyValidationResultWhenCheckstyleIsDisabled() throws TMCCheckstyleException {

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/invalid/ant-without-configuration/"), Locale.ROOT).run();
        assertTrue(result.getValidationErrors().isEmpty());
    }

    @Test
    public void shouldReturnValidationErrorsWhenCheckstyleIsEnabled() throws TMCCheckstyleException, NoSuchFieldException, IllegalAccessException {

        setFinalStatic(TMCCheckstyleConfigurationBuilder.class.getDeclaredField("TMC_CONFIGURATION_JSON"), ".tmcproject-enabled.json");

        final ValidationResult result = new CheckstyleRunner(new File("test-projects/invalid/ant/"), Locale.ROOT).run();

        final List<ValidationError> errors = result.getValidationErrors().get(new File("Trivial.java"));

        assertFalse(result.getValidationErrors().isEmpty());

        assertEquals(2, errors.size());
        assertEquals(2, errors.get(0).getLine());
        assertEquals(0, errors.get(0).getColumn());
        assertEquals("Missing package declaration.", errors.get(0).getMessage());

        assertEquals(2, errors.get(1).getLine());
        assertEquals(22, errors.get(1).getColumn());
        assertEquals("'{' is not preceded with whitespace.", errors.get(1).getMessage());

        setFinalStatic(TMCCheckstyleConfigurationBuilder.class.getDeclaredField("TMC_CONFIGURATION_JSON"), ".tmcproject.json");
    }
}
