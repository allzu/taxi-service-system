package com.taxi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "technical_inspections")
public class TechnicalInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "next_inspection_date")
    private LocalDateTime nextInspectionDate;

    // Конструкторы
    public TechnicalInspection() {
    }

    public TechnicalInspection(Car car, User mechanic, Boolean isPassed) {
        this.car = car;
        this.mechanic = mechanic;
        this.isPassed = isPassed;
        this.inspectionDate = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public User getMechanic() {
        return mechanic;
    }

    public void setMechanic(User mechanic) {
        this.mechanic = mechanic;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDateTime inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public Boolean getIsPassed() {
        return isPassed;
    }

    public void setIsPassed(Boolean isPassed) {
        this.isPassed = isPassed;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public void setMileageKm(Integer mileageKm) {
        this.mileageKm = mileageKm;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getNextInspectionDate() {
        return nextInspectionDate;
    }

    public void setNextInspectionDate(LocalDateTime nextInspectionDate) {
        this.nextInspectionDate = nextInspectionDate;
    }

    public String getDisplayName() {
        return car.getLicensePlate() + " - " +
                (isPassed ? " Исправен" : " Неисправен") + " - " +
                inspectionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}