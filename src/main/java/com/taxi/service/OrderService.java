package com.taxi.service;

import com.taxi.entity.*;
import com.taxi.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private OrderRepository orderRepository = new OrderRepository();
    private DriverRepository driverRepository = new DriverRepository();
    private CarRepository carRepository = new CarRepository();
    private UserRepository userRepository = new UserRepository();
    private WaybillRepository waybillRepository = new WaybillRepository();
    private WaybillService waybillService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public void createOrder(Order order) {
        orderRepository.save(order);
    }

    public void updateOrder(Order order) {
        orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        orderRepository.delete(id);
    }

    /**
     * Получить активные заказы (не завершенные и не отмененные)
     * Для панели диспетчера
     */
    public List<Order> getDispatcherActiveOrders() {
        try {
            // Получаем все заказы
            List<Order> allOrders = getAllOrders();
            if (allOrders == null || allOrders.isEmpty()) {
                return new ArrayList<>();
            }

            // Фильтруем заказы: оставляем только те, которые НЕ завершены и НЕ отменены
            List<Order> activeOrders = new ArrayList<>();
            for (Order order : allOrders) {
                String status = order.getStatus();
                if (status != null &&
                        !status.equalsIgnoreCase("COMPLETED") &&
                        !status.equalsIgnoreCase("CANCELLED")) {
                    activeOrders.add(order);
                }
            }

            return activeOrders;
        } catch (Exception e) {
            System.out.println(" Ошибка при получении активных заказов: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // МЕТОДЫ ДЛЯ ВОДИТЕЛЕЙ

    /**
     * Получить заказы водителя
     */
    public List<Order> getDriverOrders(Long driverId) {
        return orderRepository.findByDriverId(driverId);
    }

    /**
     * Получить активные заказы водителя
     */
    public List<Order> getActiveOrdersForDriver(Long driverId) {
        return orderRepository.findActiveByDriverId(driverId);
    }

    /**
     * Получить общее количество заказов для водителя
     */
    public long getTotalOrdersForDriver(Long driverId) {
        List<Order> orders = getDriverOrders(driverId);
        return orders != null ? orders.size() : 0;
    }

    /**
     * Получить количество завершенных заказов для водителя
     */
    public long getCompletedOrdersForDriver(Long driverId) {
        List<Order> orders = getDriverOrders(driverId);
        if (orders == null) return 0;
        return orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .count();
    }

    // === МЕТОДЫ ДЛЯ СТАТИСТИКИ ===

    /**
     * Получить общее количество заказов
     */
    public long getTotalOrders() {
        List<Order> orders = getAllOrders();
        return orders != null ? orders.size() : 0;
    }

    /**
     * Получить количество активных заказов
     */
    public long getActiveOrdersCount() {
        List<Order> activeOrders = orderRepository.findByStatus("IN_PROGRESS");
        return activeOrders != null ? activeOrders.size() : 0;
    }

    /**
     * Получить общую выручку
     */
    public double getTotalRevenue() {
        List<Order> orders = getAllOrders();
        if (orders == null) return 0.0;
        return orders.stream()
                .filter(o -> o.getPrice() != null && "COMPLETED".equals(o.getStatus()))
                .mapToDouble(Order::getPrice)
                .sum();
    }

    /**
     * Получить количество заказов за сегодня
     */
    public long getTodayOrdersCount() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Order> orders = getAllOrders();
        if (orders == null) return 0;

        return orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(todayStart) && !o.getCreatedAt().isAfter(todayEnd))
                .count();
    }

    /**
     * Получить количество завершенных заказов за сегодня
     */
    public long getCompletedTodayCount() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Order> orders = getAllOrders();
        if (orders == null) return 0;

        return orders.stream()
                .filter(o -> o.getCreatedAt() != null && "COMPLETED".equals(o.getStatus()))
                .filter(o -> !o.getCreatedAt().isBefore(todayStart) && !o.getCreatedAt().isAfter(todayEnd))
                .count();
    }

    /**
     * Получить выручку за сегодня
     */
    public double getTodayRevenue() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Order> orders = getAllOrders();
        if (orders == null) return 0.0;

        return orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()) && o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(todayStart) && !o.getCreatedAt().isAfter(todayEnd))
                .filter(o -> o.getPrice() != null)
                .mapToDouble(Order::getPrice)
                .sum();
    }

    /**
     * Получить среднее время выполнения заказа сегодня
     */
    public double getAverageOrderTimeToday() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Order> orders = getAllOrders();
        if (orders == null) return 0.0;

        List<Order> todayOrders = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()) &&
                        o.getCreatedAt() != null &&
                        o.getCompletionTime() != null)
                .filter(o -> !o.getCreatedAt().isBefore(todayStart) && !o.getCreatedAt().isAfter(todayEnd))
                .toList();

        if (todayOrders.isEmpty()) return 0.0;

        double totalMinutes = 0;
        int count = 0;

        for (Order order : todayOrders) {
            try {
                long minutes = ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getCompletionTime());
                totalMinutes += minutes;
                count++;
            } catch (Exception e) {
                // Пропускаем заказы с ошибками расчета времени
            }
        }

        return count > 0 ? totalMinutes / count : 0.0;
    }

    // ОСНОВНЫЕ БИЗНЕС-МЕТОДЫ

    /**
     * 1. Диспетчер принимает заказ от клиента
     */
    public Order createNewOrder(User dispatcher, String customerName, String customerPhone,
                                String pickupAddress, String destinationAddress) {
        // Проверяем, что пользователь - диспетчер
        if (!"OPERATOR".equals(dispatcher.getUserType())) {
            throw new RuntimeException("Только оператор (диспетчер) может создавать заказы. Роль: " + dispatcher.getUserType());
        }

        // Очистка и проверка телефона
        if (customerPhone != null) {
            String cleanedPhone = customerPhone.trim();
            cleanedPhone = cleanedPhone.replaceAll("[^0-9+]", "");
            if (cleanedPhone.length() > 20) {
                cleanedPhone = cleanedPhone.substring(0, 20);
                System.out.println("⚠  Номер телефона укорочен до 20 символов: " + cleanedPhone);
            }
            customerPhone = cleanedPhone;
        }

        // Проверяем обязательные поля
        if (customerPhone == null || customerPhone.isEmpty()) {
            throw new IllegalArgumentException("Номер телефона обязателен");
        }
        if (pickupAddress == null || pickupAddress.isEmpty()) {
            throw new IllegalArgumentException("Адрес подачи обязателен");
        }

        // Создаем заказ
        Order order = new Order();
        order.setOperator(dispatcher);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setPickupAddress(pickupAddress);
        order.setDestinationAddress(destinationAddress);
        order.setStatus("NEW");
        order.setOrderTime(LocalDateTime.now());

        orderRepository.save(order);

        System.out.println(" Диспетчер " + dispatcher.getFullName() +
                " создал новый заказ #" + order.getId());
        System.out.println("    Клиент: " + (customerName != null ? customerName : "Не указано"));
        System.out.println("    Телефон: " + customerPhone);
        System.out.println("    Откуда: " + pickupAddress);
        System.out.println("    Куда: " + (destinationAddress != null ? destinationAddress : "Не указано"));

        return order;
    }

    /**
     * 2. Диспетчер назначает заказ на водителя
     */
    public boolean assignOrderToDriver(Long orderId, Long driverId) {
        try {
            Order order = orderRepository.findById(orderId);
            Driver driver = driverRepository.findById(driverId);

            if (order == null || driver == null) {
                System.out.println(" Заказ или водитель не найдены");
                return false;
            }

            // Проверяем, есть ли у водителя активный путевой лист
            Waybill activeWaybill = waybillRepository.findActiveByDriverId(driverId);
            if (activeWaybill == null) {
                System.out.println("️ У водителя " + driver.getFullName() + " нет активного путевого листа");
                // Можно создать автоматически или просто предупредить
                // Для начала просто предупредим
            } else {
                System.out.println(" Водитель имеет активный путевой лист #" + activeWaybill.getId());
            }

            // Назначаем заказ
            order.setDriver(driver);
            order.setStatus("ASSIGNED");
            orderRepository.save(order);

            System.out.println(" Заказ #" + orderId + " назначен водителю " + driver.getFullName());
            return true;
        } catch (Exception e) {
            System.out.println(" Ошибка при назначении заказа: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 3. Водитель начинает выполнение заказа
     */
    public boolean startOrderExecution(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            order.setStatus("IN_PROGRESS");
            order.setActualPickupTime(LocalDateTime.now());
            orderRepository.save(order);

            System.out.println(" Заказ #" + orderId + " начал выполнение");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 4. Водитель завершает заказ
     */
    public boolean completeOrder(Long orderId, Double distanceKm, Double price) {
        try {
            Order order = orderRepository.findById(orderId);
            if (order == null) {
                System.out.println(" Заказ не найден: " + orderId);
                return false;
            }

            // Завершаем заказ
            order.setStatus("COMPLETED");
            order.setPrice(price);
            order.setCompletionTime(LocalDateTime.now());

            // Дистанция может быть null
            if (distanceKm != null) {
                order.setDistanceKm(distanceKm);
            }

            System.out.println(" Заказ #" + orderId + " завершен");
            System.out.println("    Стоимость: " + price + " руб.");
            if (distanceKm != null) {
                System.out.println("    Дистанция: " + distanceKm + " км");
            }

            orderRepository.save(order);

            return true;

        } catch (Exception e) {
            System.out.println(" Ошибка при завершении заказа: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обновляет статистику путевого листа при завершении заказа
     */
    private void updateWaybillStats(Waybill waybill, Double distanceKm, Double price) {
        try {
            // Получаем текущие значения
            Double currentDistance = waybill.getTotalDistance() != null ? waybill.getTotalDistance() : 0.0;
            Double currentRevenue = waybill.getTotalRevenue() != null ? waybill.getTotalRevenue() : 0.0;
            Integer currentOrders = waybill.getOrdersCount() != null ? waybill.getOrdersCount() : 0;

            // Обновляем
            waybill.setTotalDistance(currentDistance + (distanceKm != null ? distanceKm : 0.0));
            waybill.setTotalRevenue(currentRevenue + (price != null ? price : 0.0));
            waybill.setOrdersCount(currentOrders + 1);

            // Сохраняем
            waybillRepository.update(waybill);

            System.out.println(" Обновлена статистика путевого листа #" + waybill.getId());
            System.out.println("    Дистанция: " + waybill.getTotalDistance() + " км");
            System.out.println("    Выручка: " + waybill.getTotalRevenue() + " руб.");
            System.out.println("    Заказов: " + waybill.getOrdersCount());

        } catch (Exception e) {
            System.out.println("⚠ Не удалось обновить статистику путевого листа: " + e.getMessage());
        }
    }

    /**
     * 5. Отменить заказ
     */
    public boolean cancelOrder(Long orderId, String reason) {
        try {
            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            System.out.println(" Заказ #" + orderId + " отменен. Причина: " + reason);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    /**
     * Получить новые заказы (для диспетчера)
     */
    public List<Order> getNewOrders() {
        List<Order> newOrders = orderRepository.findNewOrders();
        return newOrders != null ? newOrders : List.of();
    }

    /**
     * Получить статистику водителя
     */
    public String getDriverStats(Long driverId) {
        try {
            Object[] stats = orderRepository.getDriverStats(driverId);
            if (stats == null || stats.length < 3) {
                return "Статистика временно недоступна";
            }

            long orderCount = (Long) stats[0];
            Double totalDistance = (Double) (stats[1] != null ? stats[1] : 0.0);
            Double totalRevenue = (Double) (stats[2] != null ? stats[2] : 0.0);

            return String.format(
                    " Статистика водителя:\n" +
                            "    Завершено заказов: %d\n" +
                            "    Общая дистанция: %.1f км\n" +
                            "    Общая выручка: %.2f руб\n" +
                            "    Средний чек: %.2f руб",
                    orderCount,
                    totalDistance,
                    totalRevenue,
                    orderCount > 0 ? totalRevenue / orderCount : 0.0
            );
        } catch (Exception e) {
            return "Статистика временно недоступна";
        }
    }

    /**
     * Найти доступных водителей для заказа
     */
    public List<Driver> getAvailableDrivers() {
        try {
            List<Driver> allDrivers = driverRepository.findAll();
            if (allDrivers == null) return List.of();

            return allDrivers.stream()
                    .filter(driver -> driver.getIsActive() != null && driver.getIsActive())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Автоматически назначить заказ на первого доступного водителя
     */
    public boolean autoAssignOrder(Long orderId) {
        List<Driver> availableDrivers = getAvailableDrivers();
        if (availableDrivers.isEmpty()) {
            System.out.println(" Нет доступных водителей для заказа #" + orderId);
            return false;
        }

        // Выбираем первого доступного водителя
        Driver driver = availableDrivers.get(0);
        return assignOrderToDriver(orderId, driver.getId());
    }

    /**
     * Получить заказы водителя за сегодня
     */
    public List<Order> getDriverOrdersToday(Long driverId) {
        try {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            return orderRepository.findByDriverAndPeriod(driverId, todayStart, todayEnd);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Получить активные заказы
     */
    public List<Order> getActiveOrders() {
        List<Order> activeOrders = orderRepository.findByStatus("IN_PROGRESS");
        return activeOrders != null ? activeOrders : List.of();
    }

    /**
     * Получить заказы по статусу
     */
    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders != null ? orders : List.of();
    }

    public void testOrderSave() {
        System.out.println("\n=== ТЕСТ СОХРАНЕНИЯ ЗАКАЗА ===");

        try {
            // Создаем тестового пользователя-оператора
            User operator = new User();
            operator.setId(1L); // Замени на реальный ID оператора из базы
            operator.setFullName("Тест Оператор");
            operator.setUserType("OPERATOR");

            System.out.println("1. Пробуем создать заказ...");
            Order order = new Order();
            order.setOperator(operator);
            order.setCustomerName("Тест Клиент");
            order.setCustomerPhone("+79991112233");
            order.setPickupAddress("Улица Тестовая, 1");
            order.setDestinationAddress("Проспект Тестовый, 2");
            order.setStatus("NEW");
            order.setOrderTime(LocalDateTime.now());

            System.out.println("2. Пробуем сохранить через репозиторий...");
            orderRepository.save(order);

            System.out.println("3. ID заказа после сохранения: " + order.getId());

            // Проверяем, сохранился ли
            Order savedOrder = orderRepository.findById(order.getId());
            if (savedOrder != null) {
                System.out.println(" ЗАКАЗ УСПЕШНО СОХРАНЕН!");
                System.out.println("   ID: " + savedOrder.getId());
                System.out.println("   Клиент: " + savedOrder.getCustomerName());
                System.out.println("   Телефон: " + savedOrder.getCustomerPhone());
                System.out.println("   Статус: " + savedOrder.getStatus());
            } else {
                System.out.println(" ЗАКАЗ НЕ СОХРАНИЛСЯ В БД!");
            }

        } catch (Exception e) {
            System.out.println(" ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== КОНЕЦ ТЕСТА ===\n");
    }

    public List<Order> getOrdersByWaybillId(Long waybillId) {
        try {
            return orderRepository.findByWaybillId(waybillId);
        } catch (Exception e) {
            System.out.println("Ошибка при получении заказов по путевому листу: " + e.getMessage());
            return List.of();
        }
    }

    //  МЕТОДЫ ДЛЯ РАБОТЫ С WAYBILL SERVICE (если понадобятся)

    /**
     * Lazy-инициализация WaybillService (создаем только при необходимости)
     */
    private WaybillService getWaybillService() {
        if (waybillService == null) {
            waybillService = new WaybillService();
        }
        return waybillService;
    }

    /**
     * Получить сводку по путевому листу (использует WaybillService если нужно)
     */
    public String getWaybillSummary(Long waybillId) {
        try {
            List<Order> waybillOrders = getOrdersByWaybillId(waybillId);
            if (waybillOrders.isEmpty()) {
                return "В путевом листе нет заказов";
            }

            double totalDistance = 0.0;
            double totalRevenue = 0.0;

            for (Order order : waybillOrders) {
                if (order.getDistanceKm() != null) {
                    totalDistance += order.getDistanceKm();
                }
                if (order.getPrice() != null) {
                    totalRevenue += order.getPrice();
                }
            }

            return String.format(" Сводка по путевому листу #%d:\n" +
                            "    Количество заказов: %d\n" +
                            "    Общая дистанция: %.1f км\n" +
                            "    Общая выручка: %.2f руб.\n" +
                            "    Средний чек: %.2f руб.",
                    waybillId, waybillOrders.size(), totalDistance, totalRevenue,
                    waybillOrders.size() > 0 ? totalRevenue / waybillOrders.size() : 0.0);
        } catch (Exception e) {
            return "Не удалось получить сводку по путевому листу: " + e.getMessage();
        }
    }
}