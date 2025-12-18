package com.taxi.controller;

import com.taxi.entity.Driver;
import com.taxi.entity.Order;
import com.taxi.entity.Waybill;
import com.taxi.entity.User;
import com.taxi.service.DriverService;
import com.taxi.service.OrderService;
import com.taxi.service.WaybillService;
import com.taxi.repository.WaybillRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/driver-panel")
public class DriverPanelServlet extends BaseServlet {
    private DriverService driverService = new DriverService();
    private OrderService orderService = new OrderService();
    private WaybillService waybillService = new WaybillService();
    private WaybillRepository waybillRepository = new WaybillRepository(); // ДОБАВИТЬ ЭТО

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!checkRole(req, "DRIVER")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = getCurrentUser(req);
        Driver driver = driverService.findByUserId(user.getId());

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        renderHeader(out, "Моя панель", req);
        renderSidebar(out, "driver-panel", getUserRole(req));

        out.println("        <h1 class='page-title'>Моя панель водителя</h1>");

        // Информация о водителе
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Моя информация</h2>");
        out.println("            <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 20px;'>");
        out.println("                <div>");
        out.println("                    <p><strong>ФИО:</strong> " + user.getFullName() + "</p>");
        if (driver != null) {
            out.println("                    <p><strong>Номер прав:</strong> " + driver.getLicenseNumber() + "</p>");
            out.println("                    <p><strong>Телефон:</strong> " + driver.getPhone() + "</p>");
            out.println("                    <p><strong>Мед. статус:</strong> " +
                    (driver.getMedicalStatus() != null ? driver.getMedicalStatus() : "Не указан") + "</p>");
        }
        out.println("                </div>");
        out.println("                <div>");
        out.println("                    <h3 style='color: #aaa; margin-bottom: 10px;'>Быстрые действия</h3>");
        out.println("                    <div style='display: flex; flex-direction: column; gap: 10px;'>");
        out.println("                        <a href='/orders?driver=true' class='btn'>Мои заказы</a>");
        out.println("                        <a href='/waybills?driver=true' class='btn btn-secondary'>Мои смены</a>");
        out.println("                        <a href='/medical-checks?driver=true' class='btn'>Мои медосмотры</a>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("            </div>");
        out.println("        </div>");

        // Текущая смена
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Текущая смена</h2>");

        if (driver != null) {
            // Прямой вызов через репозиторий
            Waybill activeWaybill = waybillRepository.findActiveByDriverId(driver.getId());

            if (activeWaybill != null) {
                out.println("            <div style='padding: 20px; background: rgba(255,255,255,0.05); border-radius: 10px;'>");
                out.println("                <p><strong>Начало:</strong> " + activeWaybill.getStartTime() + "</p>");
                out.println("                <p><strong>Автомобиль:</strong> " +
                        (activeWaybill.getCar() != null ?
                                activeWaybill.getCar().getBrand() + " " + activeWaybill.getCar().getModel() +
                                        " (" + activeWaybill.getCar().getLicensePlate() + ")" : "Не назначен") + "</p>");
                out.println("                <p><strong>Статус:</strong> <span style='color: #4caf50;'>Активна</span></p>");
                out.println("                <div style='margin-top: 15px;'>");
                out.println("                    <a href='/waybills?action=close&id=" + activeWaybill.getId() +
                        "' class='btn' style='padding: 10px 20px;'>Закончить смену</a>");
                out.println("                </div>");
                out.println("            </div>");
            } else {
                out.println("            <p style='color: #888; text-align: center;'>Смена не начата</p>");
            }
        }
        out.println("        </div>");

        // Активные заказы
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Мои заказы</h2>");

        if (driver != null) {
            try {
                List<Order> driverOrders = orderService.getDriverOrders(driver.getId());
                if (driverOrders != null && !driverOrders.isEmpty()) {
                    out.println("            <p>Всего заказов: " + driverOrders.size() + "</p>");
                    out.println("            <ul>");
                    for (Order order : driverOrders) {
                        out.println("                <li>Заказ #" + order.getId() + " - " +
                                (order.getStatus() != null ? order.getStatus() : "Нет статуса") + "</li>");
                    }
                    out.println("            </ul>");
                } else {
                    out.println("            <p style='color: #888; text-align: center;'>Нет заказов</p>");
                }
            } catch (Exception e) {
                out.println("            <p style='color: #f44336;'>Не удалось загрузить заказы</p>");
            }
        }
        out.println("        </div>");

        // Статистика
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Моя статистика</h2>");
        out.println("            <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;'>");

        if (driver != null) {
            try {
                long totalOrders = 0;
                long completedOrders = 0;
                long totalWaybills = 0;
                long activeOrders = 0;

                // Получаем статистику
                List<Order> orders = orderService.getDriverOrders(driver.getId());
                if (orders != null) {
                    totalOrders = orders.size();
                    completedOrders = orders.stream()
                            .filter(o -> "COMPLETED".equals(o.getStatus()))
                            .count();
                    activeOrders = orders.stream()
                            .filter(o -> "IN_PROGRESS".equals(o.getStatus()) || "ASSIGNED".equals(o.getStatus()))
                            .count();
                }

                List<Waybill> waybills = waybillRepository.findByDriverId(driver.getId());
                if (waybills != null) {
                    totalWaybills = waybills.size();
                }

                out.println("                <div style='text-align: center;'>");
                out.println("                    <div style='font-size: 32px; color: #fff;'>" + totalOrders + "</div>");
                out.println("                    <div style='color: #888;'>Всего заказов</div>");
                out.println("                </div>");

                out.println("                <div style='text-align: center;'>");
                out.println("                    <div style='font-size: 32px; color: #4caf50;'>" + completedOrders + "</div>");
                out.println("                    <div style='color: #888;'>Выполнено</div>");
                out.println("                </div>");

                out.println("                <div style='text-align: center;'>");
                out.println("                    <div style='font-size: 32px; color: #2196f3;'>" + totalWaybills + "</div>");
                out.println("                    <div style='color: #888;'>Смен</div>");
                out.println("                </div>");

                out.println("                <div style='text-align: center;'>");
                out.println("                    <div style='font-size: 32px; color: #ff9800;'>" + activeOrders + "</div>");
                out.println("                    <div style='color: #888;'>Активных</div>");
                out.println("                </div>");
            } catch (Exception e) {
                out.println("                <div style='grid-column: 1 / -1; text-align: center; color: #f44336;'>");
                out.println("                    Статистика временно недоступна");
                out.println("                </div>");
            }
        }

        out.println("            </div>");
        out.println("        </div>");

        renderFooter(out);
    }
}