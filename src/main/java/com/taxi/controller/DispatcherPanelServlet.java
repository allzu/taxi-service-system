package com.taxi.controller;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.entity.Car;
import com.taxi.entity.User;
import com.taxi.service.OrderService;
import com.taxi.service.DriverService;
import com.taxi.service.CarService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/dispatcher")
public class DispatcherPanelServlet extends BaseServlet {
    private OrderService orderService = new OrderService();
    private DriverService driverService = new DriverService();
    private CarService carService = new CarService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = getCurrentUser(req);
        if (user == null || !"OPERATOR".equals(user.getUserType())) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        renderHeader(out, "Панель диспетчера", req);
        renderSidebar(out, "dispatcher", user.getUserType());

        out.println("        <h1 class='page-title'>Панель диспетчера</h1>");

        // Карточка действий - только одна кнопка
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Быстрые действия</h2>");
        out.println("            <div style='display: flex; gap: 15px; flex-wrap: wrap;'>");
        out.println("                <a href='/orders?action=new' class='btn'>Новый заказ</a>");
        out.println("            </div>");
        out.println("        </div>");

        // Активные заказы (не завершенные и не отмененные)
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Активные заказы</h2>");

        // Получение активных заказов
        List<Order> activeOrders = orderService.getDispatcherActiveOrders();



        out.println("            <table>");
        out.println("                <thead>");
        out.println("                    <tr>");
        out.println("                        <th>ID</th>");
        out.println("                        <th>Клиент</th>");
        out.println("                        <th>Адрес подачи</th>");
        out.println("                        <th>Водитель</th>");
        out.println("                        <th>Статус</th>");
        out.println("                        <th>Действия</th>");
        out.println("                    </tr>");
        out.println("                </thead>");
        out.println("                <tbody>");

        if (activeOrders.isEmpty()) {
            out.println("                <tr>");
            out.println("                    <td colspan='6' style='text-align: center; color: #888;'>");
            out.println("                        Нет активных заказов");
            out.println("                    </td>");
            out.println("                </tr>");
        } else {
            for (Order order : activeOrders) {
                String status = order.getStatus();
                out.println("                <tr>");
                out.println("                    <td>" + order.getId() + "</td>");
                out.println("                    <td>" + (order.getCustomerName() != null ? order.getCustomerName() : "Не указано") + "</td>");
                out.println("                    <td>" + (order.getPickupAddress() != null ? order.getPickupAddress() : "Не указано") + "</td>");
                out.println("                    <td>" + (order.getDriver() != null ? order.getDriver().getFullName() : "Не назначен") + "</td>");
                out.println("                    <td>");

                // Отображение статуса
                String statusClass = "";
                String statusText = "";
                if (status != null) {
                    switch (status.toUpperCase()) {
                        case "NEW":
                            statusClass = "status-new";
                            statusText = "Новый";
                            break;
                        case "ASSIGNED":
                        case "ACCEPTED":
                            statusClass = "status-accepted";
                            statusText = "Назначен";
                            break;
                        case "IN_PROGRESS":
                            statusClass = "status-in-progress";
                            statusText = "В пути";
                            break;
                        case "ON_THE_WAY":
                            statusClass = "status-on-way";
                            statusText = "Едет к клиенту";
                            break;
                        case "PENDING":
                            statusClass = "status-pending";
                            statusText = "Ожидание";
                            break;
                        default:
                            statusClass = "status-default";
                            statusText = status;
                    }
                }
                out.println("                        <span class='status-badge " + statusClass + "'>" + statusText + "</span>");

                out.println("                    </td>");
                out.println("                    <td>");
                out.println("                        <a href='/orders?action=edit&id=" + order.getId() +
                        "' class='btn' style='padding: 5px 10px; font-size: 12px; margin-right: 5px;'>Изменить</a>");
                out.println("                        <a href='/orders?action=complete&id=" + order.getId() +
                        "' class='btn btn-secondary' style='padding: 5px 10px; font-size: 12px; margin-right: 5px;'>Завершить</a>");
                out.println("                        <a href='/orders?action=cancel&id=" + order.getId() +
                        "' class='btn btn-danger' style='padding: 5px 10px; font-size: 12px;'>Отменить</a>");
                out.println("                    </td>");
                out.println("                </tr>");
            }
        }

