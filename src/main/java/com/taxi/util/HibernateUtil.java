package com.taxi.util;

import com.taxi.entity.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static final SessionFactory sessionFactory;

    static {
        try {
            log.info(" Инициализация Hibernate...");

            Configuration configuration = new Configuration();
            configuration.configure(); // Загружает hibernate.cfg.xml

            // Добавляем entity классы вручную (на всякий случай)
            configuration.addAnnotatedClass(com.taxi.entity.Driver.class);
            configuration.addAnnotatedClass(com.taxi.entity.Car.class);
            configuration.addAnnotatedClass(com.taxi.entity.MedicalCheck.class);
            configuration.addAnnotatedClass(com.taxi.entity.Order.class);
            configuration.addAnnotatedClass(com.taxi.entity.TechnicalInspection.class);
            configuration.addAnnotatedClass(com.taxi.entity.User.class);
            configuration.addAnnotatedClass(com.taxi.entity.Waybill.class);

            sessionFactory = configuration.buildSessionFactory();

            log.info(" Hibernate успешно инициализирован");

        } catch (Throwable ex) {
            log.error(" Ошибка инициализации Hibernate: {}", ex.getMessage());
            log.error("Детали ошибки:", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            log.info(" Hibernate сессия закрыта");
        }
    }
}