package com.example.photomanager.auth;

public final class AuthContext {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE_CODE = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setRoleCode(int roleCode) {
        ROLE_CODE.set(roleCode);
    }

    public static int getRoleCode() {
        Integer v = ROLE_CODE.get();
        return v == null ? 0 : v;
    }

    public static void clear() {
        USER_ID.remove();
        ROLE_CODE.remove();
    }
}
