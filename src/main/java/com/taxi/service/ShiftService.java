package com.taxi.service;

import com.taxi.entity.Car;
import com.taxi.entity.Driver;
import com.taxi.entity.User;
import com.taxi.entity.Waybill;
import com.taxi.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ShiftService {

    private WaybillService waybillService;
    private MedicalCheckService medicalCheckService;
    private TechnicalInspectionService inspectionService;
    private DriverRepository driverRepository;
    private CarRepository carRepository;
    private UserRepository userRepository;

    public ShiftService() {
        this.waybillService = new WaybillService();
        this.medicalCheckService = new MedicalCheckService();
        this.inspectionService = new TechnicalInspectionService();
        this.driverRepository = new DriverRepository();
        this.carRepository = new CarRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Открыть смену (полная проверка)
     */
    public Waybill openShift(Long driverId, Long carId, Long doctorId,
                             Integer initialMileage, String notes) {

        // Проверяем существование сущностей
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("Водитель не найден");
        }

        Car car = carRepository.findById(carId);
        if (car == null) {
            throw new IllegalArgumentException("Автомобиль не найден");
        }

        User doctor = userRepository.findById(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Врач не найден");
        }

        // Проверяем бизнес-правила
        validateShiftOpening(driverId, carId);

        // Создаем путевой лист
        Waybill waybill = waybillService.createWaybill(driverId, doctorId, initialMileage, notes);
        // Логирование
        logShiftOpened(waybill);

        return waybill;
    }

    /**
     * Проверить возможность открытия смены
     */
    private void validateShiftOpening(Long driverId, Long carId) {
        // Проверяем водителя
        if (!waybillService.canDriverStartShift(driverId)) {
            throw new IllegalStateException("Водитель не может начать смену");
        }

        // Проверяем автомобиль
        if (!waybillService.canCarBeUsed(carId)) {
            throw new IllegalStateException("Автомобиль не может быть использован");
        }

        // Дополнительные проверки
        if (!medicalCheckService.isDriverAllowedToWork(driverId)) {
            throw new IllegalStateException("Водитель не допущен к работе по медосмотру");
        }

        if (!inspectionService.isCarOperational(carId)) {
            throw new IllegalStateException("Автомобиль не исправен");
        }
    }

    /**
     * Закрыть смену
     */
    public Waybill closeShift(Long waybillId, Long mechanicId,
                              Integer finalMileage, Double earnings, String notes) {

        // Проверяем механика
        User mechanic = userRepository.findById(mechanicId);
        if (mechanic == null) {
            throw new IllegalArgumentException("Механик не найден");
        }

        // Завершаем путевой лист
        Waybill waybill = waybillService.completeWaybill(waybillId, mechanicId, finalMileage, earnings, notes);

        // Логирование
        logShiftClosed(waybill);

        return waybill;
    }

    /**
     * Получить доступных для работы водителей
     */
    public List<Driver> getAvailableDrivers() {
        List<Driver> allowedDrivers = medicalCheckService.getAllowedDrivers();

        // Фильтруем водителей без активных смен
        return allowedDrivers.stream()
                .filter(driver -> waybillService.getActiveWaybillByDriver(driver.getId()) == null)
                .toList();
    }

    /**
     * Получить доступные автомобили
     */
    public List<Car> getAvailableCars() {
        List<Car> operationalCars = inspectionService.getOperationalCars();

        // Фильтруем автомобили без активных смен
        return operationalCars.stream()
                .filter(car -> waybillService.getActiveWaybillByCar(car.getId()) == null)
                .toList();
    }

    /**
     * Получить активные смены
     */
    public List<Waybill> getActiveShifts() {
        return waybillService.getActiveWaybills();
    }

    /**
     * Получить отчет по сменам за день
     */
    public ShiftReport getDailyReport(LocalDate date) {
        List<Waybill> allWaybills = waybillService.getAllWaybills();

        // Фильтруем смены за указанный день
        List<Waybill> dailyWaybills = allWaybills.stream()
                .filter(w -> w.getStartTime().toLocalDate().equals(date))
                .toList();

        long activeShifts = dailyWaybills.stream().filter(Waybill::isActive).count();
        long completedShifts = dailyWaybills.size() - activeShifts;

        double totalEarnings = dailyWaybills.stream()
                .filter(w -> w.getTotalEarnings() != null)
                .mapToDouble(Waybill::getTotalEarnings)
                .sum();

        int totalMileage = dailyWaybills.stream()
                .mapToInt(Waybill::getShiftMileage)
                .sum();

        return new ShiftReport(date, dailyWaybills, activeShifts,
                completedShifts, totalEarnings, totalMileage);
    }

    /**
     * Получить отчет по сменам за период (от и до)
     */
    public ShiftReport getPeriodReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Waybill> allWaybills = waybillService.getAllWaybills();

        // Фильтруем смены за указанный период
        List<Waybill> periodWaybills = allWaybills.stream()
                .filter(w -> !w.getStartTime().isBefore(startDate) && !w.getStartTime().isAfter(endDate))
                .toList();

        long activeShifts = periodWaybills.stream().filter(Waybill::isActive).count();
        long completedShifts = periodWaybills.size() - activeShifts;

        double totalEarnings = periodWaybills.stream()
                .filter(w -> w.getTotalEarnings() != null)
                .mapToDouble(Waybill::getTotalEarnings)
                .sum();

        int totalMileage = periodWaybills.stream()
                .mapToInt(Waybill::getShiftMileage)
                .sum();

        return new ShiftReport(startDate, periodWaybills, activeShifts,
                completedShifts, totalEarnings, totalMileage);
    }

    /**
     * Логирование открытия смены
     */
    private void logShiftOpened(Waybill waybill) {
        System.out.println("=".repeat(50));
        System.out.println("🚀 СМЕНА ОТКРЫТА");
        System.out.println("=".repeat(50));
        System.out.println("📋 Путевой лист: #" + waybill.getId());
        System.out.println("👤 Водитель: " + waybill.getDriver().getFullName());
        System.out.println("🚗 Автомобиль: " + waybill.getCar().getDisplayName());
        System.out.println("🏥 Врач: " + waybill.getDoctor().getFullName());
        System.out.println("⏰ Время начала: " + waybill.getStartTime());
        System.out.println("📏 Начальный пробег: " + waybill.getInitialMileageKm() + " км");
        System.out.println("=".repeat(50));
    }

    /**
     * Логирование закрытия смены
     */
    private void logShiftClosed(Waybill waybill) {
        System.out.println("=".repeat(50));
        System.out.println(" СМЕНА ЗАКРЫТА");
        System.out.println("=".repeat(50));
        System.out.println(" Путевой лист: #" + waybill.getId());
        System.out.println(" Водитель: " + waybill.getDriver().getFullName());
        System.out.println(" Автомобиль: " + waybill.getCar().getDisplayName());
        System.out.println(" Механик: " + waybill.getMechanic().getFullName());
        System.out.println(" Продолжительность: " + waybill.getShiftDuration());
        System.out.println(" Пробег за смену: " + waybill.getShiftMileage() + " км");
        System.out.println(" Заработок: " + waybill.getTotalEarnings() + " руб.");
        System.out.println("=".repeat(50));
    }

    /**
     * Класс для отчета по сменам
     */
    public static class ShiftReport {
        public final LocalDateTime reportDate;
        public final List<Waybill> waybills;
        public final long activeShifts;
        public final long completedShifts;
        public final double totalEarnings;
        public final int totalMileage;

        // Конструктор для LocalDate
        public ShiftReport(LocalDate date, List<Waybill> waybills,
                           long activeShifts, long completedShifts,
                           double totalEarnings, int totalMileage) {
            this.reportDate = date.atStartOfDay();
            this.waybills = waybills;
            this.activeShifts = activeShifts;
            this.completedShifts = completedShifts;
            this.totalEarnings = totalEarnings;
            this.totalMileage = totalMileage;
        }

        // Конструктор для LocalDateTime
        public ShiftReport(LocalDateTime date, List<Waybill> waybills,
                           long activeShifts, long completedShifts,
                           double totalEarnings, int totalMileage) {
            this.reportDate = date;
            this.waybills = waybills;
            this.activeShifts = activeShifts;
            this.completedShifts = completedShifts;
            this.totalEarnings = totalEarnings;
            this.totalMileage = totalMileage;
        }

        public double getAverageEarningsPerShift() {
            return completedShifts > 0 ? totalEarnings / completedShifts : 0;
        }

        public double getAverageMileagePerShift() {
            return completedShifts > 0 ? (double) totalMileage / completedShifts : 0;
        }

        // Метод для получения даты без времени
        public LocalDate getDateOnly() {
            return reportDate.toLocalDate();
        }
    }
}