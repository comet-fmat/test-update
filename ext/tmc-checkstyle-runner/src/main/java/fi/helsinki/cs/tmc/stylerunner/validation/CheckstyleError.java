package fi.helsinki.cs.tmc.stylerunner.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

import fi.helsinki.cs.tmc.langs.abstraction.ValidationError;

public final class CheckstyleError implements ValidationError {

    private final int column;
    private final int line;
    private final String message;
    private final String sourceName;

    @JsonCreator
    public CheckstyleError(@JsonProperty("column") final int column,
                           @JsonProperty("line") final int line,
                           @JsonProperty("message") final String message,
                           @JsonProperty("sourceName") final String sourceName) {

        this.column = column;
        this.line = line;
        this.message = message;
        this.sourceName = sourceName;
    }

    public CheckstyleError(final AuditEvent auditEvent) {

        this(auditEvent.getColumn(), auditEvent.getLine(), auditEvent.getMessage(), auditEvent.getSourceName());
    }

    @Override
    public int getColumn() {

        return column;
    }

    @Override
    public int getLine() {

        return line;
    }

    @Override
    public String getMessage() {

        return message;
    }

    @Override
    public String getSourceName() {

        return sourceName;
    }
}
