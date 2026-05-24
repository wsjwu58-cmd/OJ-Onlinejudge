package com.oj.common.exception;

public class DeletionNotAllowedException extends BaseException {
    public DeletionNotAllowedException() {}
    public DeletionNotAllowedException(String message) { super(message); }
}