        out.println("                </tbody>");
        out.println("            </table>");
        out.println("        </div>");

        // Доступные водители и автомобили
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Доступные ресурсы</h2>");
        out.println("            <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 20px;'>");

        // Доступные водители
        out.println("                <div>");
        out.println("                    <h3 style='color: #aaa; margin-bottom: 10px;'>Доступные водители</h3>");
        List<Driver> availableDrivers = driverService.getAvailableDrivers();

        if (availableDrivers.isEmpty()) {
            out.println("                    <p style='color: #888;'>Нет доступных водителей</p>");
        } else {
            out.println("                    <ul style='color: #fff; padding-left: 20px;'>");
            for (Driver driver : availableDrivers) {
                out.println("                        <li>" + driver.getFullName() +
                        " (" + driver.getLicenseNumber() + ")</li>");
            }
            out.println("                    </ul>");
        }
        out.println("                </div>");

        // Доступные автомобили
        out.println("                <div>");
        out.println("                    <h3 style='color: #aaa; margin-bottom: 10px;'>Доступные автомобили</h3>");
        List<Car> availableCars = carService.getAvailableCars();

        if (availableCars.isEmpty()) {
            out.println("                    <p style='color: #888;'>Нет доступных автомобилей</p>");
        } else {
            out.println("                    <ul style='color: #fff; padding-left: 20px;'>");
            for (Car car : availableCars) {
                out.println("                        <li>" + car.getBrand() + " " + car.getModel() +
                        " (" + car.getLicensePlate() + ")</li>");
            }
            out.println("                    </ul>");
        }
        out.println("                </div>");

        out.println("            </div>");
        out.println("        </div>");

        // Статистика
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Статистика за сегодня</h2>");
        out.println("            <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;'>");

        long todayOrders = orderService.getTodayOrdersCount();
        long completedToday = orderService.getCompletedTodayCount();
        double totalRevenue = orderService.getTodayRevenue();

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #fff;'>" + todayOrders + "</div>");
        out.println("                    <div style='color: #888;'>Заказов сегодня</div>");
        out.println("                </div>");

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #4caf50;'>" + completedToday + "</div>");
        out.println("                    <div style='color: #888;'>Выполнено</div>");
        out.println("                </div>");

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #2196f3;'>" + totalRevenue + " руб.</div>");
        out.println("                    <div style='color: #888;'>Выручка</div>");
        out.println("                </div>");

        out.println("            </div>");
        out.println("        </div>");

        // Добавляем CSS для статусов
        out.println("<style>");
        out.println(".status-badge {");
        out.println("    display: inline-block;");
        out.println("    padding: 4px 12px;");
        out.println("    border-radius: 15px;");
        out.println("    font-size: 12px;");
        out.println("    font-weight: 500;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.5px;");
        out.println("}");
        out.println(".status-new { background-color: #ff9800; color: #000; }");
        out.println(".status-accepted { background-color: #2196f3; color: #fff; }");
        out.println(".status-in-progress { background-color: #4caf50; color: #fff; }");
        out.println(".status-on-way { background-color: #9c27b0; color: #fff; }");
        out.println(".status-pending { background-color: #ff5722; color: #fff; }");
        out.println(".status-default { background-color: #757575; color: #fff; }");
        out.println(".btn-danger {");
        out.println("    background-color: #f44336;");
        out.println("    border-color: #f44336;");
        out.println("}");
        out.println(".btn-danger:hover {");
        out.println("    background-color: #d32f2f;");
        out.println("    border-color: #d32f2f;");
        out.println("}");
        out.println("</style>");

        renderFooter(out);
    }
}