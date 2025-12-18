package com.taxi.service;

import com.taxi.entity.*;
import com.taxi.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class WaybillService {

    private WaybillRepository waybillRepository;
    private DriverRepository driverRepository;
    private CarRepository carRepository;
    private UserRepository userRepository;
    private MedicalCheckService medicalCheckService;
    private TechnicalInspectionService inspectionService;
    private OrderService orderService;

    public WaybillService() {
        this.waybillRepository = new WaybillRepository();
        this.driverRepository = new DriverRepository();
        this.carRepository = new CarRepository();
        this.userRepository = new UserRepository();
        this.medicalCheckService = new MedicalCheckService();
        this.inspectionService = new TechnicalInspectionService();
        this.orderService = new OrderService();
    }

    /**
     * Создать новый путевой лист
     */
    public Waybill createWaybill(Long driverId, Long technicianId,
                                 Integer initialMileage, String notes) {
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("Водитель с ID " + driverId + " не найден");
        }

        // ⭐ ПРОВЕРКА 1: Есть ли у водителя назначенный автомобиль?
        if (driver.getCurrentCar() == null) {
            throw new IllegalStateException("Водителю не назначен автомобиль. Назначьте автомобиль на странице водителей.");
        }

        Car car = driver.getCurrentCar(); // Берем назначенный автомобиль

        User technician = userRepository.findById(technicianId);
        if (technician == null) {
            throw new IllegalArgumentException("Техник с ID " + technicianId + " не найден");
        }

        // Проверяем, что пользователь - техник/механик
        if (!"MECHANIC".equals(technician.getUserType())) {
            throw new IllegalArgumentException("Пользователь с ID " + technicianId + " не является техником/механиком");
        }

        if (!medicalCheckService.isDriverAllowedToWork(driverId)) {
            throw new IllegalStateException("Водитель не допущен к работе. Требуется медосмотр.");
        }

        if (!inspectionService.isCarOperational(car.getId())) {
            throw new IllegalStateException("Автомобиль не исправен. Требуется техосмотр.");
        }

        Waybill activeDriverWaybill = waybillRepository.findActiveByDriverId(driverId);
        if (activeDriverWaybill != null) {
            throw new IllegalStateException("У водителя уже есть активная смена #" + activeDriverWaybill.getId());
        }

        Waybill activeCarWaybill = waybillRepository.findActiveByCarId(car.getId());
        if (activeCarWaybill != null) {
            throw new IllegalStateException("Автомобиль уже используется в смене #" + activeCarWaybill.getId());
        }

        // Создаем путевой лист с техником вместо врача
        Waybill waybill = new Waybill();
        waybill.setDriver(driver);
        waybill.setCar(car);
        waybill.setDoctor(technician); // Используем техника как "врача" (кто открыл смену)
        waybill.setInitialMileageKm(initialMileage);
        waybill.setNotes(notes);
        waybill.setStartTime(LocalDateTime.now());
        waybill.setStatus(Waybill.WaybillStatus.ACTIVE);

        //  ВАЖНО: СОХРАНИТЬ В БАЗУ ДАННЫХ!
        waybill = waybillRepository.save(waybill);

        System.out.println(" Путевой лист создан: #" + waybill.getId());
        System.out.println(" Водитель: " + driver.getFullName());
        System.out.println(" Автомобиль (назначенный): " + car.getLicensePlate() + " (" + car.getModel() + ")");
        System.out.println(" Техник: " + technician.getFullName());

        return waybill;
    }

    /**
     * Завершить путевой лист (базовая версия)
     */
    public Waybill completeWaybill(Long waybillId, Long mechanicId,
                                   Integer finalMileage, Double earnings, String notes) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            throw new IllegalArgumentException("Путевой лист с ID " + waybillId + " не найден");
        }

        if (!waybill.isActive()) {
            throw new IllegalStateException("Путевой лист уже завершен или отменен");
        }

        User mechanic = userRepository.findById(mechanicId);
        if (mechanic == null) {
            throw new IllegalArgumentException("Механик с ID " + mechanicId + " не найден");
        }

        if (finalMileage != null && waybill.getInitialMileageKm() != null) {
            if (finalMileage < waybill.getInitialMileageKm()) {
                throw new IllegalArgumentException("Конечный пробег не может быть меньше начального");
            }
        }

        // Получаем заказы из путевого листа для расчета статистики
        List<Order> waybillOrders = orderService.getOrdersByWaybillId(waybillId);

        // Автоматический расчет выручки и пробега из заказов
        Double calculatedRevenue = 0.0;
        Double calculatedDistance = 0.0;

        for (Order order : waybillOrders) {
            if (order.getPrice() != null) {
                calculatedRevenue += order.getPrice();
            }
            if (order.getDistanceKm() != null) {
                calculatedDistance += order.getDistanceKm();
            }
        }

        // Если не указан заработок, используем расчетный
        if (earnings == null || earnings == 0.0) {
            earnings = calculatedRevenue;
        }

        waybill.completeWaybill(mechanic, finalMileage, earnings, notes);

        // Обновляем статистику путевого листа
        if (calculatedRevenue > 0) {
            waybill.setTotalRevenue(calculatedRevenue);
        }
        if (calculatedDistance > 0) {
            waybill.setTotalDistance(calculatedDistance);
        }
        if (!waybillOrders.isEmpty()) {
            waybill.setOrdersCount(waybillOrders.size());
        }

        Car car = waybill.getCar();
        if (finalMileage != null) {
            car.setMileageKm(finalMileage);
            carRepository.update(car);
        }

        waybillRepository.update(waybill);

        System.out.println(" Путевой лист завершен: #" + waybill.getId());
        System.out.println(" Пробег за смену: " + waybill.getShiftMileage() + " км");
        System.out.println(" Заработок: " + earnings + " руб.");
        System.out.println(" Заказов в смене: " + waybillOrders.size());

        return waybill;
    }

    /**
     * Получить все путевые листы (ВКЛЮЧАЯ АКТИВНЫЕ)
     */
    public List<Waybill> getAllWaybills() {
        return waybillRepository.findAll();
    }

    /**
     * Получить активные путевые листы
     */
    public List<Waybill> getActiveWaybills() {
        return waybillRepository.findActive();
    }

    /**
     * Получить завершенные путевые листы
     */
    public List<Waybill> getCompletedWaybills() {
        List<Waybill> allWaybills = waybillRepository.findAll();
        return allWaybills.stream()
                .filter(Waybill::isCompleted)
                .sorted((w1, w2) -> {
                    if (w1.getEndTime() == null || w2.getEndTime() == null) return 0;
                    return w2.getEndTime().compareTo(w1.getEndTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить путевые листы водителя
     */
    public List<Waybill> getWaybillsByDriver(Long driverId) {
        return waybillRepository.findByDriverId(driverId);
    }

    /**
     * Получить путевой лист по ID
     */
    public Waybill getWaybillById(Long waybillId) {
        return waybillRepository.findById(waybillId);
    }

    /**
     * Получить активный путевой лист водителя
     */
    public Waybill getActiveWaybillByDriver(Long driverId) {
        return waybillRepository.findActiveByDriverId(driverId);
    }

    /**
     * Получить активный путевой лист автомобиля
     */
    public Waybill getActiveWaybillByCar(Long carId) {
        return waybillRepository.findActiveByCarId(carId);
    }

    /**
     * Отменить путевой лист
     */
    public void cancelWaybill(Long waybillId) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            throw new IllegalArgumentException("Путевой лист с ID " + waybillId + " не найден");
        }

        if (!waybill.isActive()) {
            throw new IllegalStateException("Путевой лист уже завершен или отменен");
        }

        waybill.cancelWaybill("Отменено администратором");
        waybillRepository.update(waybill);

        System.out.println(" Путевой лист отменен: #" + waybillId);
    }

    /**
     * Удалить путевой лист
     */
    public void deleteWaybill(Long waybillId) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill != null) {
            if (waybill.getStatus() == Waybill.WaybillStatus.ACTIVE) {
                throw new IllegalStateException("Нельзя удалить активный путевой лист");
            }

            waybillRepository.delete(waybillId);
            System.out.println("🗑 Удален путевой лист: #" + waybillId);
        }
    }

    /**
     * Обновить примечания к путевому листу
     */
    public void updateWaybillNotes(Long waybillId, String notes) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            throw new IllegalArgumentException("Путевой лист с ID " + waybillId + " не найден");
        }

        waybill.setNotes(notes);
        waybillRepository.update(waybill);

        System.out.println(" Обновлены примечания к путевому листу: #" + waybillId);
    }

    /**
     * Проверить, можно ли водителю начать смену
     */
    public boolean canDriverStartShift(Long driverId) {
        if (!medicalCheckService.isDriverAllowedToWork(driverId)) {
            return false;
        }

        Waybill activeWaybill = waybillRepository.findActiveByDriverId(driverId);
        return activeWaybill == null;
    }

    /**
     * Проверить, можно ли использовать автомобиль
     */
    public boolean canCarBeUsed(Long carId) {
        if (!inspectionService.isCarOperational(carId)) {
            return false;
        }

        Waybill activeWaybill = waybillRepository.findActiveByCarId(carId);
        return activeWaybill == null;
    }

    /**
     * Завершить рабочую смену (полная версия для Сценария 3)
     */
    public Waybill completeShift(Long waybillId, Long mechanicId, Integer finalMileage,
                                 String inspectionNotes, Double additionalEarnings) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" ЗАВЕРШЕНИЕ РАБОЧЕЙ СМЕНЫ (СЦЕНАРИЙ 3)");
        System.out.println("=".repeat(60));

        // 1. Получаем путевой лист
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            throw new RuntimeException("Путевой лист #" + waybillId + " не найден");
        }

        if (!waybill.isActive()) {
            throw new RuntimeException("Путевой лист #" + waybillId + " уже завершен или отменен");
        }

        // 2. Проверяем механика
        User mechanic = userRepository.findById(mechanicId);
        if (mechanic == null || !"MECHANIC".equals(mechanic.getUserType())) {
            throw new RuntimeException("Механик не найден или не имеет нужной роли");
        }

        // 3. Проверяем автомобиль
        Car car = waybill.getCar();
        if (car == null) {
            throw new RuntimeException("Автомобиль не найден");
        }

        // 4. Проверяем активные заказы водителя
        System.out.println("\n ПРОВЕРКА АКТИВНЫХ ЗАКАЗОВ:");
        List<Order> activeOrders = orderService.getActiveOrders();
        if (!activeOrders.isEmpty()) {
            System.out.println("⚠  Обнаружены незавершенные заказы:");
            for (Order order : activeOrders) {
                System.out.println("   • Заказ #" + order.getId() + " - " + order.getStatusDisplay());
            }
            throw new RuntimeException("Невозможно завершить смену: есть незавершенные заказы");
        } else {
            System.out.println(" Все заказы завершены");
        }

        // 5. ФИНАЛЬНЫЙ ТЕХОСМОТР
        System.out.println("\n🔧 ШАГ 1: ФИНАЛЬНАЯ ПРОВЕРКА АВТОМОБИЛЯ");
        System.out.println("-".repeat(40));

        TechnicalInspection finalInspection = inspectionService.createInspection(
                car.getId(),
                mechanicId,
                true,
                finalMileage,
                "Финальная проверка после смены. " +
                        (inspectionNotes != null ? inspectionNotes : "Автомобиль в исправном состоянии.")
        );

        // Обновляем пробег автомобиля
        car.setMileageKm(finalMileage);
        carRepository.update(car);

        System.out.println(" Автомобиль проверен:");
        System.out.println("    " + car.getDisplayName());
        System.out.println("    Финальный пробег: " + finalMileage + " км");
        System.out.println("    Проверил: " + mechanic.getFullName());

        // 6. РАСЧЕТ ЗАРАБОТКА
        System.out.println("\n ШАГ 2: РАСЧЕТ ЗАРАБОТКА");
        System.out.println("-".repeat(40));

        // Получаем заказы из путевого листа
        List<Order> waybillOrders = orderService.getOrdersByWaybillId(waybillId);

        // Общий заработок = выручка от заказов + дополнительные доходы
        Double totalRevenue = 0.0;
        for (Order order : waybillOrders) {
            if (order.getPrice() != null) {
                totalRevenue += order.getPrice();
            }
        }

        if (additionalEarnings != null && additionalEarnings > 0) {
            totalRevenue += additionalEarnings;
            System.out.println("    Дополнительный доход: +" + additionalEarnings + " руб");
        }

        // Вычитаем комиссию таксопарка (20%)
        Double commission = totalRevenue * 0.20;
        Double driverEarnings = totalRevenue - commission;

        System.out.println("    Общая выручка: " + totalRevenue + " руб");
        System.out.println("    Комиссия таксопарка (20%): " + commission + " руб");
        System.out.println("    Заработок водителя: " + driverEarnings + " руб");

        // 7. ЗАКРЫТИЕ ПУТЕВОГО ЛИСТА
        System.out.println("\n ШАГ 3: ЗАКРЫТИЕ ПУТЕВОГО ЛИСТА");
        System.out.println("-".repeat(40));

        String notes = "Смена завершена.\n" +
                "Механик: " + mechanic.getFullName() + "\n" +
                "Пробег за смену: " + (finalMileage - waybill.getInitialMileageKm()) + " км\n" +
                "Количество заказов: " + waybillOrders.size() + "\n" +
                (inspectionNotes != null ? "Примечания по техосмотру: " + inspectionNotes : "");

        waybill.completeWaybill(mechanic, finalMileage, driverEarnings, notes);
        waybill.setTotalRevenue(totalRevenue);
        waybill.setOrdersCount(waybillOrders.size());

        waybillRepository.update(waybill);

        // 8. ОБНОВЛЕНИЕ СТАТУСОВ
        System.out.println("\n ШАГ 4: ОБНОВЛЕНИЕ СТАТУСОВ");
        System.out.println("-".repeat(40));

        // Обновляем статус автомобиля
        car.setInRepair(false);
        // Временное решение для технического статуса
        car.setTechnicalStatus(TechnicalStatus.OK);
        carRepository.update(car);

        // Обновляем статус водителя (требуется медосмотр для следующей смены)
        Driver driver = waybill.getDriver();
        driver.setMedicalStatus(MedicalStatus.PENDING);
        driverRepository.update(driver);

        System.out.println("Статусы обновлены:");
        System.out.println("   Автомобиль " + car.getLicensePlate() + " свободен");
        System.out.println("    Технический статус: " + TechnicalStatus.OK.getDescription());
        System.out.println("    Водитель " + driver.getFullName() + " завершил смену");
        System.out.println("    Требуется медосмотр для следующей смены");

        System.out.println("\n" + "".repeat(20));
        System.out.println(" СМЕНА УСПЕШНО ЗАВЕРШЕНА!");
        System.out.println("".repeat(20));

        return waybill;
    }

    /**
     * Получить сводку по завершенной смене
     */
    public String getShiftSummary(Long waybillId) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            throw new RuntimeException("Путевой лист не найден");
        }

        if (!waybill.isCompleted()) {
            throw new RuntimeException("Путевой лист еще не завершен");
        }

        // Получаем заказы для отображения деталей
        List<Order> waybillOrders = orderService.getOrdersByWaybillId(waybillId);

        // Безопасное получение значений
        Double totalRevenue = waybill.getTotalRevenue() != null ? waybill.getTotalRevenue() : 0.0;
        Double commission = totalRevenue * 0.20;
        Double driverEarnings = waybill.getTotalEarnings() != null ? waybill.getTotalEarnings() : 0.0;
        Integer ordersCount = waybill.getOrdersCount() != null ? waybill.getOrdersCount() : 0;

        StringBuilder summary = new StringBuilder();
        summary.append(String.format(
                " СВОДКА ПО СМЕНЕ #%d\n" +
                        "═══════════════════════════════\n" +
                        " Водитель: %s\n" +
                        " Автомобиль: %s (%s)\n" +
                        "  Продолжительность: %s\n" +
                        " Пробег за смену: %d км\n" +
                        " Выполнено заказов: %d\n" +
                        " Общая выручка: %.2f руб\n" +
                        " Комиссия таксопарка: %.2f руб\n" +
                        " Заработок водителя: %.2f руб\n" +
                        " Начало: %s\n" +
                        " Окончание: %s\n" +
                        " Закрыл смену: %s\n",
                waybill.getId(),
                waybill.getDriver() != null ? waybill.getDriver().getFullName() : "Неизвестно",
                waybill.getCar() != null ? waybill.getCar().getLicensePlate() : "Неизвестно",
                waybill.getCar() != null ? waybill.getCar().getModel() : "Неизвестно",
                waybill.getShiftDuration(),
                waybill.getShiftMileage(),
                ordersCount,
                totalRevenue,
                commission,
                driverEarnings,
                waybill.getStartTime() != null ?
                        waybill.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "Н/Д",
                waybill.getEndTime() != null ?
                        waybill.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "Н/Д",
                waybill.getMechanic() != null ? waybill.getMechanic().getFullName() : "Н/Д"
        ));

        // Добавляем список заказов
        if (!waybillOrders.isEmpty()) {
            summary.append("\n📋 СПИСОК ЗАКАЗОВ:\n");
            summary.append("═══════════════════════════════\n");
            for (Order order : waybillOrders) {
                summary.append(String.format("   #%d: %s → %s | %.2f руб | %.1f км\n",
                        order.getId(),
                        order.getPickupAddress(),
                        order.getDestinationAddress() != null ? order.getDestinationAddress() : "?",
                        order.getPrice() != null ? order.getPrice() : 0.0,
                        order.getDistanceKm() != null ? order.getDistanceKm() : 0.0
                ));
            }
        }

        return summary.toString();
    }

    /**
     * Получить финальную статистику путевого листа
     */
    public String getFinalWaybillStats(Long waybillId) {
        Waybill waybill = waybillRepository.findById(waybillId);
        if (waybill == null) {
            return "Путевой лист не найден";
        }

        return waybill.getDetailedStats();
    }

    // доп классы для статистики

    public static class WaybillStats {
        public final long totalWaybills;
        public final long activeWaybills;
        public final long completedWaybills;
        public final double totalEarnings;
        public final int totalMileage;
        public final double avgEarningsPerShift;
        public final double avgMileagePerShift;

        public WaybillStats(long totalWaybills, long activeWaybills, long completedWaybills,
                            double totalEarnings, int totalMileage,
                            double avgEarningsPerShift, double avgMileagePerShift) {
            this.totalWaybills = totalWaybills;
            this.activeWaybills = activeWaybills;
            this.completedWaybills = completedWaybills;
            this.totalEarnings = totalEarnings;
            this.totalMileage = totalMileage;
            this.avgEarningsPerShift = avgEarningsPerShift;
            this.avgMileagePerShift = avgMileagePerShift;
        }
    }

    public static class DriverReport {
        public final Driver driver;
        public final List<Waybill> waybills;
        public final int totalShifts;
        public final double totalEarnings;
        public final int totalMileage;
        public final double avgEarningsPerShift;

        public DriverReport(Driver driver, List<Waybill> waybills, int totalShifts,
                            double totalEarnings, int totalMileage, double avgEarningsPerShift) {
            this.driver = driver;
            this.waybills = waybills;
            this.totalShifts = totalShifts;
            this.totalEarnings = totalEarnings;
            this.totalMileage = totalMileage;
            this.avgEarningsPerShift = avgEarningsPerShift;
        }
    }
}