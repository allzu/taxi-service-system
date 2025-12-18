package com.taxi.util;

import com.taxi.entity.User;
import com.taxi.permission.PagePermission;
import com.taxi.permission.PagePermission.Page;
import com.taxi.permission.PagePermission.AccessLevel;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Утилиты для работы с аутентификацией и правами
 */
public class AuthUtil {

    /**
     * Получает текущего пользователя из сессии
     */
    public static User getCurrentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute("user");
    }

    /**
     * Проверяет авторизацию
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    /**
     * Проверяет, авторизован ли пользователь и имеет доступ к странице
     */
    public static boolean hasAccess(HttpServletRequest request, Page page) {
        User user = getCurrentUser(request);
        return PagePermission.canView(user, page);
    }

    /**
     * Проверяет роль пользователя
     */
    public static boolean hasRole(HttpServletRequest request, String role) {
        User user = getCurrentUser(request);
        return user != null && role.equals(user.getUserType());
    }

    /**
     * Возвращает уровень доступа к странице
     */
    public static AccessLevel getPageAccessLevel(HttpServletRequest request, Page page) {
        User user = getCurrentUser(request);
        return PagePermission.getAccessLevel(user, page);
    }
}