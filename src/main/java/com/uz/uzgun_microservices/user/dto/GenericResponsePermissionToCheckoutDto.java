package com.uz.uzgun_microservices.user.dto;

import com.uz.uzgun_microservices.api_responses.ApiErrorResponse;

import java.time.LocalDate;
import java.util.List;

public class GenericResponsePermissionToCheckoutDto {
    private PermissionToCheckoutDto payload;
    private List<ApiErrorResponse> errors;
    private LocalDate timestamp;
    private String trace;
    private String error;

    public GenericResponsePermissionToCheckoutDto(PermissionToCheckoutDto payload, List<ApiErrorResponse> errors, LocalDate timestamp, String trace, String error) {
        this.payload = payload;
        this.errors = errors;
        this.timestamp = timestamp;
        this.trace = trace;
        this.error = error;
    }

    public GenericResponsePermissionToCheckoutDto() {

    }

    public PermissionToCheckoutDto getPayload() {
        return payload;
    }

    public void setPayload(PermissionToCheckoutDto payload) {
        this.payload = payload;
    }

    public List<ApiErrorResponse> getErrors() {
        return errors;
    }

    public void setErrors(List<ApiErrorResponse> errors) {
        this.errors = errors;
    }

    public LocalDate getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDate timestamp) {
        this.timestamp = timestamp;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }



}
