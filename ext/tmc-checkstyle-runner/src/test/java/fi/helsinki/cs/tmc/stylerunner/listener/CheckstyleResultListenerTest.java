package fi.helsinki.cs.tmc.stylerunner.listener;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;

import fi.helsinki.cs.tmc.langs.abstraction.ValidationError;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.verify;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class CheckstyleResultListenerTest {

    private CheckstyleResultListener resultListener;
    private final AuditEvent audit = createNewAuditEvent("tiedosto", 0, 1, "viesti", "testi");
    private final AuditEvent audit2 = createNewAuditEvent("tiedosto2", 1, 1, "Viesti 2", "test2");

    @Before
    public void setUp() {

        resultListener = new CheckstyleResultListener();
    }

    private AuditEvent createNewAuditEvent(final String filename,
                                           final int lineNumber,
                                           final int columnNumber,
                                           final String message,
                                           final String sourceName) {

        return new AuditEvent(sourceName, filename, new LocalizedMessage(lineNumber,
                                                                         columnNumber,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         String.class,
                                                                         message));
    }

    private void containsErrors(final AuditEvent... errors) {

        final Map<File, List<ValidationError>> validationErrors = this.resultListener.getResult().getValidationErrors();

        assertEquals(errors.length, countErrorsFromMap(validationErrors));

        final Set<String> files = new TreeSet<String>();

        for (final AuditEvent error : errors) {
            files.add(error.getFileName());
            assertTrue(containsError(validationErrors, error));
        }

        assertEquals(files.size(), validationErrors.size());
    }

    private int countErrorsFromMap(final Map<File, List<ValidationError>> validationErrors) {

        int errors = 0;

        for (final Collection<ValidationError> errorCollection : validationErrors.values()) {
            errors += errorCollection.size();
        }

        return errors;
    }

    private boolean containsError(final Map<File, List<ValidationError>> validationErrors, final AuditEvent error) {

        for (Entry<File, List<ValidationError>> entry : validationErrors.entrySet()) {

            for (ValidationError validationError : entry.getValue()) {

                if (validationError.getSourceName().equals(error.getSourceName()) &&
                    validationError.getLine() == error.getLine() &&
                    validationError.getColumn() == error.getColumn() &&
                    validationError.getSourceName().equals(error.getSourceName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Test
    public void shouldContainZeroErrorsOnCreate() {

        containsErrors();
    }

    @Test
    public void shouldBeAbleToAddNewError() {

        resultListener.addError(audit);
        containsErrors(audit);
    }

    @Test
    public void shouldBeAbleToAddMultipleErrorsDifferentFiles() {

        resultListener.addError(audit);
        resultListener.addError(audit2);

        containsErrors(audit, audit2);
    }

    @Test
    public void shouldBeAbleToAddMultipleErrorsSameFile() {

        resultListener.addError(audit);
        resultListener.addError(audit);

        containsErrors(audit, audit);
    }

    @Test
    public void shouldBeAbleToAddMultipleErrorsToSameFilesAndDifferentFilesAtASameTime() {

        resultListener.addError(audit);
        resultListener.addError(audit);
        resultListener.addError(audit2);
        resultListener.addError(audit2);

        containsErrors(audit, audit, audit2, audit2);
    }

    @Test
    public void shouldBeAbleToAddNewException() {

        mockStatic(LoggerFactory.class);
        final Logger logger = mock(Logger.class);
        when(LoggerFactory.getLogger(CheckstyleResultListener.class)).thenReturn(logger);
        new CheckstyleResultListener().addException(audit, new Throwable("error"));

        verify(logger).error("Exception while audit: {}", "error");
    }
}
