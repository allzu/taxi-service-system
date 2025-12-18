package com.taxi.controller;

import com.taxi.entity.Driver;
import com.taxi.entity.MedicalCheck;
import com.taxi.entity.User;
import com.taxi.service.DriverService;
import com.taxi.service.MedicalCheckService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/doctor")
public class DoctorPanelServlet extends BaseServlet {
    private MedicalCheckService medicalService = new MedicalCheckService();
    private DriverService driverService = new DriverService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Используем метод checkRole из BaseServlet
        if (!checkRole(req, "DOCTOR", "ADMIN")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // Получаем пользователя
        User user = getCurrentUser(req);

        // Рендерим заголовок и сайдбар
        renderHeader(out, "Панель мед.работника", req);
        renderSidebar(out, "doctor", getUserRole(req));

        out.println("        <h1 class='page-title'>Панель медицинского работника</h1>");

        // Карточка действий
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Быстрые действия</h2>");
        out.println("            <div style='display: flex; gap: 15px; flex-wrap: wrap;'>");
        out.println("                <a href='/medical-checks?action=new' class='btn'>Новый медосмотр</a>");
        out.println("                <a href='/drivers' class='btn btn-secondary'>Список водителей</a>");
        out.println("                <a href='/medical-checks' class='btn btn-secondary'>История медосмотров</a>");
        out.println("            </div>");
        out.println("        </div>");

        // Список водителей, требующих медосмотр
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Водители, требующие медосмотр</h2>");

        List<Driver> drivers = driverService.getAllDrivers();
        int count = 0;

        out.println("            <table>");
        out.println("                <thead>");
        out.println("                    <tr>");
        out.println("                        <th>ФИО</th>");
        out.println("                        <th>Номер прав</th>");
        out.println("                        <th>Телефон</th>");
        out.println("                        <th>Статус</th>");
        out.println("                        <th>Действия</th>");
        out.println("                    </tr>");
        out.println("                </thead>");
        out.println("                <tbody>");

        for (Driver driver : drivers) {
            if (driver.getMedicalStatus() != null &&
                    (driver.getMedicalStatus().name().equals("PENDING") ||
                            driver.getMedicalStatus().name().equals("EXPIRED"))) {

                count++;
                out.println("                <tr>");
                out.println("                    <td>" + driver.getFullName() + "</td>");
                out.println("                    <td>" + driver.getLicenseNumber() + "</td>");
                out.println("                    <td>" + driver.getPhone() + "</td>");
                out.println("                    <td>" + driver.getMedicalStatus() + "</td>");
                out.println("                    <td>");
                out.println("                        <a href='/medical-checks?action=new&driverId=" + driver.getId() +
                        "' class='btn' style='padding: 5px 10px; font-size: 12px;'>Провести осмотр</a>");
                out.println("                    </td>");
                out.println("                </tr>");
            }
        }

        if (count == 0) {
            out.println("                <tr>");
            out.println("                    <td colspan='5' style='text-align: center; color: #888;'>");
            out.println("                        Все водители имеют действующие медосмотры");
            out.println("                    </td>");
            out.println("                </tr>");
        }

        out.println("                </tbody>");
        out.println("            </table>");
        out.println("        </div>");

        // Последние медосмотры
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Последние медосмотры</h2>");

        List<MedicalCheck> recentChecks = medicalService.getRecentMedicalChecks(10);

        out.println("            <table>");
        out.println("                <thead>");
        out.println("                    <tr>");
        out.println("                        <th>Дата</th>");
        out.println("                        <th>Водитель</th>");
        out.println("                        <th>Результат</th>");
        out.println("                        <th>Примечания</th>");
        out.println("                    </tr>");
        out.println("                </thead>");
        out.println("                <tbody>");

        for (MedicalCheck check : recentChecks) {
            out.println("                <tr>");
            out.println("                    <td>" + check.getCheckDate() + "</td>");
            out.println("                    <td>" + (check.getDriver() != null ? check.getDriver().getFullName() : "Неизвестно") + "</td>");
            out.println("                    <td>" + (check.getIsPassed() ? "Пройден" : "Не пройден") + "</td>");
            out.println("                    <td>" + (check.getNotes() != null ? check.getNotes() : "") + "</td>");
            out.println("                </tr>");
        }

        if (recentChecks.isEmpty()) {
            out.println("                <tr>");
            out.println("                    <td colspan='4' style='text-align: center; color: #888;'>");
            out.println("                        Нет данных о медосмотрах");
            out.println("                    </td>");
            out.println("                </tr>");
        }

        out.println("                </tbody>");
        out.println("            </table>");
        out.println("        </div>");

        // Статистика
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Статистика</h2>");
        out.println("            <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;'>");

        long totalChecks = medicalService.getTotalMedicalChecks();
        long passedChecks = medicalService.getPassedMedicalChecks();
        long failedChecks = medicalService.getFailedMedicalChecks();
        double passRate = totalChecks > 0 ? (double) passedChecks / totalChecks * 100 : 0;

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #fff;'>" + totalChecks + "</div>");
        out.println("                    <div style='color: #888;'>Всего медосмотров</div>");
        out.println("                </div>");

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #4caf50;'>" + passedChecks + "</div>");
        out.println("                    <div style='color: #888;'>Пройдено</div>");
        out.println("                </div>");

        out.println("                <div style='text-align: center;'>");
        out.println("                    <div style='font-size: 32px; color: #f44336;'>" + failedChecks + "</div>");
        out.println("                    <div style='color: #888;'>Не пройдено</div>");
        out.println("                </div>");

        out.println("            </div>");
        out.println("        </div>");

        renderFooter(out);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");

        if ("createCheck".equals(action)) {
            // Создание нового медосмотра
            Long driverId = Long.parseLong(req.getParameter("driverId"));
            boolean isPassed = "true".equals(req.getParameter("isPassed"));
            String notes = req.getParameter("notes");

            User doctor = getCurrentUser(req);

            MedicalCheck check = medicalService.createMedicalCheck(
                    driverId,
                    doctor.getId(),
                    isPassed,
                    notes
            );

            resp.sendRedirect(req.getContextPath() + "/doctor?success=1");
        }
    }
}