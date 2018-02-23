package fi.helsinki.cs.tmc.stylerunner.validation;

import fi.helsinki.cs.tmc.langs.abstraction.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.junit.Test;

import static org.junit.Assert.*;

public class CheckstyleResultTest {

    @Test
    public void shouldConvertJsonToCheckstyleResult() throws TMCCheckstyleException, IOException {

        final File testProject = new File("test-projects/invalid/ant/");
        final CheckstyleRunner runner = new CheckstyleRunner(testProject, Locale.ROOT);

        final File outputFile = new File("target/output.txt");
        runner.run(outputFile, false);

        final Scanner scanner = new Scanner(outputFile);

        final StringBuilder builder = new StringBuilder();

        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine());
        }

        scanner.close();
        outputFile.delete();

        final CheckstyleResult result = CheckstyleResult.build(builder.toString());

        final List<ValidationError> errors = result.getValidationErrors().get(new File("Trivial.java"));

        assertFalse(result.getValidationErrors().isEmpty());
        assertEquals(3, errors.size());

        String expected = "Indentation incorrect. Expected 0, but was 1.";

        assertEquals(2, errors.get(0).getLine());
        assertEquals(0, errors.get(0).getColumn());
        assertEquals(expected, errors.get(0).getMessage());

        expected = "'{' is not preceded with whitespace.";

        assertEquals(2, errors.get(1).getLine());
        assertEquals(22, errors.get(1).getColumn());
        assertEquals(expected, errors.get(1).getMessage());
    }
}
