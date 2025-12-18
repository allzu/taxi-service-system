package com.taxi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String brand;           // Марка: Hyundai

    @Column(nullable = false, length = 50)
    private String model;           // Модель: Solaris

    @Column(name = "license_plate", unique = true, nullable = false, length = 15)
    private String licensePlate;    // Гос. номер: А111АА78

    @Column(name = "vin", length = 17)
    private String vin;             // VIN код

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture; // Год выпуска

    @Column(name = "color", length = 30)
    private String color;           // Цвет

    @Column(name = "is_active")
    private Boolean isActive = true; // Активен в системе

    @Column(name = "in_repair")
    private Boolean inRepair = false; // В ремонте

    @Column(name = "mileage_km")
    private Integer mileageKm; // Пробег в километрах

    // Технический статус автомобиля
    @Enumerated(EnumType.STRING)
    @Column(name = "technical_status")
    private TechnicalStatus technicalStatus = TechnicalStatus.UNKNOWN;

    // Связь с текущим водителем
    @OneToOne(mappedBy = "currentCar")
    private Driver currentDriver;

    // Конструкторы
    public Car() {
    }

    public Car(String brand, String model, String licensePlate, String vin) {
        this.brand = brand;
        this.model = model;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.technicalStatus = TechnicalStatus.UNKNOWN;
    }

    public Car(String brand, String model, String licensePlate, String vin,
               Integer yearOfManufacture, String color) {
        this(brand, model, licensePlate, vin);
        this.yearOfManufacture = yearOfManufacture;
        this.color = color;
        this.technicalStatus = TechnicalStatus.UNKNOWN;
    }

    // Бизнес-методы
    /**
     * Проверяет, исправен ли автомобиль
     */
    public boolean isOperational() {
        return technicalStatus == TechnicalStatus.OK
                && Boolean.TRUE.equals(isActive)
                && !Boolean.TRUE.equals(inRepair);
    }

    /**
     * Проверяет, свободен ли автомобиль
     */
    public boolean isAvailable() {
        return currentDriver == null;
    }

    /**
     * Проверяет, требуется ли техосмотр
     */
    public boolean needsInspection() {
        return technicalStatus == TechnicalStatus.NEEDS_INSPECTION
                || technicalStatus == TechnicalStatus.UNKNOWN;
    }

    /**
     * Обновляет технический статус после осмотра
     */
    public void updateTechnicalStatus(boolean passed) {
        this.technicalStatus = passed ? TechnicalStatus.OK : TechnicalStatus.NEEDS_REPAIR;
        this.inRepair = !passed;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public Integer getYearOfManufacture() { return yearOfManufacture; }
    public void setYearOfManufacture(Integer yearOfManufacture) { this.yearOfManufacture = yearOfManufacture; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getInRepair() { return inRepair; }
    public void setInRepair(Boolean inRepair) { this.inRepair = inRepair; }

    public Integer getMileageKm() { return mileageKm; }
    public void setMileageKm(Integer mileageKm) { this.mileageKm = mileageKm; }

    public TechnicalStatus getTechnicalStatus() { return technicalStatus; }
    public void setTechnicalStatus(TechnicalStatus technicalStatus) {
        this.technicalStatus = technicalStatus;
    }

    public Driver getCurrentDriver() { return currentDriver; }
    public void setCurrentDriver(Driver currentDriver) {
        this.currentDriver = currentDriver;
    }

    // Удобный метод для отображения в интерфейсе
    public String getDisplayName() {
        return brand + " " + model + " " + licensePlate;
    }

    public void addMileage(Integer additionalKm) {
        if (this.mileageKm == null) {
            this.mileageKm = additionalKm;
        } else {
            this.mileageKm += additionalKm;
        }
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                ", technicalStatus=" + technicalStatus +
                ", isAvailable=" + isAvailable() +
                '}';
    }

}

