package fi.helsinki.cs.tmc.stylerunner.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationError;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CheckstyleResult implements ValidationResult {

    private Strategy strategy = Strategy.DISABLED;
    private final Map<File, List<ValidationError>> validationErrors = new HashMap<File, List<ValidationError>>();

    public static CheckstyleResult build(final String json) throws IOException {

        final SimpleModule module = new SimpleModule("TypeMapper");
        module.addAbstractTypeMapping(ValidationError.class, CheckstyleError.class);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        return mapper.readValue(json, CheckstyleResult.class);
    }

    public void setStrategy(final Strategy strategy) {

        this.strategy = strategy;
    }

    @Override
    public Strategy getStrategy() {

        return strategy;
    }

    @Override
    public Map<File, List<ValidationError>> getValidationErrors() {

        return validationErrors;
    }

    public void addError(final AuditEvent auditEvent) {

        final File file = new File(auditEvent.getFileName());

        if (!validationErrors.containsKey(file)) {
            validationErrors.put(file, new ArrayList<ValidationError>());
        }

        validationErrors.get(file).add(new CheckstyleError(auditEvent));
    }

    @JsonIgnore
    public void writeToFile(final File file) throws IOException {

        new ObjectMapper().writeValue(file, this);
    }
}
