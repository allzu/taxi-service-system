package com.taxi.controller;

import com.taxi.entity.Car;
import com.taxi.entity.TechnicalInspection;
import com.taxi.entity.User;
import com.taxi.service.CarService;
import com.taxi.service.TechnicalInspectionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/mechanic")
public class MechanicPanelServlet extends BaseServlet {
    private TechnicalInspectionService inspectionService = new TechnicalInspectionService();
    private CarService carService = new CarService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = getCurrentUser(req);
        if (user == null || !"MECHANIC".equals(user.getUserType())) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        renderHeader(out, "Панель механика", req);
        renderSidebar(out, "mechanic", user.getUserType());

        out.println("        <h1 class='page-title'>Панель механика</h1>");

        // Карточка действий
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Быстрые действия</h2>");
        out.println("            <div style='display: flex; gap: 15px; flex-wrap: wrap;'>");
        out.println("                <a href='/inspections?action=new' class='btn'>Новый техосмотр</a>");
        out.println("                <a href='/cars' class='btn btn-secondary'>Список автомобилей</a>");
        out.println("                <a href='/inspections' class='btn btn-secondary'>История техосмотров</a>");
        out.println("                <a href='/waybills?action=open' class='btn'>Открыть смену</a>");
        out.println("            </div>");
        out.println("        </div>");

        // Автомобили, требующие техосмотр
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Автомобили, требующие техосмотр</h2>");

        List<Car> cars = carService.getAllCars();
        int count = 0;

        out.println("            <table>");
        out.println("                <thead>");
        out.println("                    <tr>");
        out.println("                        <th>Марка</th>");
        out.println("                        <th>Модель</th>");
        out.println("                        <th>Гос. номер</th>");
        out.println("                        <th>Тех. статус</th>");
        out.println("                        <th>Действия</th>");
        out.println("                    </tr>");
        out.println("                </thead>");
        out.println("                <tbody>");

        for (Car car : cars) {
            if (car.getTechnicalStatus() != null &&
                    (car.getTechnicalStatus().name().equals("NEEDS_INSPECTION") ||
                            car.getTechnicalStatus().name().equals("IN_REPAIR"))) {

                count++;
                out.println("                <tr>");
                out.println("                    <td>" + car.getBrand() + "</td>");
                out.println("                    <td>" + car.getModel() + "</td>");
                out.println("                    <td>" + car.getLicensePlate() + "</td>");
                out.println("                    <td>" + car.getTechnicalStatus() + "</td>");
                out.println("                    <td>");
                out.println("                        <a href='/inspections?action=new&carId=" + car.getId() +
                        "' class='btn' style='padding: 5px 10px; font-size: 12px;'>Провести осмотр</a>");
                out.println("                    </td>");
                out.println("                </tr>");
            }
        }

        if (count == 0) {
            out.println("                <tr>");
            out.println("                    <td colspan='5' style='text-align: center; color: #888;'>");
            out.println("                        Все автомобили в исправном состоянии");
            out.println("                    </td>");
            out.println("                </tr>");
        }

        out.println("                </tbody>");
        out.println("            </table>");
        out.println("        </div>");

        // Последние техосмотры
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Последние техосмотры</h2>");

// Получаем все проверки и ограничиваем до 10 вручную
        List<TechnicalInspection> allInspections = inspectionService.getAllTechnicalInspections();
        List<TechnicalInspection> recentInspections = allInspections.stream()
                .limit(10)
                .toList();

        out.println("            <table>");
        out.println("                <thead>");
        out.println("                    <tr>");
        out.println("                        <th>Дата</th>");
        out.println("                        <th>Автомобиль</th>");
        out.println("                        <th>Результат</th>");
        out.println("                        <th>Примечания</th>");
        out.println("                    </tr>");
        out.println("                </thead>");
        out.println("                <tbody>");

        for (TechnicalInspection inspection : recentInspections) {
            out.println("                <tr>");
            out.println("                    <td>" + inspection.getInspectionDate() + "</td>");
            out.println("                    <td>" + (inspection.getCar() != null ?
                    inspection.getCar().getBrand() + " " + inspection.getCar().getModel() +
                            " (" + inspection.getCar().getLicensePlate() + ")" : "Неизвестно") + "</td>");
            out.println("                    <td>" + (inspection.getIsPassed() ? "Пройден" : "Не пройден") + "</td>");
            out.println("                    <td>" + (inspection.getNotes() != null ? inspection.getNotes() : "") + "</td>");
            out.println("                </tr>");
        }

        if (recentInspections.isEmpty()) {
            out.println("                <tr>");
            out.println("                    <td colspan='4' style='text-align: center; color: #888;'>");
            out.println("                        Нет данных о техосмотрах");
            out.println("                    </td>");
            out.println("                </tr>");
        }

        out.println("                </tbody>");
        out.println("            </table>");
        out.println("        </div>");

        // Статистика - ИСПРАВЛЕННАЯ ВЕРСИЯ
        out.println("        <div class='card'>");
        out.println("            <h2 style='color: #fff; margin-bottom: 20px;'>Статистика</h2>");
        out.println("            <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;'>");

        try {
            // ИСПРАВЛЕНО: Получаем списки и берем их размер
            List<Car> allCars = carService.getAllCars();
            List<Car> availableCarsList = carService.getAvailableCars();
            List<Car> carsInRepairList = carService.getCarsInRepair();

            long totalCars = allCars.size();
            long availableCars = availableCarsList.size();
            long inRepairCars = carsInRepairList.size();
            double availabilityRate = totalCars > 0 ? (double) availableCars / totalCars * 100 : 0;

            out.println("                <div style='text-align: center;'>");
            out.println("                    <div style='font-size: 32px; color: #fff;'>" + totalCars + "</div>");
            out.println("                    <div style='color: #888;'>Всего автомобилей</div>");
            out.println("                </div>");

            out.println("                <div style='text-align: center;'>");
            out.println("                    <div style='font-size: 32px; color: #4caf50;'>" + availableCars + "</div>");
            out.println("                    <div style='color: #888;'>Доступно</div>");
            out.println("                </div>");

            out.println("                <div style='text-align: center;'>");
            out.println("                    <div style='font-size: 32px; color: #f44336;'>" + inRepairCars + "</div>");
            out.println("                    <div style='color: #888;'>В ремонте</div>");
            out.println("                </div>");

            out.println("                <div style='text-align: center;'>");
            out.println("                    <div style='font-size: 32px; color: #2196f3;'>" + String.format("%.1f", availabilityRate) + "%</div>");
            out.println("                    <div style='color: #888;'>Доступность</div>");
            out.println("                </div>");

        } catch (Exception e) {
            out.println("                <div style='grid-column: 1 / -1; text-align: center; color: #f44336;'>");
            out.println("                    Статистика временно недоступна");
            out.println("                </div>");
        }

        out.println("            </div>");
        out.println("        </div>");

        renderFooter(out);
    }
}