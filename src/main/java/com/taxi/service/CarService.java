package com.taxi.service;

import com.taxi.entity.Car;
import com.taxi.entity.TechnicalInspection;
import com.taxi.repository.TechnicalInspectionRepository;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CarService {

    public List<Car> getAllCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Car", Car.class).list();
        }
    }

    public Car getCarById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Car.class, id);
        }
    }

    public boolean createCar(Car car) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            session.persist(car);
            transaction.commit();

            return car.getId() != null;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при создании автомобиля", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void updateCar(Car car) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            session.merge(car);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении автомобиля", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public boolean deleteCar(Long id) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            Car car = session.get(Car.class, id);
            if (car != null) {
                session.remove(car);
            }

            transaction.commit();
            return car != null;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при удалении автомобиля", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List<Car> getAvailableCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Car c WHERE c.isActive = true AND c.inRepair = false AND c.id NOT IN " +
                            "(SELECT d.currentCar.id FROM Driver d WHERE d.currentCar IS NOT NULL)",
                    Car.class
            ).list();
        }
    }

    public List<Car> getCarsInRepair() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Car c WHERE c.inRepair = true",
                    Car.class
            ).list();
        }
    }

    public List<Car> getActiveCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Car c WHERE c.isActive = true",
                    Car.class
            ).list();
        }
    }

    /**
     * Получить общее количество автомобилей
     */
    public long getTotalCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(c) FROM Car c",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    /**
     * Получить список доступных автомобилей (для DispatcherPanelServlet)
     */
    public List<Car> getAvailableCarsList() {
        return getAvailableCars();
    }

    /**
     * Получить количество активных автомобилей
     */
    public long getActiveCarsCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(c) FROM Car c WHERE c.isActive = true",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    /**
     * Получить количество автомобилей в ремонте
     */
    public long getCarsInRepairCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(c) FROM Car c WHERE c.inRepair = true",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    /**
     * Получить количество доступных автомобилей
     */
    public long getAvailableCarsCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(c) FROM Car c WHERE c.isActive = true AND c.inRepair = false AND c.id NOT IN " +
                            "(SELECT d.currentCar.id FROM Driver d WHERE d.currentCar IS NOT NULL)",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0;
        }
    }

    /**
     * Получить средний пробег
     */
    public double getAverageMileage() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double avg = session.createQuery(
                    "SELECT AVG(c.mileageKm) FROM Car c WHERE c.mileageKm IS NOT NULL",
                    Double.class
            ).uniqueResult();
            return avg != null ? avg : 0.0;
        }
    }

    public boolean hasValidInspection(Long carId) {
        TechnicalInspection lastInspection = TechnicalInspectionRepository.findLatestByCarId(carId);
        return lastInspection != null && lastInspection.getIsPassed();
    }
}