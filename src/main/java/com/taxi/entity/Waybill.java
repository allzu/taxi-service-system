package com.taxi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "waybills")
public class Waybill {

    public enum WaybillStatus {
        ACTIVE("Активна"),
        COMPLETED("Завершена"),
        CANCELLED("Отменена");

        private final String displayName;

        WaybillStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с водителем
    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // Связь с автомобилем
    @ManyToOne
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne
    @JoinColumn(name = "mechanic_id")
    private User mechanic;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WaybillStatus status; // Используем enum вместо строки

    @Column(name = "initial_mileage_km")
    private Integer initialMileageKm; // Пробег в начале смены

    @Column(name = "final_mileage_km")
    private Integer finalMileageKm; // Пробег в конце смены

    @Column(name = "total_earnings")
    private Double totalEarnings = 0.0; // Общий заработок за смену

    @Column(name = "orders_count")
    private Integer ordersCount = 0; // Количество выполненных заказов

    @Column(name = "total_distance")
    private Double totalDistance = 0.0; // Общая дистанция всех заказов

    @Column(name = "total_revenue")
    private Double totalRevenue = 0.0; // Общая выручка от заказов

    @Column(name = "notes", length = 500)
    private String notes;

    @Transient
    private List<Order> orders = new ArrayList<>();

    // Конструкторы
    public Waybill() {
        this.startTime = LocalDateTime.now();
        this.status = WaybillStatus.ACTIVE;
        // Гарантируем инициализацию полей
        ensureNonNullFields();
    }

    public Waybill(Driver driver, Car car, User doctor) {
        this();
        this.driver = driver;
        this.car = car;
        this.doctor = doctor;
    }

    public Waybill(Driver driver, Car car, User doctor, Integer initialMileage) {
        this(driver, car, doctor);
        this.initialMileageKm = initialMileage;
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    /**
     * Гарантирует, что числовые поля не null
     */
    private void ensureNonNullFields() {
        if (this.totalEarnings == null) this.totalEarnings = 0.0;
        if (this.totalDistance == null) this.totalDistance = 0.0;
        if (this.totalRevenue == null) this.totalRevenue = 0.0;
        if (this.ordersCount == null) this.ordersCount = 0;
    }

    /**
     * Безопасные геттеры для числовых полей
     */
    public Double getSafeTotalDistance() {
        return totalDistance != null ? totalDistance : 0.0;
    }

    public Double getSafeTotalRevenue() {
        return totalRevenue != null ? totalRevenue : 0.0;
    }

    public Double getSafeTotalEarnings() {
        return totalEarnings != null ? totalEarnings : 0.0;
    }

    public Integer getSafeOrdersCount() {
        return ordersCount != null ? ordersCount : 0;
    }

    // БИЗНЕС-МЕТОДЫ

    /**
     * Закрыть путевой лист (завершить смену)
     */
    public void completeWaybill(User mechanic, Integer finalMileage, Double earnings, String notes) {
        ensureNonNullFields();

        this.endTime = LocalDateTime.now();
        this.mechanic = mechanic;
        this.finalMileageKm = finalMileage;

        // Обновляем общий заработок
        if (earnings != null) {
            this.totalEarnings = earnings;
        } else if (this.totalRevenue > 0) {
            this.totalEarnings = this.totalRevenue;
        }

        this.notes = notes != null ? notes : this.notes;
        this.status = WaybillStatus.COMPLETED;

        // Обновляем пробег автомобиля
        if (car != null && finalMileage != null) {
            car.setMileageKm(finalMileage);
        }

        System.out.println(" Путевой лист #" + id + " завершен");
        System.out.println("    Водитель: " + driver.getFullName());
        System.out.println("    Автомобиль: " + car.getLicensePlate() + " (" + car.getModel() + ")");
        System.out.println("    Пробег за смену: " + getShiftMileage() + " км");
        System.out.println("    Заказов выполнено: " + ordersCount);
        System.out.println("    Общая дистанция: " + totalDistance + " км");
        System.out.println("    Заработок: " + totalEarnings + " руб.");
        System.out.println("     Продолжительность: " + getShiftDuration());
    }

    /**
     * Отменить путевой лист
     */
    public void cancelWaybill(String reason) {
        this.endTime = LocalDateTime.now();
        this.status = WaybillStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "Отменено: " + reason;

        System.out.println(" Путевой лист #" + id + " отменен");
        System.out.println("   Причина: " + reason);
    }

    /**
     * Рассчитать пробег за смену
     */
    public int getShiftMileage() {
        if (initialMileageKm != null && finalMileageKm != null) {
            return finalMileageKm - initialMileageKm;
        }
        return 0;
    }

    /**
     * Проверить, активна ли смена
     */
    public boolean isActive() {
        return WaybillStatus.ACTIVE.equals(this.status);
    }

    /**
     * Проверить, завершена ли смена
     */
    public boolean isCompleted() {
        return WaybillStatus.COMPLETED.equals(this.status);
    }

    /**
     * Проверить, отменена ли смена
     */
    public boolean isCancelled() {
        return WaybillStatus.CANCELLED.equals(this.status);
    }

    /**
     * Получить продолжительность смены в часах и минутах
     */
    public String getShiftDuration() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        long hours = java.time.Duration.between(startTime, end).toHours();
        long minutes = java.time.Duration.between(startTime, end).toMinutes() % 60;
        return hours + "ч " + minutes + "м";
    }

