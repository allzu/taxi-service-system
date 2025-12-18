package com.taxi.controller;

import com.taxi.service.ReportService;
import com.taxi.entity.User;
import com.taxi.entity.Order;
import com.taxi.repository.OrderRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.LocalDate;
import java.util.List;

public class IncomeReportServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();
    private final OrderRepository orderRepository = new OrderRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"ADMIN".equals(user.getUserType())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
            return;
        }

        // Получаем параметры фильтра
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String driverIdStr = request.getParameter("driverId");

        LocalDate startDate = null;
        LocalDate endDate = null;
        Long driverId = null;

        try {
            if (startDateStr != null && !startDateStr.isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
            if (driverIdStr != null && !driverIdStr.isEmpty()) {
                driverId = Long.parseLong(driverIdStr);
            }
        } catch (Exception e) {
            // Оставляем null
        }

        // Получаем данные для CSV
        List<ReportService.IncomeReportRecord> records = reportService.getIncomeReportRecords(
                startDate, endDate, driverId, null
        );

        // Настраиваем response для скачивания CSV
        String filename = "отчет_по_заказам_" + LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter out = response.getWriter();

        // Записываем BOM для правильного отображения кириллицы в Excel
        out.write('\uFEFF');

        // Записываем заголовок с нужными столбцами
        out.println("Дата завершения заказа;Водитель;Автомобиль;Гос. номер;Клиент;Телефон клиента;Откуда;Куда;Дистанция (км);Стоимость (руб)");

        // Записываем данные с разделителем точка с запятой
        for (ReportService.IncomeReportRecord record : records) {
            out.println(formatCsvLine(record));
        }
    }

    private String formatCsvLine(ReportService.IncomeReportRecord record) {
        StringBuilder line = new StringBuilder();

        // 1. Дата завершения заказа
        String dateStr = "";
        try {
            if (record.getDate() != null) {
                dateStr = record.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
        } catch (Exception e) {
            dateStr = "";
        }
        line.append(dateStr).append(";");

        // 2. Водитель (без кавычек, так как используем ; как разделитель)
        line.append(safeValue(record.getDriverName(), "Не назначен")).append(";");

        // 3. Автомобиль (модель)
        line.append(safeValue(record.getCarModel(), "—")).append(";");

        // 4. Гос. номер
        line.append(safeValue(record.getCarLicense(), "—")).append(";");

        // 5. Клиент
        String customerName = getCustomerNameFromOrder(record.getOrderId());
        line.append(safeValue(customerName, "—")).append(";");

        // 6. Телефон клиента
        String customerPhone = getCustomerPhoneFromOrder(record.getOrderId());
        line.append(safeValue(customerPhone, "—")).append(";");

        // 7. Откуда
        String pickupAddress = getPickupAddressFromOrder(record.getOrderId());
        line.append(safeValue(pickupAddress, "—")).append(";");

        // 8. Куда
        String destinationAddress = getDestinationAddressFromOrder(record.getOrderId());
        line.append(safeValue(destinationAddress, "—")).append(";");

        // 9. Дистанция (км) - с запятой как десятичный разделитель
        line.append(String.format("%.2f", record.getDistance()).replace('.', ',')).append(";");

        // 10. Стоимость (руб) - с запятой как десятичный разделитель
        line.append(String.format("%.2f", record.getRevenue()).replace('.', ','));

        return line.toString();
    }

    private String safeValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        // Заменяем точку с запятой на запятую, чтобы не ломать CSV
        return value.replace(";", ",").replace("\"", "'").trim();
    }

    private String getCustomerNameFromOrder(Long orderId) {
        if (orderId == null) return null;
        try {
            Order order = orderRepository.findById(orderId);
            if (order != null) {
                return order.getCustomerName();
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return null;
    }

    private String getCustomerPhoneFromOrder(Long orderId) {
        if (orderId == null) return null;
        try {
            Order order = orderRepository.findById(orderId);
            if (order != null) {
                return order.getCustomerPhone();
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return null;
    }

    private String getPickupAddressFromOrder(Long orderId) {
        if (orderId == null) return null;
        try {
            Order order = orderRepository.findById(orderId);
            if (order != null) {
                return order.getPickupAddress();
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return null;
    }

    private String getDestinationAddressFromOrder(Long orderId) {
        if (orderId == null) return null;
        try {
            Order order = orderRepository.findById(orderId);
            if (order != null) {
                return order.getDestinationAddress();
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return null;
    }
}