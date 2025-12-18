package com.taxi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "access_code")
    private Integer accessCode;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Конструкторы
    public User() { // Пустой конструктор обязателен для Hibernate
    }

    public User(String fullName, String login, String password, String userType, Integer accessCode) {
        this.fullName = fullName;
        this.login = login;
        this.password = password;
        this.userType = userType;
        this.accessCode = accessCode;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Integer getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(Integer accessCode) {
        this.accessCode = accessCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // Метод toString для удобства отладки
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", login='" + login + '\'' +
                ", userType='" + userType + '\'' +
                ", accessCode=" + accessCode +
                ", isActive=" + isActive +
                '}';
    }
}