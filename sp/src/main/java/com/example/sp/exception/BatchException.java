package com.example.sp.exception;

public class BatchException extends RuntimeException {
    //Exception to deal with runtime exceptions while performing batch operations 
    public BatchException(String message) {
        super(message);
    }
}
