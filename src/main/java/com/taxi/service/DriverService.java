package com.taxi.service;

import com.taxi.entity.*;
import com.taxi.repository.DriverRepository;
import com.taxi.repository.CarRepository;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class DriverService {
    private DriverRepository driverRepository = new DriverRepository();
    private CarRepository carRepository;
    private UserService userService = new UserService(); // Добавляем для работы с пользователями

    public DriverService() {
        this.driverRepository = new DriverRepository();
        this.carRepository = new CarRepository();
    }

    public List<Driver> getAllDrivers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Driver", Driver.class).list();
        }
    }

    public Driver getDriverById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Driver.class, id);
        }
    }

    public Long createDriver(Driver driver) {
        Session session = null;
        Transaction transaction = null;
        try {
            System.out.println(" Начинаем создание водителя: " + driver.getFullName());
            System.out.println("    Данные: права=" + driver.getLicenseNumber() + ", тел=" + driver.getPhone());

            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            System.out.println("    Сохраняем водителя в БД...");
            session.persist(driver);
            session.flush(); // Принудительно сохраняем, чтобы получить ID

            transaction.commit();

            System.out.println(" Водитель создан: " + driver.getFullName() + " (ID: " + driver.getId() + ")");
            return driver.getId();

        } catch (Exception e) {
            System.err.println(" КРИТИЧЕСКАЯ ОШИБКА при создании водителя:");
            System.err.println("   Имя: " + driver.getFullName());
            System.err.println("   Права: " + driver.getLicenseNumber());
            System.err.println("   Причина: " + e.getMessage());
            e.printStackTrace();

            if (transaction != null) {
                try {
                    transaction.rollback();
                    System.out.println("    Транзакция откатана");
                } catch (Exception rollbackEx) {
                    System.err.println("   Ошибка при откате: " + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("Ошибка при создании водителя: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
                System.out.println("    Сессия закрыта");
            }
        }
    }

    public void updateDriver(Driver driver) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            session.merge(driver);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении водителя", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void deleteDriver(Long id) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            Driver driver = session.get(Driver.class, id);

            if (driver != null) {
                System.out.println(" Удаляем водителя: " + driver.getFullName() + " (ID: " + id + ")");

                // 1. Находим и обнуляем все заказы этого водителя
                System.out.println("    Ищем заказы водителя...");
                List<Order> driverOrders = session.createQuery(
                                "FROM Order o WHERE o.driver.id = :driverId", Order.class)
                        .setParameter("driverId", id)
                        .list();

                if (!driverOrders.isEmpty()) {
                    System.out.println("    Обнуляем водителя в " + driverOrders.size() + " заказах");
                    for (Order order : driverOrders) {
                        order.setDriver(null);
                        session.merge(order);
                    }
                }

                // 2. Отвязываем автомобиль
                if (driver.getCurrentCar() != null) {
                    System.out.println("    Отвязываем автомобиль: " + driver.getCurrentCar().getLicensePlate());
                    driver.setCurrentCar(null);
                    session.merge(driver);
                }

                // 3. Отвязываем пользователя
                if (driver.getUser() != null) {
                    System.out.println("    Отвязываем пользователя: " + driver.getUser().getLogin());
                    driver.setUser(null);
                    session.merge(driver);
                }

                // 4. Удаляем медосмотры водителя
                System.out.println("    Удаляем медосмотры...");
                session.createQuery("DELETE FROM MedicalCheck m WHERE m.driver.id = :driverId")
                        .setParameter("driverId", id)
                        .executeUpdate();

                // 5. Удаляем путевые листы водителя
                System.out.println("    Удаляем путевые листы...");
                session.createQuery("DELETE FROM Waybill w WHERE w.driver.id = :driverId")
                        .setParameter("driverId", id)
                        .executeUpdate();

                // 6. Теперь можно удалить самого водителя
                System.out.println("    Удаляем водителя...");
                session.remove(driver);

                transaction.commit();
                System.out.println(" Водитель и все связанные данные успешно удалены");

            } else {
                System.out.println(" Водитель с ID " + id + " не найден");
                transaction.rollback();
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println(" Ошибка при удалении водителя (ID: " + id + "): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Ошибка при удалении водителя: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public List<Driver> getDriversWithoutCar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Driver d WHERE d.currentCar IS NULL AND d.isActive = true",
                    Driver.class
            ).list();
        }
    }

    public void assignCarToDriver(Long driverId, Long carId) {
        Driver driver = driverRepository.findById(driverId);
        Car car = carRepository.findById(carId);

        if (driver == null || car == null) {
            throw new IllegalArgumentException("Водитель или автомобиль не найден");
        }

        // ПРОВЕРКА: Автомобиль должен быть исправен
        if (!car.isOperational()) {
            throw new IllegalStateException("Автомобиль не исправен. Требуется техосмотр.");
        }

        // ПРОВЕРКА: Автомобиль не должен быть занят другим водителем
        if (car.getCurrentDriver() != null && !car.getCurrentDriver().getId().equals(driverId)) {
            throw new IllegalStateException("Автомобиль уже назначен другому водителю: " +
                    car.getCurrentDriver().getFullName());
        }

        driver.setCurrentCar(car);
        driverRepository.update(driver);

        System.out.println(" Автомобиль " + car.getLicensePlate() +
                " назначен водителю " + driver.getFullName());
    }

    public void unassignCarFromDriver(Long driverId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            Driver driver = session.get(Driver.class, driverId);
            if (driver != null) {
                driver.setCurrentCar(null);
                session.merge(driver);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при снятии автомобиля с водителя", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    // ИЗМЕНЕНИЕ: Обновляем метод для новой связи
    public Driver findByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.user.id = :userId",
                            Driver.class
                    ).setParameter("userId", userId)
                    .uniqueResult();
        }
    }

    public List<Driver> getAvailableDrivers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.isActive = true AND " +
                                    "(d.medicalStatus = :passed1 OR d.medicalStatus = :passed2)",
                            Driver.class
                    ).setParameter("passed1", MedicalStatus.PASSED)
                    .setParameter("passed2", MedicalStatus.PASSED)
                    .list();
        }
    }

    public List<Driver> getDriversByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.currentCar.id = :carId",
                            Driver.class
                    ).setParameter("carId", carId)
                    .list();
        }
    }

    // === НОВЫЕ МЕТОДЫ ДЛЯ СВЯЗИ С ПОЛЬЗОВАТЕЛЯМИ ===

    /**
     * Получить пользователей без привязанных водителей
     */
    public List<User> getAvailableUsersForDriver() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT u FROM User u " +
                            "LEFT JOIN Driver d ON d.user.id = u.id " +
                            "WHERE u.userType = 'DRIVER' AND d.id IS NULL " +
                            "ORDER BY u.fullName",
                    User.class
            ).list();
        }
    }

    /**
     * Сохранить водителя с привязкой к пользователю
     */
    public Long saveDriverWithUser(Driver driver, Long userId) {
        try {
            System.out.println(" Привязываем пользователя к водителю...");
            System.out.println("   Водитель: " + driver.getFullName());
            System.out.println("   User ID: " + userId);

            User user = userService.getUserById(userId);
            if (user != null) {
                System.out.println("    Найден пользователь: " + user.getFullName() + " (" + user.getLogin() + ")");
                driver.setUser(user);
            } else {
                System.out.println("   ️ Пользователь с ID=" + userId + " не найден, создаем без привязки");
            }

            return createDriver(driver);

        } catch (Exception e) {
            System.err.println(" Ошибка при связывании водителя с пользователем: " + e.getMessage());
            throw new RuntimeException("Ошибка при создании водителя с пользователем: " + e.getMessage(), e);
        }
    }

    /**
     * Обновить водителя с привязкой к пользователю
     */
    public void updateDriverWithUser(Driver driver, Long userId) {
        if (userId != null && !userId.equals(0L)) {
            User user = userService.getUserById(userId);
            if (user != null) {
                driver.setUser(user);
                System.out.println(" Обновлена привязка к пользователю: " + user.getFullName());
            }
        } else {
            driver.setUser(null); // Отвязать пользователя
            System.out.println(" Пользователь отвязан от водителя");
        }
        updateDriver(driver);
    }

    public long getTotalDrivers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(d) FROM Driver d",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    public long getActiveDriversCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(d) FROM Driver d WHERE d.isActive = true",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    public long getDriversWithMedicalCheckCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(d) FROM Driver d WHERE d.medicalStatus = :status",
                            Long.class
                    ).setParameter("status", MedicalStatus.PASSED)
                    .uniqueResult();
            return count != null ? count : 0;
        }
    }

    public List<Car> getAvailableCars() {
        System.out.println("\n ДЕБАГ: ПОИСК ДОСТУПНЫХ АВТОМОБИЛЕЙ ");

        try {
            if (carRepository == null) {
                System.out.println(" carRepository is NULL!");
                return List.of();
            }

            System.out.println(" carRepository загружен");
            List<Car> allCars = carRepository.findAll();
            System.out.println("📊 Всего автомобилей в БД: " + allCars.size());

            if (allCars.isEmpty()) {
                System.out.println(" В БД нет автомобилей!");
                return List.of();
            }

            System.out.println("\n📋 СПИСОК ВСЕХ АВТОМОБИЛЕЙ:");
            for (Car car : allCars) {
                System.out.println(String.format(
                        " ID: %d, %s %s (%s) | " +
                                "Активен: %s | " +
                                "В ремонте: %s | " +
                                "Водитель: %s | " +
                                "Статус: %s",
                        car.getId(),
                        car.getBrand(),
                        car.getModel(),
                        car.getLicensePlate(),
                        car.getIsActive(),
                        car.getInRepair(),
                        (car.getCurrentDriver() != null ? car.getCurrentDriver().getFullName() : "НЕТ"),
                        car.getTechnicalStatus()
                ));
            }

            List<Car> availableCars = allCars.stream()
                    .filter(car -> {
                        boolean isActive = car.getIsActive() == null || car.getIsActive();
                        if (!isActive) {
                            System.out.println("    " + car.getLicensePlate() + " - не активен");
                        }
                        return isActive;
                    })
                    .filter(car -> {
                        boolean notInRepair = car.getInRepair() == null || !car.getInRepair();
                        if (!notInRepair) {
                            System.out.println("    " + car.getLicensePlate() + " - в ремонте");
                        }
                        return notInRepair;
                    })
                    .filter(car -> {
                        boolean hasNoDriver = car.getCurrentDriver() == null;
                        if (!hasNoDriver) {
                            System.out.println("    " + car.getLicensePlate() + " - занят водителем: " +
                                    car.getCurrentDriver().getFullName());
                        }
                        return hasNoDriver;
                    })
                    .collect(Collectors.toList());

            System.out.println("\n ДОСТУПНЫХ АВТОМОБИЛЕЙ: " + availableCars.size());

            if (availableCars.isEmpty()) {
                System.out.println("⚠ Нет доступных автомобилей! Проверь фильтры выше.");
            } else {
                System.out.println(" ДОСТУПНЫЕ АВТОМОБИЛИ:");
                for (Car car : availableCars) {
                    System.out.println("    " + car.getLicensePlate() + " - " + car.getBrand() + " " + car.getModel());
                }
            }

            return availableCars;

        } catch (Exception e) {
            System.err.println(" КРИТИЧЕСКАЯ ОШИБКА в getAvailableCars(): " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Driver> getDriversWithExpiredMedicalCheck() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.medicalStatus = :expired",
                            Driver.class
                    ).setParameter("expired", MedicalStatus.PASSED)
                    .list();
        }
    }

    public List<Driver> getDriversOnDuty() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Driver d WHERE d.isOnDuty = true",
                    Driver.class
            ).list();
        }
    }

    public List<Driver> getDriversByFilter(String filter) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            switch (filter) {
                case "active":
                    return session.createQuery(
                            "FROM Driver d WHERE d.isActive = true",
                            Driver.class
                    ).list();
                case "inactive":
                    return session.createQuery(
                            "FROM Driver d WHERE d.isActive = false",
                            Driver.class
                    ).list();
                case "with-car":
                    return session.createQuery(
                            "FROM Driver d WHERE d.currentCar IS NOT NULL",
                            Driver.class
                    ).list();
                case "without-car":
                    return session.createQuery(
                            "FROM Driver d WHERE d.currentCar IS NULL",
                            Driver.class
                    ).list();
                default:
                    return getAllDrivers();
            }
        }
    }

    public Driver findDriverByUserName(String userName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.fullName = :userName",
                            Driver.class
                    ).setParameter("userName", userName)
                    .uniqueResult();
        }
    }

    public Driver findDriverByUserId(Long userId) {
        return driverRepository.findByUserId(userId);
    }

    public Driver findDriverByLicenseNumber(String licenseNumber) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver WHERE licenseNumber = :licenseNumber",
                            Driver.class)
                    .setParameter("licenseNumber", licenseNumber)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске водителя по номеру прав: " + e.getMessage());
            return null;
        }
    }
}