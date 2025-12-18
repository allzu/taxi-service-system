package com.taxi.repository;

import com.taxi.entity.MedicalCheck;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MedicalCheckRepository {

    // Получить все медосмотры
    public List<MedicalCheck> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicalCheck mc ORDER BY mc.checkDate DESC",
                    MedicalCheck.class
            ).list();
        }
    }

    // Найти медосмотр по ID
    public MedicalCheck findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(MedicalCheck.class, id);
        }
    }

    // Найти медосмотры водителя
    public List<MedicalCheck> findByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicalCheck mc WHERE mc.driver.id = :driverId ORDER BY mc.checkDate DESC",
                    MedicalCheck.class
            ).setParameter("driverId", driverId).list();
        }
    }

    // Получить последний медосмотр водителя
    public MedicalCheck findLatestByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM MedicalCheck mc WHERE mc.driver.id = :driverId ORDER BY mc.checkDate DESC",
                            MedicalCheck.class
                    )
                    .setParameter("driverId", driverId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // Получить последний допускающий медосмотр водителя
    public MedicalCheck findLatestPassedByDriverId(Long driverId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM MedicalCheck mc WHERE mc.driver.id = :driverId AND mc.isPassed = true ORDER BY mc.checkDate DESC",
                            MedicalCheck.class
                    )
                    .setParameter("driverId", driverId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // Сохранить медосмотр
    public void save(MedicalCheck medicalCheck) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(medicalCheck);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // Обновить медосмотр
    public void update(MedicalCheck medicalCheck) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(medicalCheck);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // Удалить медосмотр
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            MedicalCheck medicalCheck = session.get(MedicalCheck.class, id);
            if (medicalCheck != null) {
                session.delete(medicalCheck);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }
}