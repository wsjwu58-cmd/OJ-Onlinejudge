package com.oj.enumeration;


public enum ActivityType {
    USER_REGISTER("USER_REGISTER", "用户注册"),
    USER_LOGIN("USER_LOGIN", "用户登录"),
    PROBLEM_SUBMIT("PROBLEM_SUBMIT", "题目提交"),
    PROBLEM_ACCEPT("PROBLEM_ACCEPT", "题目通过"),
    PROBLEM_CREATE("PROBLEM_CREATE", "题目创建"),
    GROUP_CREATE("GROUP_CREATE", "题单创建"),
    GROUP_UPDATE("GROUP_UPDATE", "题单更新");

    private final String code;
    private final String name;

    ActivityType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }


}
