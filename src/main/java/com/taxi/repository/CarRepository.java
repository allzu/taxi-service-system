package com.taxi.repository;

import com.taxi.entity.Car;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CarRepository {

    /**
     * Получить все автомобили
     */


    public List<Car> findAll() {
        System.out.println("CarRepository.findAll() вызван");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            System.out.println(" Сессия Hibernate открыта");

            List<Car> cars = session.createQuery("FROM Car", Car.class).list();
            System.out.println("Найдено записей: " + cars.size());

            return cars;

        } catch (Exception e) {
            System.err.println(" ОШИБКА в CarRepository.findAll(): " + e.getMessage());
            e.printStackTrace();
            return List.of(); // возвращаем пустой список вместо null
        }
    }

    /**
     * Найти автомобиль по ID
     */
    public Car findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Car.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Сохранить автомобиль
     */
    public Car save(Car car) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (car.getId() == null) {
                session.persist(car);
            } else {
                car = session.merge(car);
            }
            transaction.commit();
            return car;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Ошибка сохранения автомобиля", e);
        }
    }

    /**
     * Обновить автомобиль
     */
    public Car update(Car car) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            car = session.merge(car);
            transaction.commit();
            return car;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Ошибка обновления автомобиля", e);
        }
    }

    /**
     * Удалить автомобиль
     */
    public boolean delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Car car = session.get(Car.class, id);
            if (car != null) {
                session.remove(car);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Найти автомобили по модели
     */
    public List<Car> findByModel(String model) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car WHERE LOWER(model) LIKE LOWER(:model) ORDER BY licensePlate",
                            Car.class)
                    .setParameter("model", "%" + model + "%")
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти автомобиль по номеру
     */
    public Car findByLicensePlate(String licensePlate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car WHERE licensePlate = :licensePlate",
                            Car.class)
                    .setParameter("licensePlate", licensePlate)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Получить автомобили с пробегом больше указанного
     */
    public List<Car> findByMileageGreaterThan(int mileage) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car WHERE mileageKm > :mileage ORDER BY mileageKm DESC",
                            Car.class)
                    .setParameter("mileage", mileage)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Обновить пробег автомобиля
     */
    public boolean updateMileage(Long carId, int newMileage) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Car car = session.get(Car.class, carId);
            if (car != null) {
                car.setMileageKm(newMileage);
                session.merge(car);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Получить общее количество автомобилей
     */
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return (Long) session.createQuery("SELECT COUNT(c) FROM Car c").uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Получить средний пробег автомобилей
     */
    public double getAverageMileage() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double avg = (Double) session.createQuery("SELECT AVG(c.mileageKm) FROM Car c").uniqueResult();
            return avg != null ? avg : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Найти доступные автомобили (не в ремонте и активные)
     */
    public List<Car> findAvailableCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car c WHERE c.isActive = true AND c.inRepair = false ORDER BY c.licensePlate",
                            Car.class)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти автомобили, требующие техосмотра (пробег > 15000 с последнего осмотра)
     * Это упрощенный метод, реальная логика должна проверять дату последнего техосмотра
     */
    public List<Car> findCarsRequiringInspection() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car c WHERE c.mileageKm > 15000 AND c.isActive = true ORDER BY c.mileageKm DESC",
                            Car.class)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти автомобили в ремонте
     */
    public List<Car> findCarsInRepair() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car c WHERE c.inRepair = true ORDER BY c.licensePlate",
                            Car.class)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти активные автомобили
     */
    public List<Car> findActiveCars() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Car c WHERE c.isActive = true ORDER BY c.licensePlate",
                            Car.class)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}