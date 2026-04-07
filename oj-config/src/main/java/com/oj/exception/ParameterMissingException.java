package com.oj.exception;

/**
 * 逻辑层面的参数缺失异常（用于 Agent 询问用户补齐关键信息）。
 * 注意：不要把它当成“代码 bug”，它代表业务流程需要用户补参数。
 */
public class ParameterMissingException extends BaseException {

    private final String missingParam;

    public ParameterMissingException(String missingParam) {
        super("参数缺失");
        this.missingParam = missingParam;
    }

    public String getMissingParam() {
        return missingParam;
    }
}

