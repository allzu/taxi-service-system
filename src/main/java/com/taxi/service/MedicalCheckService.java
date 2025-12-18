package com.taxi.service;

import com.taxi.entity.Driver;
import com.taxi.entity.MedicalCheck;
import com.taxi.entity.User;
import com.taxi.repository.DriverRepository;
import com.taxi.repository.MedicalCheckRepository;
import com.taxi.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MedicalCheckService {

    private MedicalCheckRepository medicalCheckRepository;
    private DriverRepository driverRepository;
    private UserRepository userRepository;

    public MedicalCheckService() {
        this.medicalCheckRepository = new MedicalCheckRepository();
        this.driverRepository = new DriverRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Создать новый медосмотр
     */
    public MedicalCheck createMedicalCheck(Long driverId, Long doctorId,
                                           Boolean isPassed, String notes) {

        // Проверяем водителя
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("Водитель с ID " + driverId + " не найден");
        }

        // Проверяем врача
        User doctor = userRepository.findById(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Врач с ID " + doctorId + " не найден");
        }

        // Создаем медосмотр
        MedicalCheck medicalCheck = new MedicalCheck(driver, doctor, isPassed);
        medicalCheck.setNotes(notes);
        medicalCheck.setCheckDate(LocalDateTime.now());

        //  Обновляем водителя
        driver.updateMedicalStatus(isPassed, LocalDateTime.now());
        driverRepository.update(driver);
        // Если медосмотр пройден, можно открыть смену
        if (Boolean.TRUE.equals(isPassed)) {
            medicalCheck.setOpensShift(true);
        }

        // Сохраняем
        medicalCheckRepository.save(medicalCheck);

        System.out.println("🏥 Медосмотр создан: " +
                driver.getFullName() + " - " +
                (isPassed ? " Допущен" : " Не допущен"));

        return medicalCheck;
    }

    /**
     * Получить медосмотр по ID
     */
    public MedicalCheck getMedicalCheckById(Long id) {
        return medicalCheckRepository.findById(id);
    }

    /**
     * Получить все медосмотры
     */
    public List<MedicalCheck> getAllMedicalChecks() {
        return medicalCheckRepository.findAll();
    }

    /**
     * Получить медосмотры водителя
     */
    public List<MedicalCheck> getMedicalChecksByDriver(Long driverId) {
        return medicalCheckRepository.findByDriverId(driverId);
    }

    /**
     * Получить последний медосмотр водителя
     */
    public MedicalCheck getLastMedicalCheckByDriver(Long driverId) {
        return medicalCheckRepository.findLatestByDriverId(driverId);
    }

    /**
     * Проверить, допущен ли водитель к работе
     */
    public boolean isDriverAllowedToWork(Long driverId) {
        MedicalCheck lastCheck = medicalCheckRepository.findLatestPassedByDriverId(driverId);
        return lastCheck != null && Boolean.TRUE.equals(lastCheck.getIsPassed());
    }

    /**
     * Получить водителей, допущенных к работе
     */
    public List<Driver> getAllowedDrivers() {
        List<MedicalCheck> allChecks = medicalCheckRepository.findAll();

        // Фильтруем только последние пройденные медосмотры для каждого водителя
        return allChecks.stream()
                .filter(check -> Boolean.TRUE.equals(check.getIsPassed()))
                .collect(Collectors.toMap(
                        check -> check.getDriver().getId(),
                        check -> check,
                        (check1, check2) ->
                                check1.getCheckDate().isAfter(check2.getCheckDate()) ? check1 : check2
                ))
                .values().stream()
                .map(MedicalCheck::getDriver)
                .collect(Collectors.toList());
    }

    /**
     * Получить водителей, не допущенных к работе
     */
    public List<Driver> getNotAllowedDrivers() {
        List<Driver> allDrivers = driverRepository.findAll();
        List<Driver> allowedDrivers = getAllowedDrivers();

        // Создаем множество ID допущенных водителей для быстрого поиска
        Set<Long> allowedDriverIds = allowedDrivers.stream()
                .map(Driver::getId)
                .collect(Collectors.toSet());

        // Возвращаем водителей, которых нет в списке допущенных
        return allDrivers.stream()
                .filter(driver -> !allowedDriverIds.contains(driver.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Обновить медосмотр
     */
    public void updateMedicalCheck(Long checkId, Boolean isPassed, String notes, Boolean opensShift) {
        MedicalCheck medicalCheck = medicalCheckRepository.findById(checkId);
        if (medicalCheck == null) {
            throw new IllegalArgumentException("Медосмотр с ID " + checkId + " не найден");
        }

        medicalCheck.setIsPassed(isPassed);
        medicalCheck.setNotes(notes);
        medicalCheck.setOpensShift(opensShift);

        medicalCheckRepository.update(medicalCheck);

        System.out.println("✏ Медосмотр обновлен: " +
                medicalCheck.getDriver().getFullName() + " - " +
                (isPassed ? " Допущен" : " Не допущен"));
    }

    /**
     * Удалить медосмотр
     */
    public void deleteMedicalCheck(Long checkId) {
        MedicalCheck medicalCheck = medicalCheckRepository.findById(checkId);
        if (medicalCheck != null) {
            System.out.println("🗑️ Удален медосмотр: " +
                    medicalCheck.getDriver().getFullName());
            medicalCheckRepository.delete(checkId);
        }
    }

    /**
     * Открыть смену для водителя (на основе последнего медосмотра)
     */
    public boolean openShiftForDriver(Long driverId) {
        MedicalCheck lastCheck = medicalCheckRepository.findLatestPassedByDriverId(driverId);

        if (lastCheck == null) {
            System.out.println(" Нельзя открыть смену: водитель не прошел медосмотр");
            return false;
        }

        if (!Boolean.TRUE.equals(lastCheck.getIsPassed())) {
            System.out.println(" Нельзя открыть смену: водитель не допущен к работе");
            return false;
        }

        lastCheck.setOpensShift(true);
        medicalCheckRepository.update(lastCheck);

        System.out.println(" Смена открыта для водителя: " +
                lastCheck.getDriver().getFullName());

        return true;
    }

    /**
     * Получить статистику по медосмотрам
     */
    public MedicalCheckStats getStatistics() {
        List<MedicalCheck> allChecks = medicalCheckRepository.findAll();

        long total = allChecks.size();
        long passed = allChecks.stream()
                .filter(check -> Boolean.TRUE.equals(check.getIsPassed()))
                .count();
        long failed = total - passed;
        long opensShift = allChecks.stream()
                .filter(check -> Boolean.TRUE.equals(check.getOpensShift()))
                .count();

        return new MedicalCheckStats(total, passed, failed, opensShift);
    }

    public long getFailedMedicalChecks() {
        List<MedicalCheck> checks = getAllMedicalChecks();
        return checks.stream()
                .filter(check -> !check.getIsPassed())
                .count();
    }

    public long getPassedMedicalChecks() {
        List<MedicalCheck> checks = getAllMedicalChecks();
        return checks.stream()
                .filter(MedicalCheck::getIsPassed)
                .count();
    }

    public long getTotalMedicalChecks() {
        List<MedicalCheck> checks = getAllMedicalChecks();
        return checks.size();
    }

    public List<MedicalCheck> getRecentMedicalChecks(int limit) {
        List<MedicalCheck> checks = getAllMedicalChecks();
        // Сортируем по дате (последние сначала) и ограничиваем
        return checks.stream()
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Вспомогательный класс для статистики
     */
    public static class MedicalCheckStats {
        public final long total;
        public final long passed;
        public final long failed;
        public final long opensShift;

        public MedicalCheckStats(long total, long passed, long failed, long opensShift) {
            this.total = total;
            this.passed = passed;
            this.failed = failed;
            this.opensShift = opensShift;
        }

        public double getPassRate() {
            return total > 0 ? (passed * 100.0 / total) : 0;
        }
    }

    /**
     * Получить список водителей для формы (для сервлета)
     */
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    /**
     * Получить список врачей для формы (для сервлета)
     */
    public List<User> getAllDoctors() {
        return userRepository.findByRole("DOCTOR");
    }

    /**
     * Получить медосмотры по фильтрам
     */
    public List<MedicalCheck> getFilteredMedicalChecks(Long driverId, LocalDate date,
                                                       Boolean status, Boolean opensShift) {
        List<MedicalCheck> allChecks = getAllMedicalChecks();

        return allChecks.stream()
                .filter(check -> {
                    if (driverId != null && !check.getDriver().getId().equals(driverId)) {
                        return false;
                    }
                    if (date != null && !check.getCheckDate().toLocalDate().equals(date)) {
                        return false;
                    }
                    if (status != null && !check.getIsPassed().equals(status)) {
                        return false;
                    }
                    if (opensShift != null && !opensShift.equals(check.getOpensShift())) {
                        return false;
                    }
                    return true;
                })
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate()))
                .collect(Collectors.toList());
    }

    /**
     * Поиск медосмотров по тексту
     */
    public List<MedicalCheck> searchMedicalChecks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllMedicalChecks();
        }

        String searchTerm = query.toLowerCase().trim();
        List<MedicalCheck> allChecks = getAllMedicalChecks();

        return allChecks.stream()
                .filter(check -> {
                    return check.getDriver().getFullName().toLowerCase().contains(searchTerm) ||
                            check.getDoctor().getFullName().toLowerCase().contains(searchTerm) ||
                            (check.getNotes() != null && check.getNotes().toLowerCase().contains(searchTerm)) ||
                            check.getDriver().getLicenseNumber().toLowerCase().contains(searchTerm);
                })
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate()))
                .collect(Collectors.toList());
    }

    /**
     * Получить медосмотры за сегодня
     */
    public List<MedicalCheck> getTodayMedicalChecks() {
        LocalDate today = LocalDate.now();
        return getAllMedicalChecks().stream()
                .filter(check -> check.getCheckDate().toLocalDate().equals(today))
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate()))
                .collect(Collectors.toList());
    }

    /**
     * Получить просроченные медосмотры (старше 1 года)
     */
    public List<MedicalCheck> getExpiredMedicalChecks() {
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return getAllMedicalChecks().stream()
                .filter(check -> check.getCheckDate().toLocalDate().isBefore(oneYearAgo))
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate()))
                .collect(Collectors.toList());
    }

    /**
     * Массовое удаление просроченных медосмотров
     */
    public int deleteExpiredMedicalChecks() {
        List<MedicalCheck> expiredChecks = getExpiredMedicalChecks();
        int deletedCount = 0;

        for (MedicalCheck check : expiredChecks) {
            medicalCheckRepository.delete(check.getId());
            deletedCount++;
            System.out.println("🗑️ Удален просроченный медосмотр: " +
                    check.getDriver().getFullName() + " (" +
                    check.getCheckDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ")");
        }

        return deletedCount;
    }

    /**
     * Получить статистику по периодам
     */
    public PeriodStats getPeriodStats() {
        List<MedicalCheck> allChecks = getAllMedicalChecks();
        LocalDate today = LocalDate.now();

        long todayCount = allChecks.stream()
                .filter(check -> check.getCheckDate().toLocalDate().equals(today))
                .count();

        long thisWeekCount = allChecks.stream()
                .filter(check -> check.getCheckDate().toLocalDate().isAfter(today.minusDays(7)))
                .count();

        long thisMonthCount = allChecks.stream()
                .filter(check -> check.getCheckDate().toLocalDate().isAfter(today.minusDays(30)))
                .count();

        return new PeriodStats(todayCount, thisWeekCount, thisMonthCount);
    }

    /**
     * Статистика по периодам
     */
    public static class PeriodStats {
        public final long today;
        public final long thisWeek;
        public final long thisMonth;

        public PeriodStats(long today, long thisWeek, long thisMonth) {
            this.today = today;
            this.thisWeek = thisWeek;
            this.thisMonth = thisMonth;
        }
    }
}