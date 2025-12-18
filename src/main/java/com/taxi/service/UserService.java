package com.taxi.service;

import com.taxi.entity.User;
import com.taxi.repository.UserRepository;
import com.taxi.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class UserService {
    private UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            if (user == null) {
                System.out.println("⚠ Пользователь с ID=" + id + " не найден");
            }
            return user;
        } catch (Exception e) {
            System.err.println(" Ошибка при поиске пользователя: " + e.getMessage());
            return null;
        }
    }

    public List<User> getDoctors() {
        return userRepository.findByRole("DOCTOR");
    }

    public List<User> getMechanics() {
        return userRepository.findByRole("MECHANIC");
    }

    public User authenticate(String login, String password) {
        User user = userRepository.findByLogin(login);
        if (user != null && user.getPassword().equals(password) && user.getIsActive()) {
            return user;
        }
        return null;
    }

    public User findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public User findById(Long id) {
        return userRepository.findById(id);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void update(User user) {
        userRepository.update(user);
    }

    public void delete(Long id) {
        userRepository.delete(id);
    }

    /**
     * Получить общее количество пользователей
     */
    public long getTotalUsers() {
        return userRepository.getTotalUsers();
    }

    /**
     * Получить количество активных пользователей
     */
    public long getActiveUsersCount() {
        return getAllUsers().stream()
                .filter(User::getIsActive)
                .count();
    }

    /**
     * Получить пользователей по роли
     */
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }
}