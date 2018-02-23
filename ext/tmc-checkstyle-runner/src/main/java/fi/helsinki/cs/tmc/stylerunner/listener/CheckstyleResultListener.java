package fi.helsinki.cs.tmc.stylerunner.listener;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

import fi.helsinki.cs.tmc.stylerunner.validation.CheckstyleResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CheckstyleResultListener implements AuditListener {

    private final Logger logger = LoggerFactory.getLogger(CheckstyleResultListener.class);
    private final CheckstyleResult result = new CheckstyleResult();

    @Override
    public void auditStarted(final AuditEvent auditEvent) {

        logger.info("Starting audit...");
    }

    @Override
    public void auditFinished(final AuditEvent auditEvent) {

        logger.info("Audit finished.");
    }

    @Override
    public void fileStarted(final AuditEvent auditEvent) {

        logger.info("Auditing file {}...", auditEvent.getFileName());
    }

    @Override
    public void fileFinished(final AuditEvent auditEvent) {

        logger.info("Auditing file {} finished.", auditEvent.getFileName());
    }

    @Override
    public void addError(final AuditEvent auditEvent) {

        logger.info("Validation error {}: In {} ({}).", auditEvent.getSourceName(),
                                                        auditEvent.getMessage(),
                                                        auditEvent.getFileName(),
                                                        auditEvent.getLine(),
                                                        auditEvent.getColumn());

        result.addError(auditEvent);
    }

    @Override
    public void addException(final AuditEvent auditEvent, final Throwable throwable) {

        logger.error("Exception while audit: {}", throwable.getMessage());
    }

    public CheckstyleResult getResult() {

        return result;
    }
}
