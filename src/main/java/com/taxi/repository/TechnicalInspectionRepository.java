package com.taxi.repository;

import com.taxi.entity.TechnicalInspection;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class TechnicalInspectionRepository {

    // Получить все техосмотры
    public List<TechnicalInspection> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM TechnicalInspection ti ORDER BY ti.inspectionDate DESC",
                    TechnicalInspection.class
            ).list();
        }
    }

    // Найти техосмотр по ID
    public TechnicalInspection findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(TechnicalInspection.class, id);
        }
    }

    // Найти техосмотры автомобиля
    public List<TechnicalInspection> findByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM TechnicalInspection ti WHERE ti.car.id = :carId ORDER BY ti.inspectionDate DESC",
                    TechnicalInspection.class
            ).setParameter("carId", carId).list();
        }
    }

    // Получить последний техосмотр автомобиля
    public static TechnicalInspection findLatestByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM TechnicalInspection ti WHERE ti.car.id = :carId ORDER BY ti.inspectionDate DESC",
                            TechnicalInspection.class
                    )
                    .setParameter("carId", carId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // Получить последний исправный техосмотр автомобиля
    public TechnicalInspection findLatestPassedByCarId(Long carId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM TechnicalInspection ti WHERE ti.car.id = :carId AND ti.isPassed = true ORDER BY ti.inspectionDate DESC",
                            TechnicalInspection.class
                    )
                    .setParameter("carId", carId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // Сохранить техосмотр
    public TechnicalInspection save(TechnicalInspection inspection) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(inspection);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
        return inspection;
    }

    // Обновить техосмотр
    public void update(TechnicalInspection inspection) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(inspection);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // Удалить техосмотр
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            TechnicalInspection inspection = session.get(TechnicalInspection.class, id);
            if (inspection != null) {
                session.delete(inspection);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }
}