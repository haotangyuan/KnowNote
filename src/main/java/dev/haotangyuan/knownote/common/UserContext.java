package dev.haotangyuan.knownote.common;

/**
 * 用户上下文，ThreadLocal 存储当前登录用户信息
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static boolean isLoggedIn() {
        return USER_ID.get() != null;
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
