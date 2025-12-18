package com.taxi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_checks")
public class MedicalCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "check_date", nullable = false)
    private LocalDateTime checkDate;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "opens_shift")
    private Boolean opensShift = false;

    // Конструкторы
    public MedicalCheck() {
        this.checkDate = LocalDateTime.now();
    }

    public MedicalCheck(Driver driver, User doctor, Boolean isPassed) {
        this();
        this.driver = driver;
        this.doctor = doctor;
        this.isPassed = isPassed;
    }

    // Бизнес-методы
    public void openShift() {
        if (this.isPassed) {
            this.opensShift = true;
            System.out.println(" Смена открыта для водителя: " + driver.getFullName());
        } else {
            System.out.println(" Нельзя открыть смену - водитель не прошел медосмотр");
        }
    }

    // Вспомогательные методы
    public boolean canDrive() {
        return Boolean.TRUE.equals(this.isPassed);
    }

    public String getStatusText() {
        return Boolean.TRUE.equals(isPassed) ? " Допущен" : " Не допущен";
    }

    public String getStatusClass() {
        return Boolean.TRUE.equals(isPassed) ? "status-passed" : "status-failed";
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public LocalDateTime getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDateTime checkDate) { this.checkDate = checkDate; }
    public Boolean getIsPassed() { return isPassed; }
    public void setIsPassed(Boolean isPassed) { this.isPassed = isPassed; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getOpensShift() { return opensShift; }
    public void setOpensShift(Boolean opensShift) { this.opensShift = opensShift; }

    @Override
    public String toString() {
        return "MedicalCheck{" +
                "id=" + id +
                ", driver=" + driver.getFullName() +
                ", doctor=" + doctor.getFullName() +
                ", isPassed=" + isPassed +
                ", opensShift=" + opensShift +
                '}';
    }
}