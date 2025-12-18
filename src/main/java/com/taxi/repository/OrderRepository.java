package com.taxi.repository;

import com.taxi.entity.Order;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    // Существующие методы
    public List<Order> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Order ORDER BY orderTime DESC", Order.class).list();
        }
    }

    public Order findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Order.class, id);
        }
    }

    public Order save(Order order) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                if (order.getId() == null) {
                    session.persist(order);
                } else {
                    order = session.merge(order);
                }
                tx.commit();

                // Отладочный вывод
                System.out.println(" Заказ сохранен: #" + order.getId() +
                        " | Путевой лист: " + (order.getWaybill() != null ?
                        "#" + order.getWaybill().getId() : "не привязан"));

                return order;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            System.out.println(" Ошибка при сохранении заказа: " + e.getMessage());
            throw e;
        }
    }

    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Order order = session.get(Order.class, id);
            if (order != null) {
                session.delete(order);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Найти заказы по водителю
     */
    public List<Order> findByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.driver.id = :driverId ORDER BY o.orderTime DESC",
                            Order.class)
                    .setParameter("driverId", driverId)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти заказы по статусу
     */
    public List<Order> findByStatus(String status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.status = :status ORDER BY o.orderTime DESC",
                            Order.class)
                    .setParameter("status", status)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти активные заказы водителя
     */
    public List<Order> findActiveByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.driver.id = :driverId " +
                                    "AND o.status IN ('ASSIGNED', 'IN_PROGRESS') " +
                                    "ORDER BY o.orderTime",
                            Order.class)
                    .setParameter("driverId", driverId)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти новые (не назначенные) заказы
     */
    public List<Order> findNewOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.status = 'NEW' ORDER BY o.orderTime",
                            Order.class)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти заказы за период
     */
    public List<Order> findByPeriod(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.orderTime BETWEEN :startDate AND :endDate " +
                                    "ORDER BY o.orderTime DESC",
                            Order.class)
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Найти завершенные заказы водителя за период
     */
    public List<Order> findCompletedByDriverAndPeriod(Long driverId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.driver.id = :driverId " +
                                    "AND o.status = 'COMPLETED' " +
                                    "AND o.completionTime BETWEEN :startDate AND :endDate " +
                                    "ORDER BY o.completionTime DESC",
                            Order.class)
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
     * Получить статистику заказов по водителю
     */
    public Object[] getDriverStats(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return (Object[]) session.createQuery(
                            "SELECT COUNT(o), SUM(o.distanceKm), SUM(o.price) " +
                                    "FROM Order o WHERE o.driver.id = :driverId AND o.status = 'COMPLETED'")
                    .setParameter("driverId", driverId)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[]{0L, 0.0, 0.0};
        }
    }

    /**
     * Найти заказы водителя за период
     */
    public List<Order> findByDriverAndPeriod(Long driverId, LocalDateTime start, LocalDateTime end) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.driver.id = :driverId " +
                                    "AND o.createdAt >= :start AND o.createdAt <= :end " +
                                    "ORDER BY o.createdAt DESC", Order.class)
                    .setParameter("driverId", driverId)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .list();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске заказов водителя за период: " + e.getMessage());
            return List.of();
        }
    }

    public List<Order> findByWaybillId(Long waybillId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Order o WHERE o.waybill.id = :waybillId " +
                    "AND o.status = 'COMPLETED' " +
                    "ORDER BY o.completionTime DESC";
            return session.createQuery(hql, Order.class)
                    .setParameter("waybillId", waybillId)
                    .list();
        } catch (Exception e) {
            System.out.println("Ошибка при поиске заказов по путевому листу: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}