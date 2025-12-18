package com.taxi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с диспетчером, который создал заказ
    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    // Связь с водителем, который выполняет заказ
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // Связь с автомобилем, на котором выполняется заказ
    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;

    // Связь с путевым листом (доработать)
    @ManyToOne
    @JoinColumn(name = "waybill_id")
    private Waybill waybill;

    // Информация о клиенте
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    // Адреса
    @Column(name = "pickup_address", nullable = false, length = 200)
    private String pickupAddress;

    @Column(name = "destination_address", length = 200)
    private String destinationAddress;

    // Временные метки
    @Column(name = "order_time", nullable = false)
    private LocalDateTime orderTime;

    @Column(name = "planned_pickup_time")
    private LocalDateTime plannedPickupTime;

    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    // Финансовая информация
    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "price")
    private Double price;

    // Статус заказа
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", length = 500)
    private String notes;

    // Конструкторы
    public Order() {
        this.orderTime = LocalDateTime.now();
        this.status = "NEW";
    }

    // Основной конструктор
    public Order(User operator, String pickupAddress, String customerPhone) {
        this();
        this.operator = operator;
        this.pickupAddress = pickupAddress;
        this.customerPhone = customerPhone;
    }

    // Полный конструктор
    public Order(User operator, String customerName, String customerPhone,
                 String pickupAddress, String destinationAddress) {
        this();
        this.operator = operator;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
    }


    /**
     * Назначить заказ водителю и автомобилю
     */
    public void assignToDriver(Driver driver, Car car) {
        this.driver = driver;
        this.car = car;
        this.status = "ASSIGNED";
        System.out.println(" Заказ #" + id + " назначен водителю: " +
                driver.getFullName() + " на авто: " + car.getDisplayName());
    }

    /**
     * Начать поездку (водитель прибыл к клиенту)
     */
    public void startTrip() {
        this.actualPickupTime = LocalDateTime.now();
        this.status = "IN_PROGRESS";
        System.out.println(" Заказ #" + id + " начал выполнение");
    }

    /**
     * Завершить заказ
     */
    public void completeOrder(Double distance, Double price) {
        this.completionTime = LocalDateTime.now();
        this.price = price;
        this.status = "COMPLETED";

        // Дистанция может быть null
        if (distance != null) {
            this.distanceKm = distance;
        }

        System.out.println("🏁 Заказ #" + id + " завершен. Стоимость: " + price + " руб.");
    }

    /**
     * Отменить заказ
     */
    public void cancelOrder(String reason) {
        this.status = "CANCELLED";
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Отменен: " + reason;
        System.out.println(" Заказ #" + id + " отменен. Причина: " + reason);
    }

    /**
     * Проверить, можно ли назначить заказ
     */
    public boolean canBeAssigned() {
        return "NEW".equals(this.status);
    }

    /**
     * Проверить, выполняется ли заказ сейчас
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(this.status);
    }

    // геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOperator() { return operator; }
    public void setOperator(User operator) { this.operator = operator; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public Waybill getWaybill() { return waybill; }
    public void setWaybill(Waybill waybill) { this.waybill = waybill; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }

    public LocalDateTime getCreatedAt() { return orderTime; }

    public LocalDateTime getPlannedPickupTime() { return plannedPickupTime; }
    public void setPlannedPickupTime(LocalDateTime plannedPickupTime) { this.plannedPickupTime = plannedPickupTime; }

    public LocalDateTime getActualPickupTime() { return actualPickupTime; }
    public void setActualPickupTime(LocalDateTime actualPickupTime) { this.actualPickupTime = actualPickupTime; }

    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }


    /**
     * Краткая информация о заказе
     */
    public String getDisplayInfo() {
        return "Заказ #" + id + " - " + pickupAddress +
                (destinationAddress != null ? " → " + destinationAddress : "") +
                " (" + getStatusDisplay() + ")";
    }

    /**
     * Русское отображение статуса
     */
    public String getStatusDisplay() {
        switch (status) {
            case "NEW": return "Новый";
            case "ASSIGNED": return "Назначен";
            case "IN_PROGRESS": return "В процессе";
            case "COMPLETED": return "Завершен";
            case "CANCELLED": return "Отменен";
            default: return status;
        }
    }

    /**
     * Информация для отладки
     */
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customer='" + customerName + '\'' +
                ", from='" + pickupAddress + '\'' +
                ", to='" + destinationAddress + '\'' +
                ", status=" + getStatusDisplay() +
                ", price=" + price +
                ", driver=" + (driver != null ? driver.getFullName() : "не назначен") +
                ", waybill=" + (waybill != null ? "#" + waybill.getId() : "не привязан") +
                '}';
    }
}