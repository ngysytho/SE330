package com.example.chatapp.exception;

public record ErrorResponse(int status, String error, String message, long timestamp) {
}