    /**
     * Получить продолжительность смены в минутах
     */
    public long getShiftDurationMinutes() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    /**
     * Получить текстовое описание статуса
     */
    public String getStatusText() {
        return status != null ? status.getDisplayName() : "Неизвестно";
    }

    /**
     * Получить начальный пробег (alias для совместимости)
     */
    public Integer getInitialMileage() {
        return initialMileageKm;
    }

    /**
     * Получить конечный пробег (alias для совместимости)
     */
    public Integer getFinalMileage() {
        return finalMileageKm;
    }


    /**
     * Добавить заказ в статистику
     */
    public void addOrder(Order order) {
        if (order == null) return;
        ensureNonNullFields();

        // Добавляем в список заказов
        orders.add(order);

        // Обновляем счетчик
        ordersCount = orders.size();

        System.out.println("📦 Заказ #" + order.getId() + " добавлен в путевой лист #" + id);
    }

    /**
     * Обновить статистику при завершении заказа
     */
    public void updateStats(Double distance, Double revenue) {
        ensureNonNullFields();

        if (distance != null && distance > 0) {
            totalDistance += distance;
        }
        if (revenue != null && revenue > 0) {
            totalRevenue += revenue;
            totalEarnings += revenue;
        }

        System.out.println("📊 Статистика путевого листа #" + id + " обновлена:");
        System.out.println("   • Заказов: " + ordersCount);
        System.out.println("   • Дистанция: " + totalDistance + " км");
        System.out.println("   • Выручка: " + totalRevenue + " руб");
        System.out.println("   • Заработок: " + totalEarnings + " руб");
    }

    /**
     * Получить среднюю стоимость заказа
     */
    public Double getAverageOrderPrice() {
        ensureNonNullFields();
        if (ordersCount > 0 && totalRevenue != null) {
            return totalRevenue / ordersCount;
        }
        return 0.0;
    }

    /**
     * Получить среднюю дистанцию заказа
     */
    public Double getAverageOrderDistance() {
        ensureNonNullFields();
        if (ordersCount > 0 && totalDistance != null) {
            return totalDistance / ordersCount;
        }
        return 0.0;
    }

    /**
     * Получить доход в час
     */
    public Double getRevenuePerHour() {
        ensureNonNullFields();
        long minutes = getShiftDurationMinutes();
        if (minutes > 0 && totalRevenue > 0) {
            double hours = minutes / 60.0;
            return totalRevenue / hours;
        }
        return 0.0;
    }

    /**
     * Получить детальную статистику
     */
    public String getDetailedStats() {
        ensureNonNullFields();
        return String.format(
                "📊 Статистика путевого листа #%d:\n" +
                        "   👤 Водитель: %s\n" +
                        "   🚗 Автомобиль: %s\n" +
                        "   ⏱️  Продолжительность: %s\n" +
                        "   📦 Выполнено заказов: %d\n" +
                        "   📏 Общая дистанция: %.1f км\n" +
                        "   💰 Общая выручка: %.2f руб\n" +
                        "   📊 Средний чек: %.2f руб\n" +
                        "   📊 Средняя дистанция: %.1f км\n" +
                        "   ⏳ Доход в час: %.2f руб/час",
                id,
                driver != null ? driver.getFullName() : "Неизвестно",
                car != null ? car.getLicensePlate() : "Неизвестно",
                getShiftDuration(),
                ordersCount,
                totalDistance,
                totalRevenue,
                getAverageOrderPrice(),
                getAverageOrderDistance(),
                getRevenuePerHour()
        );
    }

    // геттеры сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public User getMechanic() { return mechanic; }
    public void setMechanic(User mechanic) { this.mechanic = mechanic; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public WaybillStatus getStatus() { return status; }
    public void setStatus(WaybillStatus status) { this.status = status; }

    public Integer getInitialMileageKm() { return initialMileageKm; }
    public void setInitialMileageKm(Integer initialMileageKm) {
        this.initialMileageKm = initialMileageKm;
    }

    public Integer getFinalMileageKm() { return finalMileageKm; }
    public void setFinalMileageKm(Integer finalMileageKm) {
        this.finalMileageKm = finalMileageKm;
    }

    public Double getTotalEarnings() {
        ensureNonNullFields();
        return totalEarnings;
    }
    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings != null ? totalEarnings : 0.0;
    }

    public Integer getOrdersCount() {
        ensureNonNullFields();
        return ordersCount;
    }
    public void setOrdersCount(Integer ordersCount) {
        this.ordersCount = ordersCount != null ? ordersCount : 0;
    }

    public Double getTotalDistance() {
        ensureNonNullFields();
        return totalDistance;
    }
    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance != null ? totalDistance : 0.0;
    }

    public Double getTotalRevenue() {
        ensureNonNullFields();
        return totalRevenue;
    }
    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : 0.0;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    @Override
    public String toString() {
        ensureNonNullFields();
        return "Waybill{" +
                "id=" + id +
                ", driver=" + (driver != null ? driver.getFullName() : "null") +
                ", car=" + (car != null ? car.getLicensePlate() : "null") +
                ", status=" + status +
                ", ordersCount=" + ordersCount +
                ", totalRevenue=" + totalRevenue +
                ", duration=" + getShiftDuration() +
                '}';
    }

    /**
     * Краткая информация о путевом листе
     */
    public String getDisplayInfo() {
        ensureNonNullFields();
        return String.format(
                "Путевой лист #%d | %s | %s | Заказов: %d | Выручка: %.2f руб",
                id,
                driver != null ? driver.getFullName() : "Без водителя",
                getStatusText(),
                ordersCount,
                totalRevenue
        );
    }
}