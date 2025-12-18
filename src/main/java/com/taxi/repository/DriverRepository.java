package com.taxi.repository;

import com.taxi.entity.Car;
import com.taxi.entity.Driver;
import com.taxi.entity.User;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class DriverRepository {

    // Получить всех водителей
    public List<Driver> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Driver", Driver.class).list();
        }
    }

    // Найти водителя по ID
    public Driver findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Driver.class, id);
        }
    }

    // Сохранить водителя
    public void save(Driver driver) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(driver);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // Удалить водителя
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Driver driver = session.get(Driver.class, id);
            if (driver != null) {
                session.delete(driver);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    public List<Driver> findDriversWithoutCar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Driver d WHERE d.currentCar IS NULL AND d.isActive = true",
                    Driver.class
            ).list();
        }
    }

    // найти водителя по авто
    public List<Driver> findDriversByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Driver d WHERE d.currentCar.id = :carId",
                    Driver.class
            ).setParameter("carId", carId).list();
        }
    }

    // назначить авто
    public void assignCarToDriver(Long driverId, Long carId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            // Находим водителя и автомобиль
            Driver driver = session.get(Driver.class, driverId);
            Car car = session.get(Car.class, carId);

            if (driver != null && car != null) {
                driver.setCurrentCar(car);
                session.saveOrUpdate(driver);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // отвязать авто
    public void unassignCarFromDriver(Long driverId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Driver driver = session.get(Driver.class, driverId);
            if (driver != null) {
                driver.setCurrentCar(null);
                session.saveOrUpdate(driver);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Обновить информацию о водителе
     */
    public void update(Driver driver) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(driver); // Используем merge для обновления
            transaction.commit();
            System.out.println(" Водитель обновлен: " + driver.getFullName());
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println(" Ошибка при обновлении водителя: " + e.getMessage());
            throw e;
        }
    }

    public Driver findByLicenseNumber(String licenseNumber) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Driver WHERE licenseNumber = :license", Driver.class
            ).setParameter("license", licenseNumber).uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске водителя по номеру прав: " + e.getMessage());
            return null;
        }
    }

    /**
     * Найти водителя по ID пользователя (ОБНОВЛЕНО для связи с User)
     */
    public Driver findByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d WHERE d.user.id = :userId",
                            Driver.class
                    ).setParameter("userId", userId)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске водителя по ID пользователя: " + e.getMessage());
            return null;
        }
    }

    /**
     * Найти всех водителей с информацией о пользователях
     */
    public List<Driver> findAllWithUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Driver d LEFT JOIN FETCH d.user ORDER BY d.fullName", Driver.class)
                    .list();
        }
    }

    // Найти пользователей без привязанных водителей
    public List<User> findUsersWithoutDriver(String userType) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT u FROM User u " +
                                    "LEFT JOIN Driver d ON d.user.id = u.id " +
                                    "WHERE u.userType = :userType AND d.id IS NULL " +
                                    "ORDER BY u.fullName",
                            User.class
                    ).setParameter("userType", userType)
                    .list();
        }
    }
}
