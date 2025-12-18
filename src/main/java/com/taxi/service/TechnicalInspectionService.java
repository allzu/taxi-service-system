package com.taxi.service;

import com.taxi.entity.TechnicalInspection;
import com.taxi.entity.Car;
import com.taxi.entity.TechnicalStatus;
import com.taxi.entity.User;
import com.taxi.repository.TechnicalInspectionRepository;
import com.taxi.repository.CarRepository;
import com.taxi.repository.UserRepository;
import com.taxi.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TechnicalInspectionService {
    private TechnicalInspectionRepository inspectionRepository = new TechnicalInspectionRepository();
    private CarRepository carRepository = new CarRepository();
    private UserRepository userRepository = new UserRepository();

    /**
     * Получить все техосмотры с загруженными автомобилями и механиками
     * Решает проблему "could not initialize proxy - no Session"
     */
    public List<TechnicalInspection> findAllWithCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Используем JOIN FETCH для загрузки связанных сущностей
            Query<TechnicalInspection> query = session.createQuery(
                    "SELECT DISTINCT ti FROM TechnicalInspection ti " +
                            "LEFT JOIN FETCH ti.car " +
                            "LEFT JOIN FETCH ti.mechanic " +
                            "ORDER BY ti.inspectionDate DESC",
                    TechnicalInspection.class
            );
            return query.list();
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке техосмотров: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Получить все технические осмотры
     * Теперь использует правильную загрузку
     */
    public List<TechnicalInspection> findAll() {
        return findAllWithCars();
    }

    /**
     * Получить техосмотр по ID с загруженными связями
     */
    public TechnicalInspection findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<TechnicalInspection> query = session.createQuery(
                    "SELECT ti FROM TechnicalInspection ti " +
                            "LEFT JOIN FETCH ti.car " +
                            "LEFT JOIN FETCH ti.mechanic " +
                            "WHERE ti.id = :id",
                    TechnicalInspection.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске техосмотра: " + e.getMessage());
            return null;
        }
    }

    public void save(TechnicalInspection inspection) {
        inspectionRepository.save(inspection);
    }

    public void update(TechnicalInspection inspection) {
        inspectionRepository.update(inspection);
    }

    public void delete(Long id) {
        inspectionRepository.delete(id);
    }

    /**
     * Получить все технические осмотры
     */
    public List<TechnicalInspection> getAllTechnicalInspections() {
        return findAllWithCars();
    }

    /**
     * Получить последние техосмотры
     */
    public List<TechnicalInspection> getRecentInspections(int limit) {
        List<TechnicalInspection> allInspections = findAllWithCars();
        return allInspections.stream()
                .sorted((i1, i2) -> i2.getInspectionDate().compareTo(i1.getInspectionDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получить техосмотры по автомобилю
     */
    public List<TechnicalInspection> getInspectionsByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<TechnicalInspection> query = session.createQuery(
                    "SELECT ti FROM TechnicalInspection ti " +
                            "LEFT JOIN FETCH ti.car " +
                            "LEFT JOIN FETCH ti.mechanic " +
                            "WHERE ti.car.id = :carId " +
                            "ORDER BY ti.inspectionDate DESC",
                    TechnicalInspection.class
            );
            query.setParameter("carId", carId);
            return query.list();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске техосмотров по автомобилю: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Получить количество техосмотров
     */
    public long getTotalInspections() {
        return findAllWithCars().size();
    }

    /**
     * Получить количество пройденных техосмотров
     */
    public long getPassedInspectionsCount() {
        return findAllWithCars().stream()
                .filter(TechnicalInspection::getIsPassed)
                .count();
    }

    /**
     * Получить количество не пройденных техосмотров
     */
    public long getFailedInspectionsCount() {
        return findAllWithCars().stream()
                .filter(inspection -> !inspection.getIsPassed())
                .count();
    }

    /**
     * Создать технический осмотр
     */
    public TechnicalInspection createInspection(Long carId, Long mechanicId, boolean isPassed,
                                                Integer mileage, String notes) {
        // Находим автомобиль
        Car car = carRepository.findById(carId);
        if (car == null) {
            throw new IllegalArgumentException("Автомобиль с ID " + carId + " не найден");
        }

        // Находим механика
        User mechanic = userRepository.findById(mechanicId);
        if (mechanic == null) {
            throw new IllegalArgumentException("Пользователь с ID " + mechanicId + " не найден");
        }

        // Проверяем, что пользователь - механик
        if (!"MECHANIC".equals(mechanic.getUserType())) {
            throw new IllegalArgumentException("Пользователь с ID " + mechanicId + " не является механиком");
        }

        TechnicalInspection inspection = new TechnicalInspection();

        // Устанавливаем связи с объектами
        inspection.setCar(car);
        inspection.setMechanic(mechanic);
        inspection.setIsPassed(isPassed);
        inspection.setNotes(notes);
        inspection.setInspectionDate(LocalDateTime.now());

        // Если нужен пробег
        if (mileage != null) {
            inspection.setMileageKm(mileage);
        }

        //  ВАЖНО: Обновляем технический статус автомобиля
        if (isPassed) {
            // Если техосмотр пройден
            car.setTechnicalStatus(TechnicalStatus.OK);
            car.setInRepair(false);
            System.out.println(" Автомобиль " + car.getLicensePlate() +
                    " прошел техосмотр. Статус: OK");
        } else {
            // Если техосмотр не пройден
            car.setTechnicalStatus(TechnicalStatus.NEEDS_REPAIR);
            car.setInRepair(true);
            System.out.println(" Автомобиль " + car.getLicensePlate() +
                    " не прошел техосмотр. Статус: NEEDS_REPAIR");
        }

        // Сохраняем изменения в автомобиле
        carRepository.update(car);

        // Сохраняем техосмотр
        TechnicalInspection savedInspection = inspectionRepository.save(inspection);

        System.out.println(" Техосмотр создан: #" + savedInspection.getId());
        System.out.println(" Автомобиль: " + car.getLicensePlate());
        System.out.println(" Механик: " + mechanic.getFullName());
        System.out.println(" Результат: " + (isPassed ? "Пройден" : "Не пройден"));

        return savedInspection;
    }

    /**
     * Быстрое создание техосмотра (для сценариев)
     */
    public TechnicalInspection quickCreateInspection(Car car, User mechanic, boolean isPassed, String notes) {
        TechnicalInspection inspection = new TechnicalInspection();
        inspection.setCar(car);
        inspection.setMechanic(mechanic);
        inspection.setIsPassed(isPassed);
        inspection.setNotes(notes);
        inspection.setInspectionDate(LocalDateTime.now());

        return inspectionRepository.save(inspection);
    }

    /**
     * Получить техосмотры по статусу
     */
    public List<TechnicalInspection> getInspectionsByStatus(boolean isPassed) {
        return findAllWithCars().stream()
                .filter(inspection -> inspection.getIsPassed() == isPassed)
                .collect(Collectors.toList());
    }

    /**
     * Получить последний техосмотр автомобиля
     */
    public TechnicalInspection getLastInspectionForCar(Long carId) {
        return getInspectionsByCarId(carId).stream()
                .sorted((i1, i2) -> i2.getInspectionDate().compareTo(i1.getInspectionDate()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Проверить, исправен ли автомобиль (прошел техосмотр)
     */
    public boolean isCarOperational(Long carId) {
        try {
            List<TechnicalInspection> inspections = getInspectionsByCarId(carId);
            if (inspections.isEmpty()) {
                return false;
            }

            // Берем последний техосмотр
            TechnicalInspection lastInspection = inspections.stream()
                    .sorted((i1, i2) -> i2.getInspectionDate().compareTo(i1.getInspectionDate()))
                    .findFirst()
                    .orElse(null);

            // Проверяем, что техосмотр пройден
            return lastInspection != null
                    && lastInspection.getIsPassed() != null
                    && lastInspection.getIsPassed();
        } catch (Exception e) {
            System.err.println("Ошибка при проверке автомобиля: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получить статистику по техосмотрам
     */
    public InspectionStats getStatistics() {
        List<TechnicalInspection> inspections = findAllWithCars();

        long total = inspections.size();
        long passed = inspections.stream().filter(TechnicalInspection::getIsPassed).count();
        long failed = total - passed;

        double avgMileage = inspections.stream()
                .filter(i -> i.getMileageKm() != null)
                .mapToInt(TechnicalInspection::getMileageKm)
                .average()
                .orElse(0.0);

        return new InspectionStats(total, passed, failed, avgMileage);
    }

    /**
     * Вспомогательный класс для статистики
     */
    public static class InspectionStats {
        public final long total;
        public final long passed;
        public final long failed;
        public final double averageMileage;

        public InspectionStats(long total, long passed, long failed, double averageMileage) {
            this.total = total;
            this.passed = passed;
            this.failed = failed;
            this.averageMileage = averageMileage;
        }
    }

    /**
     * Получить список всех автомобилей
     */
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    /**
     * Получить список всех механиков
     */
    public List<User> getAllMechanics() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "SELECT u FROM User u WHERE u.userType = 'MECHANIC' ORDER BY u.fullName",  // Исправлено!
                    User.class
            );
            return query.list();
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке механиков: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Получить исправные автомобили (прошли техосмотр)
     */
    public List<Car> getOperationalCars() {
        List<Car> allCars = carRepository.findAll();

        return allCars.stream()
                .filter(car -> {
                    TechnicalInspection lastInspection = getLastInspectionForCar(car.getId());
                    return lastInspection != null
                            && lastInspection.getIsPassed() != null
                            && lastInspection.getIsPassed();
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить неисправные автомобили (не прошли техосмотр)
     */
    public List<Car> getNonOperationalCars() {
        List<Car> allCars = carRepository.findAll();

        return allCars.stream()
                .filter(car -> {
                    TechnicalInspection lastInspection = getLastInspectionForCar(car.getId());
                    return lastInspection != null
                            && lastInspection.getIsPassed() != null
                            && !lastInspection.getIsPassed();
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить автомобили без техосмотра
     */
    public List<Car> getCarsWithoutInspection() {
        List<Car> allCars = carRepository.findAll();

        return allCars.stream()
                .filter(car -> {
                    List<TechnicalInspection> inspections = getInspectionsByCarId(car.getId());
                    return inspections.isEmpty();
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить отфильтрованные техосмотры
     */
    public List<TechnicalInspection> getFilteredInspections(Long carId, LocalDate date,
                                                            Boolean status, String search) {
        List<TechnicalInspection> allInspections = findAllWithCars();

        return allInspections.stream()
                .filter(inspection -> {
                    // Фильтр по автомобилю
                    if (carId != null && !inspection.getCar().getId().equals(carId)) {
                        return false;
                    }

                    // Фильтр по дате
                    if (date != null && !inspection.getInspectionDate().toLocalDate().equals(date)) {
                        return false;
                    }

                    // Фильтр по статусу
                    if (status != null && !inspection.getIsPassed().equals(status)) {
                        return false;
                    }

                    // Поиск по тексту
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = inspection.getCar().getLicensePlate().toLowerCase().contains(searchLower) ||
                                inspection.getCar().getModel().toLowerCase().contains(searchLower) ||
                                inspection.getCar().getBrand().toLowerCase().contains(searchLower) ||
                                inspection.getMechanic().getFullName().toLowerCase().contains(searchLower) ||
                                (inspection.getNotes() != null && inspection.getNotes().toLowerCase().contains(searchLower));
                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((i1, i2) -> i2.getInspectionDate().compareTo(i1.getInspectionDate()))
                .collect(Collectors.toList());
    }

    /**
     * Получить количество исправных автомобилей
     */
    public long getOperationalCarsCount() {
        return getOperationalCars().size();
    }

    /**
     * Получить количество неисправных автомобилей
     */
    public long getNonOperationalCarsCount() {
        return getNonOperationalCars().size();
    }

    /**
     * Получить количество автомобилей без техосмотра
     */
    public long getCarsWithoutInspectionCount() {
        return getCarsWithoutInspection().size();
    }

    /**
     * Удалить старые техосмотры (старше 2 лет)
     */
    public int deleteOldInspections() {
        LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
        List<TechnicalInspection> allInspections = findAllWithCars();

        int deletedCount = 0;
        for (TechnicalInspection inspection : allInspections) {
            if (inspection.getInspectionDate().isBefore(twoYearsAgo)) {
                delete(inspection.getId());
                deletedCount++;
            }
        }
        return deletedCount;
    }
}