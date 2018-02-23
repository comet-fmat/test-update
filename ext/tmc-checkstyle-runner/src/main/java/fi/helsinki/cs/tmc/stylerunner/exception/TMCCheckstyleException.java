package fi.helsinki.cs.tmc.stylerunner.exception;

public final class TMCCheckstyleException extends Exception {

    public TMCCheckstyleException(final String message) {

        super(message);
    }

    public TMCCheckstyleException(final String message, final Throwable cause) {

        super(message, cause);
    }
}
