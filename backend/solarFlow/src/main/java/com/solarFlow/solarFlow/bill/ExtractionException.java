package com.solarFlow.solarFlow.bill;

public class ExtractionException extends RuntimeException {

    private final String code;         // stored in bill_extractions.error_code
    private final boolean retryable;   // true only for transient errors (timeout, 429, 5xx)

    public ExtractionException(String code, boolean retryable, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public String getCode() { return code; }
    public boolean isRetryable() { return retryable; }
}