package com.oj.common.exception;

import lombok.Getter;

@Getter
public class ParameterMissingException extends BaseException {
    private final String missingParam;

    public ParameterMissingException(String missingParam) {
        super("缺少必要参数: " + missingParam);
        this.missingParam = missingParam;
    }
}
