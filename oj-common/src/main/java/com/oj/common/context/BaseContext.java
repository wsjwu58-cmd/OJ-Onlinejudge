package com.oj.common.context;

public class BaseContext {
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    public static ThreadLocal<String> roleThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

    public static void setCurrentRole(String role) {
        roleThreadLocal.set(role);
    }

    public static String getCurrentRole() {
        return roleThreadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
        roleThreadLocal.remove();
    }
}
