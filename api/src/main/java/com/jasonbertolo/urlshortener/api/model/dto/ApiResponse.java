package com.jasonbertolo.urlshortener.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiResponse<T> {

    public static final String OK = "ok";

    private String status;
    private String message;
    private T data;

    public ApiResponse(T data) {
        this.status = OK;
        this.message = data.getClass().getSimpleName();
        this.data = data;
    }

    public ApiResponse(String message, T data) {
        this.status = OK;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
