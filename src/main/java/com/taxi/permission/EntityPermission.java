package com.taxi.permission;

import com.taxi.entity.*;

/**
 * Права доступа к конкретным сущностям (заказам, автомобилям и т.д.)
 */
public class EntityPermission {

    /**
     * Проверяет, может ли пользователь завершить заказ
     */
    public static boolean canCompleteOrder(User user, Order order) {
        if (user == null || order == null) return false;

        String userType = user.getUserType();

        // ADMIN может завершить ЛЮБОЙ заказ
        if ("ADMIN".equals(userType)) {
            return true;
        }

        // DRIVER может завершить ТОЛЬКО СВОЙ заказ
        if ("DRIVER".equals(userType)) {
            if (order.getDriver() == null) return false;

            // Проверяем, что пользователь - этот водитель
            return order.getDriver().getId().equals(user.getId());
        }

        return false;
    }

    /**
     * Проверяет, может ли пользователь назначить водителя на заказ
     */
    public static boolean canAssignDriver(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "OPERATOR".equals(userType);
    }

    /**
     * Проверяет, может ли пользователь создать заказ
     */
    public static boolean canCreateOrder(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "OPERATOR".equals(userType);
    }

    /**
     * Проверяет, может ли пользователь отменить заказ
     */
    public static boolean canCancelOrder(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "OPERATOR".equals(userType);
    }

    /**
     * Проверяет, может ли пользователь редактировать автомобиль
     */
    public static boolean canEditCar(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "MECHANIC".equals(userType);
    }

    /**
     * Проверяет, может ли пользователь редактировать медосмотр
     */
    public static boolean canEditMedicalCheck(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "DOCTOR".equals(userType);
    }

    /**
     * Проверяет, может ли пользователь назначать автомобиль водителю
     */
    public static boolean canAssignCarToDriver(User user) {
        if (user == null) return false;

        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "MECHANIC".equals(userType);
    }


    /**
     * Проверяет, может ли пользователь видеть все заказы (не только свои)
     */
    public static boolean canViewAllOrders(User user) {
        if (user == null) return false;
        String userType = user.getUserType();
        return "ADMIN".equals(userType) || "OPERATOR".equals(userType);
    }
}