package com.taxi.permission;

import com.taxi.entity.User;

/**
 * Права доступа к страницам системы
 */
public class PagePermission {

    // Список страниц системы
    public enum Page {
        CARS,           // Автомобили
        DRIVERS,        // Водители
        ORDERS,         // Заказы
        WAYBILLS,       // Путевые листы
        INSPECTIONS,    // Техосмотры
        MEDICAL_CHECKS, // Медосмотры
        ADMIN_PANEL,    // Панель админа
        DISPATCHER_PANEL, // Панель диспетчера
        DOCTOR_PANEL,   // Панель врача
        MECHANIC_PANEL, // Панель механика
        DRIVER_PANEL    // Панель водителя
    }

    // Уровни доступа
    public enum AccessLevel {
        NONE,       //  Нет доступа
        VIEW,       //  Только просмотр
        EDIT        //  Полный доступ
    }

    /**
     * Проверяет доступ пользователя к странице
     */
    public static AccessLevel getAccessLevel(User user, Page page) {
        if (user == null) return AccessLevel.NONE;

        String userType = user.getUserType();

        // ADMIN - полный доступ ко всему
        if ("ADMIN".equals(userType)) {
            return AccessLevel.EDIT;
        }

        // Проверка по ролям и страницам
        switch (page) {
            case ORDERS:
                if ("OPERATOR".equals(userType)) return AccessLevel.EDIT;
                if ("DRIVER".equals(userType)) return AccessLevel.VIEW;
                return AccessLevel.VIEW; // Остальные - только просмотр

            case CARS:
                if ("MECHANIC".equals(userType)) return AccessLevel.EDIT;
                return AccessLevel.VIEW; // Остальные - только просмотр

            case DRIVERS:
                if ("MECHANIC".equals(userType)) return AccessLevel.EDIT;
                return AccessLevel.VIEW; // Остальные - только просмотр

            case MEDICAL_CHECKS:
                if ("DOCTOR".equals(userType)) return AccessLevel.EDIT;
                return AccessLevel.VIEW; // Остальные - только просмотр

            case INSPECTIONS:
            case WAYBILLS:
                if ("MECHANIC".equals(userType)) return AccessLevel.EDIT;
                return AccessLevel.VIEW; // Остальные - только просмотр

            case ADMIN_PANEL:
                return "ADMIN".equals(userType) ? AccessLevel.EDIT : AccessLevel.NONE;

            case DISPATCHER_PANEL:
                return "OPERATOR".equals(userType) ? AccessLevel.EDIT : AccessLevel.NONE;

            case DOCTOR_PANEL:
                return "DOCTOR".equals(userType) ? AccessLevel.EDIT : AccessLevel.NONE;

            case MECHANIC_PANEL:
                return "MECHANIC".equals(userType) ? AccessLevel.EDIT : AccessLevel.NONE;

            case DRIVER_PANEL:
                return "DRIVER".equals(userType) ? AccessLevel.EDIT : AccessLevel.NONE;

            default:
                return AccessLevel.VIEW;
        }
    }

    /**
     * Проверяет, может ли пользователь просматривать страницу
     */
    public static boolean canView(User user, Page page) {
        return getAccessLevel(user, page) != AccessLevel.NONE;
    }

    /**
     * Проверяет, может ли пользователь редактировать на странице
     */
    public static boolean canEdit(User user, Page page) {
        return getAccessLevel(user, page) == AccessLevel.EDIT;
    }
}