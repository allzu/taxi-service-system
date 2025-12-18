package com.taxi.controller;

import com.taxi.entity.User;
import com.taxi.service.*;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.taxi.entity.Driver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/admin")
public class AdminPanelServlet extends HttpServlet {

    private UserService userService = new UserService();
    private OrderService orderService = new OrderService();
    private DriverService driverService = new DriverService();
    private CarService carService = new CarService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Проверка авторизации и роли
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        // Только ADMIN имеет доступ
        if (!"ADMIN".equals(currentUser.getUserType())) {
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            HtmlUtil.renderAccessDeniedPage(out, request, currentUser.getUserType());
            return;
        }

        // Устанавливаем кодировку
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Собираем контент и рендерим страницу
        String content = buildAdminContent(request);
        HtmlUtil.renderFullPage(out, request, "Панель администратора", "admin", content);
    }

    private String buildAdminContent(HttpServletRequest request) {
        StringBuilder content = new StringBuilder();

        // Собираем статистику
        long totalUsers = userService.getTotalUsers();
        long totalDrivers = driverService.getAllDrivers().size();
        long totalCars = carService.getAllCars().size();
        long totalOrders = orderService.getAllOrders().size();
        long completedOrders = orderService.getAllOrders().stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .count();

        // === БЛОК 1: СТАТИСТИКА СИСТЕМЫ ===
        content.append("<div class='card'>")
                .append("<h1 class='page-title'> Панель администратора</h1>")
                .append("<p class='page-subtitle'>Общий обзор и управление системой</p>")
                .append("</div>");

        content.append("<div class='card'>")
                .append("<h3 style='color: #fff; margin-bottom: 25px; border-bottom: 1px solid #333; padding-bottom: 10px;'> Статистика системы</h3>")
                .append("<div class='stats-grid'>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalUsers).append("</div>")
                .append("<div class='stat-label'>Пользователей</div>")
                .append("</div>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalDrivers).append("</div>")
                .append("<div class='stat-label'>Водителей</div>")
                .append("</div>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalCars).append("</div>")
                .append("<div class='stat-label'>Автомобилей</div>")
                .append("</div>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalOrders).append("</div>")
                .append("<div class='stat-label'>Всего заказов</div>")
                .append("</div>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(completedOrders).append("</div>")
                .append("<div class='stat-label'>Завершено</div>")
                .append("</div>");

        content.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(completedOrders > 0 ? "Доступен" : "Нет данных").append("</div>")
                .append("<div class='stat-label'>Финансовый отчет</div>")
                .append("</div>");

        content.append("</div>") // Закрываем stats-grid
                .append("</div>");

        // === БЛОК 2: ВЫГРУЗКА ОТЧЕТА ПО ДОХОДАМ (отдельная карточка) ===
        content.append("<div class='card' style='margin-top: 30px; border-left: 4px solid #4caf50;'>")
                .append("<h2 style='color: #fff; margin-bottom: 20px; display: flex; align-items: center; gap: 10px;'>")
                .append("<span style='font-size: 1.5em;'></span> Выгрузка отчета по заказам")
                .append("</h2>")
                .append("<p style='color: #aaa; margin-bottom: 25px;'>Сформируйте отчет по завершенным заказам с фильтрацией по дате и водителю</p>");

        // Форма фильтрации для выгрузки
        content.append("<div class='filter-form' style='background: rgba(255,255,255,0.02);'>")
                .append("<form method='GET' action='").append(request.getContextPath()).append("/admin/income-report/csv' target='_blank'>")
                .append("<div class='form-row'>")
                .append("<div class='form-group'>")
                .append("<label for='startDate'><span style='color: #4caf50;'></span> Начальная дата</label>")
                .append("<input type='date' id='startDate' name='startDate' class='form-control' value='")
                .append(request.getParameter("startDate") != null ? request.getParameter("startDate") : "")
                .append("'>")
                .append("</div>")
                .append("<div class='form-group'>")
                .append("<label for='endDate'><span style='color: #4caf50;'></span> Конечная дата</label>")
                .append("<input type='date' id='endDate' name='endDate' class='form-control' value='")
                .append(request.getParameter("endDate") != null ? request.getParameter("endDate") : "")
                .append("'>")
                .append("</div>")
                .append("<div class='form-group'>")
                .append("<label for='driverId'><span style='color: #2196f3;'></span> Водитель</label>")
                .append("<select id='driverId' name='driverId' class='form-control'>")
                .append("<option value=''>Все водители</option>");

        // Получаем список водителей
        try {
            List<Driver> allDrivers = driverService.getAllDrivers();
            for (Driver driver : allDrivers) {
                String selected = "";
                String driverIdParam = request.getParameter("driverId");
                if (driverIdParam != null && !driverIdParam.isEmpty() &&
                        driver.getId() != null && driver.getId().toString().equals(driverIdParam)) {
                    selected = "selected";
                }
                content.append("<option value='").append(driver.getId()).append("' ").append(selected).append(">")
                        .append(driver.getFullName() != null ? driver.getFullName() : "Водитель #" + driver.getId())
                        .append("</option>");
            }
        } catch (Exception e) {
            content.append("<option value=''>Ошибка загрузки водителей</option>");
        }

        content.append("</select>")
                .append("</div>")
                .append("</div>")
                .append("<div class='btn-group'>")
                .append("<button type='submit' class='btn btn-success' style='background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);'>")
                .append("<span style='font-size: 1.2em;'></span> Скачать отчет (CSV)")
                .append("</button>")
                .append("<button type='button' onclick='resetReportFilters()' class='btn btn-secondary'>")
                .append("<span style='font-size: 1.2em;'></span> Сбросить фильтры")
                .append("</button>")
                .append("</div>")
                .append("<div style='margin-top: 20px; padding: 15px; background: rgba(255,255,255,0.03); border-radius: 8px;'>")
                .append("<p style='color: #888; font-size: 0.9em; margin: 0;'>")
                .append("<span style='color: #4caf50;'></span> <strong>Отчет включает:</strong> Дата, Водитель, Автомобиль, Гос. номер, Клиент, Телефон, Откуда, Куда, Дистанция, Стоимость")
                .append("</p>")
                .append("</div>")
                .append("</form>")
                .append("</div>")
                .append("</div>"); // Закрываем card

        // === БЛОК 3: БЫСТРЫЕ ДЕЙСТВИЯ ===
        content.append("<div class='card'>")
                .append("<h3 style='color: #fff; margin-bottom: 25px; border-bottom: 1px solid #333; padding-bottom: 10px;'> Быстрые действия</h3>")
                .append("<div class='info-grid'>");

        content.append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #2196f3;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Управление пользователями</h4>")
                .append("<small style='color: #666;'>Создание и редактирование учетных записей</small>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; margin-bottom: 20px;'>Добавление новых сотрудников, назначение ролей, блокировка учетных записей</p>")
                .append("<div class='form-actions'>")
                .append("<a href='/admin/users' class='btn btn-sm btn-primary'>Перейти к управлению →</a>")
                .append("</div>")
                .append("</div>");

        content.append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #ff9800;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Управление водителями</h4>")
                .append("<small style='color: #666;'>Учет водителей и их документов</small>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; margin-bottom: 20px;'>Медосмотры, права, назначение автомобилей, рабочие смены</p>")
                .append("<div class='form-actions'>")
                .append("<a href='/drivers' class='btn btn-sm btn-primary'>Перейти к водителям →</a>")
                .append("</div>")
                .append("</div>");

        content.append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #9c27b0;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Управление автопарком</h4>")
                .append("<small style='color: #666;'>Техническое состояние автомобилей</small>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; margin-bottom: 20px;'>Техосмотры, ремонты, назначение водителей, учет пробега</p>")
                .append("<div class='form-actions'>")
                .append("<a href='/cars' class='btn btn-sm btn-primary'>Перейти к автомобилям →</a>")
                .append("</div>")
                .append("</div>");

        content.append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #e91e63;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Управление заказами</h4>")
                .append("<small style='color: #666;'>Создание и отслеживание заказов</small>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; margin-bottom: 20px;'>Новые заказы, назначение водителей, отслеживание статусов, история</p>")
                .append("<div class='form-actions'>")
                .append("<a href='/orders' class='btn btn-sm btn-primary'>Перейти к заказам →</a>")
                .append("</div>")
                .append("</div>");

        content.append("</div>") // Закрываем info-grid
                .append("</div>");

        // JavaScript для сброса фильтров
        content.append("<script>")
                .append("function resetReportFilters() {")
                .append("    document.getElementById('startDate').value = '';")
                .append("    document.getElementById('endDate').value = '';")
                .append("    document.getElementById('driverId').value = '';")
                .append("}")
                .append("</script>");

        return content.toString();
    }
}