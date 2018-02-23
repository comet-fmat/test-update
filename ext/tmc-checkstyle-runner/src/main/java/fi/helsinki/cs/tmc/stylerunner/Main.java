package fi.helsinki.cs.tmc.stylerunner;

import fi.helsinki.cs.tmc.stylerunner.exception.TMCCheckstyleException;

import java.io.File;
import java.util.Locale;

public final class Main {

    private Main() {}

    private static void printUsage() {

        System.out.println("Usage:");
        System.out.println("Properties (java -Dproperty=value)");
        System.out.println("  tmc.project_dir — The path for the project directory.");
        System.out.println("  tmc.validations_file — A path to a file to write the validation results.");
        System.out.println("  tmc.locale — Locale for validation messages (ISO 639 standard).");
        System.out.println("  tmc.overwrite_validations_file — Overwrite an existing validation results file (optional).");
    }

    private static void reportException(final Throwable initialException) {

        System.err.println(initialException.getClass().getName() + ": " + initialException.getMessage());
        Throwable exception = initialException;

        while (exception.getCause() != null) {
            exception = exception.getCause();
            System.err.println("Caused by: " + exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    private static String requireProperty(final String name) {

        final String property = System.getProperty(name);

        if (property == null) {
            System.err.println("Missing property: " + name + "\n");
            printUsage();
            System.exit(0);
        }

        return property;
    }

    public static void main(final String[] args) {

        final File projectDirectory = new File(requireProperty("tmc.project_dir"));
        final File output = new File(requireProperty("tmc.validations_file"));
        final Locale locale = new Locale(requireProperty("tmc.locale"));
        final Boolean overwrite = Boolean.valueOf(System.getProperty("tmc.overwrite_validations_file"));

        try {
            new CheckstyleRunner(projectDirectory, locale).run(output, overwrite);
        } catch (TMCCheckstyleException exception) {
            reportException(exception);
            System.exit(1);
        }
    }
}
