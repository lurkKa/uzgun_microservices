package com.uz.uzgun_microservices.api_responses;


public class ApiErrorResponse {
    private String code;
    private String message;
    private String detailMessage;
    private Object payload;

    public ApiErrorResponse(String code, String message, String detailMessage, Object payload) {
        this.code = code;
        this.message = message;
        this.detailMessage = detailMessage;
        this.payload = payload;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
