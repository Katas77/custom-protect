package com.example.custom_protect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    int status,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp
) {
    public ErrorResponse(String message, int status) {
        this(message, status, LocalDateTime.now());
    }
}