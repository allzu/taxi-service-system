package com.taxi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "license_number", unique = true, nullable = false, length = 20)
    private String licenseNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "medical_status")
    private MedicalStatus medicalStatus = MedicalStatus.PENDING;

    @Column(name = "last_medical_check")
    private LocalDateTime lastMedicalCheck;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car currentCar;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalCheck> medicalChecks;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Waybill> waybills;

    // Конструкторы
    public Driver() {
    }

    public Driver(String fullName, String licenseNumber, String phone) {
        this.fullName = fullName;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.medicalStatus = MedicalStatus.PENDING;
    }

    // Бизнес-методы
    /**
     * Проверяет, допущен ли водитель к работе
     */
    public boolean isAllowedToWork() {
        return medicalStatus == MedicalStatus.PASSED;
    }

    /**
     * Проверяет, есть ли у водителя активный путевой лист
     * ИСПРАВЛЕНО: Используем вложенный enum из Waybill
     */
    public boolean hasActiveWaybill() {
        if (waybills == null) return false;
        return waybills.stream()
                .anyMatch(w -> w != null && w.getStatus() == Waybill.WaybillStatus.ACTIVE);
    }

    /**
     * Проверяет, есть ли у водителя активная смена (синоним для удобства)
     */
    public boolean hasActiveShift() {
        return hasActiveWaybill();
    }

    /**
     * Обновляет статус после медосмотра
     */
    public void updateMedicalStatus(boolean passed, LocalDateTime checkTime) {
        this.medicalStatus = passed ? MedicalStatus.PASSED : MedicalStatus.FAILED;
        this.lastMedicalCheck = checkTime;
    }

    /**
     * Проверяет, свободен ли водитель (не назначен на авто)
     */
    public boolean isAvailable() {
        return currentCar == null;
    }

    /**
     * Получить активный путевой лист
     */
    public Waybill getActiveWaybill() {
        if (waybills == null) return null;
        return waybills.stream()
                .filter(w -> w != null && w.getStatus() == Waybill.WaybillStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MedicalStatus getMedicalStatus() { return medicalStatus; }
    public void setMedicalStatus(MedicalStatus medicalStatus) { this.medicalStatus = medicalStatus; }

    public LocalDateTime getLastMedicalCheck() { return lastMedicalCheck; }
    public void setLastMedicalCheck(LocalDateTime lastMedicalCheck) { this.lastMedicalCheck = lastMedicalCheck; }

    public Car getCurrentCar() { return currentCar; }
    public void setCurrentCar(Car currentCar) { this.currentCar = currentCar; }

    public List<MedicalCheck> getMedicalChecks() { return medicalChecks; }
    public void setMedicalChecks(List<MedicalCheck> medicalChecks) { this.medicalChecks = medicalChecks; }

    public List<Waybill> getWaybills() { return waybills; }
    public void setWaybills(List<Waybill> waybills) { this.waybills = waybills; }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", phone='" + phone + '\'' +
                ", isActive=" + isActive +
                ", user=" + (user != null ? user.getId() : "null") +
                ", medicalStatus=" + medicalStatus +
                ", lastMedicalCheck=" + lastMedicalCheck +
                ", currentCar=" + (currentCar != null ? currentCar.getId() : "null") +
                '}';
    }
}