package com.tis.nablarch.mcp.tools.handlerqueue.model;

import java.util.List;

/**
 * ハンドラキュー制約検証結果DTO。
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
}
