package com.taxi.repository;

import com.taxi.entity.Waybill;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class WaybillRepository {

    /**
     * Получить все путевые листы
     */
    public List<Waybill> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Waybill w ORDER BY w.startTime DESC",
                    Waybill.class
            ).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти путевой лист по ID
     */
    public Waybill findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Waybill.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Найти активный путевой лист водителя
     */
    public Waybill findActiveByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Waybill w WHERE w.driver.id = :driverId AND w.status = 'ACTIVE'",
                            Waybill.class
                    )
                    .setParameter("driverId", driverId)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Найти активный путевой лист автомобиля
     */
    public Waybill findActiveByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Waybill w WHERE w.car.id = :carId AND w.status = 'ACTIVE'",
                            Waybill.class
                    )
                    .setParameter("carId", carId)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Найти путевые листы водителя
     */
    public List<Waybill> findByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Waybill w WHERE w.driver.id = :driverId ORDER BY w.startTime DESC",
                    Waybill.class
            ).setParameter("driverId", driverId).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти активные путевые листы
     */
    public List<Waybill> findActive() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Waybill w WHERE w.status = 'ACTIVE' ORDER BY w.startTime",
                    Waybill.class
            ).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти завершенные путевые листы
     */
    public List<Waybill> findCompleted() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Waybill w WHERE w.status = 'COMPLETED' ORDER BY w.endTime DESC",
                    Waybill.class
            ).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти путевые листы за период
     */
    public List<Waybill> findByPeriod(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Waybill w WHERE w.startTime BETWEEN :startDate AND :endDate ORDER BY w.startTime DESC",
                            Waybill.class
                    )
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Получить путевые листы водителя за период
     */
    public List<Waybill> findByDriverAndPeriod(Long driverId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Waybill w WHERE w.driver.id = :driverId " +
                                    "AND w.startTime BETWEEN :startDate AND :endDate " +
                                    "ORDER BY w.startTime DESC",
                            Waybill.class
                    )
                    .setParameter("driverId", driverId)
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Сохранить путевой лист
     */
    public Waybill save(Waybill waybill) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (waybill.getId() == null) {
                session.persist(waybill);
            } else {
                waybill = session.merge(waybill);
            }
            transaction.commit();
            return waybill;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Ошибка сохранения путевого листа", e);
        }
    }

    /**
     * Обновить путевой лист
     */
    public Waybill update(Waybill waybill) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            waybill = session.merge(waybill);
            transaction.commit();
            return waybill;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Ошибка обновления путевого листа", e);
        }
    }

    /**
     * Удалить путевой лист
     */
    public boolean delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Waybill waybill = session.get(Waybill.class, id);
            if (waybill != null) {
                session.remove(waybill);
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
     * Получить количество активных путевых листов
     */
    public long countActive() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return (Long) session.createQuery(
                            "SELECT COUNT(w) FROM Waybill w WHERE w.status = 'ACTIVE'")
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Получить общий заработок по всем завершенным сменам
     */
    public double getTotalEarnings() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = (Double) session.createQuery(
                            "SELECT SUM(w.totalEarnings) FROM Waybill w WHERE w.status = 'COMPLETED'")
                    .uniqueResult();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Получить общий пробег по всем завершенным сменам
     */
    public int getTotalMileage() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long total = (Long) session.createQuery(
                            "SELECT SUM(w.finalMileageKm - w.initialMileageKm) FROM Waybill w WHERE w.status = 'COMPLETED'")
                    .uniqueResult();
            return total != null ? total.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // метод для исправления существующих записей в БД
    public void fixNullFields() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            List<Waybill> waybills = session.createQuery("FROM Waybill", Waybill.class).list();
            for (Waybill waybill : waybills) {
                if (waybill.getTotalDistance() == null) waybill.setTotalDistance(0.0);
                if (waybill.getTotalRevenue() == null) waybill.setTotalRevenue(0.0);
                if (waybill.getTotalEarnings() == null) waybill.setTotalEarnings(0.0);
                if (waybill.getOrdersCount() == null) waybill.setOrdersCount(0);
                session.merge(waybill);
            }

            transaction.commit();
            System.out.println("✅ Исправлены null-поля в " + waybills.size() + " путевых листах");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}