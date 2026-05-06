package com.chronos.exception;

public class JobSchedulingException extends RuntimeException {
    public JobSchedulingException(String message) { super(message); }
    public JobSchedulingException(String message, Throwable cause) { super(message, cause); }
}
