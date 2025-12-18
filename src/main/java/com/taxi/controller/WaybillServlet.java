package com.taxi.controller;

import com.taxi.entity.*;
import com.taxi.repository.*;
import com.taxi.service.*;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/waybills/*")
public class WaybillServlet extends HttpServlet {

    private WaybillService waybillService = new WaybillService();
    private DriverRepository driverRepository = new DriverRepository();
    private CarRepository carRepository = new CarRepository();
    private UserRepository userRepository = new UserRepository();
    private MedicalCheckService medicalCheckService = new MedicalCheckService();
    private TechnicalInspectionService inspectionService = new TechnicalInspectionService();
    private OrderService orderService = new OrderService();

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ПРОВЕРКА АВТОРИЗАЦИИ
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();
        String path = request.getPathInfo();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Проверка доступа для DOCTOR и OPERATOR
            if ("DOCTOR".equals(userRole) || "OPERATOR".equals(userRole)) {
                HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                return;
            }

            // DRIVER может видеть только свои путевые листы
            if ("DRIVER".equals(userRole)) {
                handleDriverAccess(request, response, currentUser);
                return;
            }

            // ADMIN и MECHANIC - полный доступ
            if (!"ADMIN".equals(userRole) && !"MECHANIC".equals(userRole)) {
                HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                return;
            }

            // Основной код для ADMIN и MECHANIC
            if (path == null || path.equals("/") || path.isEmpty()) {
                showWaybillsList(request, out, userRole);
            } else if (path.equals("/new")) {
                showCreateForm(out, request);
            } else if (path.equals("/view")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long waybillId = Long.parseLong(idParam);
                    showWaybillDetails(waybillId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }
            } else if (path.equals("/edit")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long waybillId = Long.parseLong(idParam);
                    showEditForm(waybillId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }
            } else if (path.equals("/orders")) {
                String waybillIdParam = request.getParameter("waybillId");
                if (waybillIdParam != null) {
                    Long waybillId = Long.parseLong(waybillIdParam);
                    showWaybillOrders(waybillId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }
            } else if (path.equals("/close")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long waybillId = Long.parseLong(idParam);
                    showCloseForm(waybillId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }

            } else if (path.equals("/confirm-delete")) {
                // Страница подтверждения удаления
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long waybillId = Long.parseLong(idParam);
                    showDeleteConfirmation(waybillId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }

            } else {
                HtmlUtil.renderErrorPage(out, request, "Страница не найдена", "Запрошенная страница не существует");
            }
        } catch (NumberFormatException e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка формата", "Неверный формат ID");
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", e.getMessage());
            e.printStackTrace();
        }
    }

    //  СПЕЦИАЛЬНЫЕ МЕТОДЫ ДЛЯ ВОДИТЕЛЯ

    private void handleDriverAccess(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, ServletException {
        String path = request.getPathInfo();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Получаем водителя по текущему пользователю
        Driver driver = driverRepository.findByUserId(currentUser.getId());
        if (driver == null) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", "Профиль водителя не найден");
            return;
        }

        try {
            if (path == null || path.equals("/") || path.isEmpty()) {
                // Показываем только путевые листы этого водителя
                showDriverWaybills(request, out, driver);
            } else if (path.equals("/view")) {
                // Просмотр конкретного путевого листа
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long waybillId = Long.parseLong(idParam);
                    checkDriverWaybillAccess(waybillId, driver, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }
            } else if (path.equals("/orders")) {
                // Просмотр заказов в путевом листе
                String waybillIdParam = request.getParameter("waybillId");
                if (waybillIdParam != null) {
                    Long waybillId = Long.parseLong(waybillIdParam);
                    if (checkDriverWaybillOwnership(waybillId, driver)) {
                        showWaybillOrders(waybillId, out, request);
                    } else {
                        HtmlUtil.renderAccessDeniedPage(out, request, "DRIVER");
                    }
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID путевого листа");
                }
            } else {
                // Все остальные пути недоступны для водителя
                HtmlUtil.renderAccessDeniedPage(out, request, "DRIVER");
            }
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", e.getMessage());
        }
    }

    private void showDriverWaybills(HttpServletRequest request, PrintWriter out, Driver driver) {
        List<Waybill> driverWaybills = waybillService.getWaybillsByDriver(driver.getId());

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>")
                .append("<h1 class='page-title'> Мои путевые листы</h1>")
                .append("<p class='page-subtitle'>История ваших смен</p>")
                .append("</div>");

        // Информация о водителе
        content.append("<div class='card mb-20'>")
                .append("<div class='info-grid'>")
                .append("<div><strong> Водитель:</strong><br>").append(driver.getFullName()).append("</div>")
                .append("<div><strong> Автомобиль:</strong><br>")
                .append(driver.getCurrentCar() != null ?
                        driver.getCurrentCar().getLicensePlate() + " (" + driver.getCurrentCar().getModel() + ")" :
                        "Не назначен")
                .append("</div>")
                .append("</div>")
                .append("</div>");

        // Активная смена
        List<Waybill> activeWaybills = driverWaybills.stream()
                .filter(w -> w.getStatus() == Waybill.WaybillStatus.ACTIVE)
                .collect(Collectors.toList());

        if (!activeWaybills.isEmpty()) {
            Waybill activeWaybill = activeWaybills.get(0);
            content.append("<div class='card mb-20'>")
                    .append("<div class='card-header'>")
                    .append("<h3> Активная смена</h3>")
                    .append("</div>")
                    .append("<div class='info-grid'>")
                    .append("<div>")
                    .append("<p><strong>ID:</strong> #").append(activeWaybill.getId()).append("</p>")
                    .append("<p><strong>Начало:</strong> ").append(dateFormatter.format(activeWaybill.getStartTime())).append("</p>")
                    .append("</div>")
                    .append("<div>")
                    .append("<p><strong>Пробег:</strong> ").append(activeWaybill.getInitialMileageKm() != null ?
                            activeWaybill.getInitialMileageKm() + " км" : "-").append("</p>")
                    .append("<p><strong>Статус:</strong> <span class='badge badge-success'>Активна</span></p>")
                    .append("</div>")
                    .append("</div>")
                    .append("<div class='form-actions'>")
                    .append("<a href='/orders?my=true' class='btn btn-primary'> Мои заказы</a>")
                    .append("<a href='/driver-panel' class='btn btn-secondary'> Панель управления</a>")
                    .append("</div>")
                    .append("</div>");
        }

        // История смен
        if (driverWaybills.isEmpty()) {
            content.append("<div class='card text-center'>")
                    .append("<div class='empty-state'>")
                    .append("<div class='empty-icon'></div>")
                    .append("<h3>Нет путевых листов</h3>")
                    .append("<p>У вас еще нет завершенных смен</p>")
                    .append("</div>")
                    .append("</div>");
        } else {
            content.append("<div class='card'>")
                    .append("<h3> История смен</h3>")
                    .append("<div class='table-container'>")
                    .append("<table>")
                    .append("<thead>")
                    .append("<tr>")
                    .append("<th>ID</th>")
                    .append("<th>Дата</th>")
                    .append("<th>Автомобиль</th>")
                    .append("<th>Пробег</th>")
                    .append("<th>Заработок</th>")
                    .append("<th>Статус</th>")
                    .append("<th>Действия</th>")
                    .append("</tr>")
                    .append("</thead>")
                    .append("<tbody>");

            for (Waybill waybill : driverWaybills) {
                String statusClass = getStatusClass(waybill.getStatus());
                String statusText = getStatusText(waybill.getStatus());

                String carInfo = waybill.getCar() != null ?
                        waybill.getCar().getLicensePlate() + "<br><small>" + waybill.getCar().getModel() + "</small>" :
                        "-";

                content.append("<tr>")
                        .append("<td>#").append(waybill.getId()).append("</td>")
                        .append("<td>").append(dateFormatter.format(waybill.getStartTime())).append("</td>")
                        .append("<td>").append(carInfo).append("</td>")
                        .append("<td>").append(waybill.getShiftMileage() > 0 ?
                                waybill.getShiftMileage() + " км" : "-").append("</td>")
                        .append("<td>").append(waybill.getTotalEarnings() != null ?
                                String.format("%.2f ₽", waybill.getTotalEarnings()) : "-").append("</td>")
                        .append("<td><span class='badge ").append(statusClass).append("'>").append(statusText).append("</span></td>")
                        .append("<td>")
                        .append("<div class='action-buttons-small'>")
                        .append("<a href='/waybills/view?id=").append(waybill.getId())
                        .append("' class='btn btn-sm btn-primary'> Просмотр</a>")
                        .append("</div>")
                        .append("</td>")
                        .append("</tr>");
            }

            content.append("</tbody>")
                    .append("</table>")
                    .append("</div>")
                    .append("</div>");

            // Статистика для водителя
            content.append(showDriverStatistics(driverWaybills));
        }

        HtmlUtil.renderFullPage(out, request, "Мои путевые листы", "driver-waybills", content.toString());
    }

    private boolean checkDriverWaybillOwnership(Long waybillId, Driver driver) {
        Waybill waybill = waybillService.getWaybillById(waybillId);
        return waybill != null && waybill.getDriver() != null &&
                waybill.getDriver().getId().equals(driver.getId());
    }

    private void checkDriverWaybillAccess(Long waybillId, Driver driver, PrintWriter out, HttpServletRequest request) {
        if (checkDriverWaybillOwnership(waybillId, driver)) {
            showWaybillDetails(waybillId, out, request);
        } else {
            HtmlUtil.renderAccessDeniedPage(out, request, "DRIVER");
        }
    }

    // ОСНОВНЫЕ МЕТОДЫ (ADMIN/MECHANIC)

    private void showWaybillsList(HttpServletRequest request, PrintWriter out, String userRole) {
        // Получаем параметры фильтрации
        String driverFilter = request.getParameter("driver");
        String carFilter = request.getParameter("car");
        String statusFilter = request.getParameter("status");
        String dateFilter = request.getParameter("date");
        String search = request.getParameter("search");

        // Получаем отфильтрованный список путевых листов
        List<Waybill> waybills = getFilteredWaybills(driverFilter, carFilter, statusFilter, dateFilter, search);

        // Получаем данные для фильтров
        List<Driver> allDrivers = driverRepository.findAll();
        List<Car> allCars = carRepository.findAll();

        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Показываем сообщения об успехе/ошибке
        String success = request.getParameter("success");
        String error = request.getParameter("error");

        if (success != null) {
            String message = switch (success) {
                case "opened" -> " Смена успешно открыта!";
                case "closed" -> " Смена успешно завершена!";
                case "deleted" -> "️ Путевой лист успешно удален!";
                default -> "";
            };
            if (!message.isEmpty()) {
                content.append("<div class='card fade-in'>")
                        .append("<div class='alert alert-success'>").append(message).append("</div>")
                        .append("</div>");
            }
        }

        if (error != null) {
            content.append("<div class='card fade-in'>")
                    .append("<div class='alert alert-danger'> Ошибка: ").append(error).append("</div>")
                    .append("</div>");
        }

        // Заголовок страницы
        content.append("<div class='card mb-20'>")
                .append("<h1 class='page-title'> Путевые листы</h1>")
                .append("<p class='page-subtitle'>Управление сменами водителей</p>")
                .append("</div>");

        // Кнопка добавления (только для механиков и админов)
        content.append("<div class='card mb-20'>")
                .append("<div class='action-buttons'>")
                .append("<a href='/waybills/new' class='btn btn-success'> Открыть новую смену</a>")
                .append("</div>")
                .append("</div>");

        // Блок фильтров
        content.append("<div class='card mb-20'>")
                .append("<h3> Фильтры</h3>")
                .append("<form method='get' action='/waybills' class='form-horizontal'>")
                .append("<div class='info-grid'>");

        // Поиск
        content.append("<div class='form-group'>")
                .append("<label for='search' class='form-label'>Поиск</label>")
                .append("<input type='text' class='form-control' id='search' name='search' ")
                .append("placeholder='ФИО водителя, номер авто...' value='").append(search != null ? search : "").append("'>")
                .append("</div>");

        // Фильтр по водителю
        content.append("<div class='form-group'>")
                .append("<label for='driver' class='form-label'>Водитель</label>")
                .append("<select class='form-control' id='driver' name='driver'>")
                .append("<option value=''>Все водители</option>");

        for (Driver driver : allDrivers) {
            boolean selected = driverFilter != null && driverFilter.equals(driver.getId().toString());
            content.append("<option value='").append(driver.getId()).append("' ")
                    .append(selected ? "selected" : "").append(">")
                    .append(driver.getFullName()).append(" (").append(driver.getLicenseNumber()).append(")</option>");
        }

        content.append("</select>")
                .append("</div>");

        // Фильтр по автомобилю
        content.append("<div class='form-group'>")
                .append("<label for='car' class='form-label'>Автомобиль</label>")
                .append("<select class='form-control' id='car' name='car'>")
                .append("<option value=''>Все автомобили</option>");

        for (Car car : allCars) {
            boolean selected = carFilter != null && carFilter.equals(car.getId().toString());
            content.append("<option value='").append(car.getId()).append("' ")
                    .append(selected ? "selected" : "").append(">")
                    .append(car.getLicensePlate()).append(" (").append(car.getModel()).append(")</option>");
        }

        content.append("</select>")
                .append("</div>");

        // Фильтр по статусу
        content.append("<div class='form-group'>")
                .append("<label for='status' class='form-label'>Статус</label>")
                .append("<select class='form-control' id='status' name='status'>")
                .append("<option value=''>Все статусы</option>")
                .append("<option value='active' ").append("active".equals(statusFilter) ? "selected" : "").append(">Активные</option>")
                .append("<option value='completed' ").append("completed".equals(statusFilter) ? "selected" : "").append(">Завершенные</option>")
                .append("<option value='cancelled' ").append("cancelled".equals(statusFilter) ? "selected" : "").append(">Отмененные</option>")
                .append("</select>")
                .append("</div>");

        // Фильтр по дате
        content.append("<div class='form-group'>")
                .append("<label for='date' class='form-label'>Дата начала</label>")
                .append("<input type='date' class='form-control' id='date' name='date' value='")
                .append(dateFilter != null ? dateFilter : "").append("'>")
                .append("</div>");

        content.append("</div>") // Закрываем info-grid
                .append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-primary'> Применить</button>")
                .append("<a href='/waybills' class='btn btn-secondary'> Сбросить</a>")
                .append("</div>")
                .append("</form>")
                .append("</div>");

        // Таблица путевых листов
        if (waybills.isEmpty()) {
            content.append("<div class='card text-center fade-in'>")
                    .append("<div class='empty-state'>")
                    .append("<div class='empty-icon'></div>")
                    .append("<h3>Нет путевых листов</h3>")
                    .append("<p>По выбранным фильтрам ничего не найдено</p>")
                    .append("<a href='/waybills/new' class='btn btn-success mt-20'>Открыть первую смену</a>")
                    .append("</div>")
                    .append("</div>");
        } else {
            content.append("<div class='card fade-in'>")
                    .append("<div class='table-container'>")
                    .append("<table>")
                    .append("<thead>")
                    .append("<tr>")
                    .append("<th>ID</th>")
                    .append("<th>Водитель</th>")
                    .append("<th>Автомобиль</th>")
                    .append("<th>Начало</th>")
                    .append("<th>Конец</th>")
                    .append("<th>Пробег</th>")
                    .append("<th>Заработок</th>")
                    .append("<th>Статус</th>")
                    .append("<th>Действия</th>")
                    .append("</tr>")
                    .append("</thead>")
                    .append("<tbody>");

            for (Waybill waybill : waybills) {
                String statusClass = getStatusClass(waybill.getStatus());
                String statusText = getStatusText(waybill.getStatus());

                String carInfo = waybill.getCar() != null ?
                        waybill.getCar().getLicensePlate() + "<br><small>" + waybill.getCar().getModel() + "</small>" :
                        "-";

                content.append("<tr>")
                        .append("<td><strong>#").append(waybill.getId()).append("</strong></td>")
                        .append("<td><strong>").append(waybill.getDriver().getFullName()).append("</strong></td>")
                        .append("<td>").append(carInfo).append("</td>")
                        .append("<td>").append(dateFormatter.format(waybill.getStartTime())).append("</td>")
                        .append("<td>").append(waybill.getEndTime() != null ?
                                dateFormatter.format(waybill.getEndTime()) : "-").append("</td>")
                        .append("<td>").append(waybill.getShiftMileage() > 0 ?
                                waybill.getShiftMileage() + " км" : "-").append("</td>")
                        .append("<td>").append(waybill.getTotalEarnings() != null ?
                                String.format("%.2f ₽", waybill.getTotalEarnings()) : "-").append("</td>")
                        .append("<td><span class='badge ").append(statusClass).append("'>").append(statusText).append("</span></td>")
                        .append("<td>")
                        .append("<div class='action-buttons-small'>");

                if (waybill.getStatus() == Waybill.WaybillStatus.ACTIVE) {
                    content.append("<a href='/waybills/close?id=").append(waybill.getId())
                            .append("' class='btn btn-sm btn-success' title='Завершить смену'> Завершить</a>");
                    content.append("<a href='/waybills/view?id=").append(waybill.getId())
                            .append("' class='btn btn-sm btn-primary' title='Просмотр'>️ Просмотр</a>");
                } else {
                    content.append("<a href='/waybills/view?id=").append(waybill.getId())
                            .append("' class='btn btn-sm btn-primary' title='Просмотр'> Просмотр</a>");
                    content.append("<a href='/waybills/confirm-delete?id=").append(waybill.getId())
                            .append("' class='btn btn-sm btn-danger' title='Удалить путевой лист'> Удалить</a>");
                }

                content.append("</div>")
                        .append("</td>")
                        .append("</tr>");
            }

            content.append("</tbody>")
                    .append("</table>")
                    .append("</div>");

            // Количество записей
            content.append("<div class='mt-20' style='padding-top: 15px; border-top: 1px solid #333;'>")
                    .append("<div style='color: #888; font-size: 0.9em;'>Показано: ").append(waybills.size()).append(" записей</div>")
                    .append("</div>")
                    .append("</div>");
        }

        // Статистика
        content.append(showStatisticsHtml());

        HtmlUtil.renderFullPage(out, request, "Путевые листы", "waybills", content.toString());
    }

    // ==================== ДЕТАЛИ ПУТЕВОГО ЛИСТА (НОВЫЙ МЕТОД) ====================

    private void showWaybillDetails(Long waybillId, PrintWriter out, HttpServletRequest request) {
        try {
            Waybill waybill = waybillService.getWaybillById(waybillId);
            if (waybill == null) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка", "Путевой лист не найден");
                return;
            }

            // Проверка прав для DRIVER
            User currentUser = (User) request.getSession().getAttribute("user");
            String userRole = currentUser.getUserType();

            if ("DRIVER".equals(userRole)) {
                Driver driver = driverRepository.findByUserId(currentUser.getId());
                if (driver == null || !driver.getId().equals(waybill.getDriver().getId())) {
                    HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                    return;
                }
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>")
                    .append("<div class='card-header'>")
                    .append("<h2 class='card-title'> Путевой лист #").append(waybill.getId()).append("</h2>")
                    .append("</div>")
                    .append("<div class='card-body'>");

            // Информация о путевом листе в виде сетки
            content.append("<div class='info-grid'>");

            // Водитель и автомобиль
            content.append("<div class='info-section'>")
                    .append("<h3> Водитель и авто</h3>")
                    .append("<p><strong>Водитель:</strong> ").append(waybill.getDriver().getFullName()).append("</p>")
                    .append("<p><strong>Телефон:</strong> ").append(waybill.getDriver().getPhone() != null ?
                            waybill.getDriver().getPhone() : "-").append("</p>")
                    .append("<p><strong>В/у:</strong> ").append(waybill.getDriver().getLicenseNumber()).append("</p>")
                    .append("<p><strong>Автомобиль:</strong> ").append(waybill.getCar().getLicensePlate())
                    .append(" (").append(waybill.getCar().getBrand()).append(" ").append(waybill.getCar().getModel()).append(")</p>")
                    .append("</div>");

            // Время и пробег
            content.append("<div class='info-section'>")
                    .append("<h3> Время и пробег</h3>")
                    .append("<p><strong>Начало смены:</strong> ").append(dateFormatter.format(waybill.getStartTime())).append("</p>");

            if (waybill.getEndTime() != null) {
                content.append("<p><strong>Конец смены:</strong> ").append(dateFormatter.format(waybill.getEndTime())).append("</p>");
                long hours = java.time.Duration.between(waybill.getStartTime(), waybill.getEndTime()).toHours();
                long minutes = java.time.Duration.between(waybill.getStartTime(), waybill.getEndTime()).toMinutesPart();
                content.append("<p><strong>Продолжительность:</strong> ").append(hours).append(" ч ").append(minutes).append(" мин</p>");
            }

            content.append("<p><strong>Начальный пробег:</strong> ").append(waybill.getInitialMileageKm() != null ?
                            waybill.getInitialMileageKm() + " км" : "-").append("</p>")
                    .append("<p><strong>Конечный пробег:</strong> ").append(waybill.getFinalMileage() != null ?
                            waybill.getFinalMileage() + " км" : "-").append("</p>")
                    .append("<p><strong>Пробег за смену:</strong> ").append(waybill.getShiftMileage()).append(" км</p>")
                    .append("</div>");

            // Финансы и статус
            content.append("<div class='info-section'>")
                    .append("<h3> Финансы</h3>")
                    .append("<p><strong>Статус:</strong> ").append(getStatusBadge(waybill.getStatus())).append("</p>")
                    .append("<p><strong>Заработок водителя:</strong> ").append(waybill.getTotalEarnings() != null ?
                            String.format("%.2f ₽", waybill.getTotalEarnings()) : "-").append("</p>")
                    .append("<p><strong>Выручка:</strong> ").append(waybill.getTotalRevenue() != null ?
                            String.format("%.2f ₽", waybill.getTotalRevenue()) : "-").append("</p>")
                    .append("</div>");

            // Персонал
            content.append("<div class='info-section'>")
                    .append("<h3> Персонал</h3>")
                    .append("<p><strong>Открыл смену:</strong> ").append(waybill.getDoctor() != null ?
                            waybill.getDoctor().getFullName() : "-").append("</p>");

            if (waybill.getMechanic() != null) {
                content.append("<p><strong>Закрыл смену:</strong> ").append(waybill.getMechanic().getFullName()).append("</p>");
            }

            if (waybill.getNotes() != null && !waybill.getNotes().isEmpty()) {
                content.append("<p><strong>Примечания:</strong> ").append(waybill.getNotes()).append("</p>");
            }
            content.append("</div>");

            content.append("</div>");

            //  СЕКЦИЯ С ЗАКАЗАМИ
            content.append("<div class='info-section mt-30'>")
                    .append("<h3>📦 Заказы в этом путевом листе</h3>");

            // Получаем заказы, связанные с этим путевым листом
            List<Order> waybillOrders = orderService.getOrdersByWaybillId(waybillId);

            if (waybillOrders.isEmpty()) {
                content.append("<div class='empty-state'>")
                        .append("<div class='empty-icon'></div>")
                        .append("<p>В этом путевом листе пока нет заказов</p>")
                        .append("</div>");
            } else {
                content.append("<div class='orders-list'>");

                double totalWaybillRevenue = 0;
                double totalWaybillDistance = 0;

                for (Order order : waybillOrders) {
                    content.append("<div class='order-item'>")
                            .append("<div class='order-header'>")
                            .append("<strong>Заказ #").append(order.getId()).append("</strong>");

                    if (order.getCompletionTime() != null) {
                        content.append("<span class='order-time'>").append(dateFormatter.format(order.getCompletionTime())).append("</span>");
                    }

                    content.append("</div>")
                            .append("<div class='order-route'>")
                            .append("📍 ").append(order.getPickupAddress());

                    if (order.getDestinationAddress() != null) {
                        content.append(" → 📍 ").append(order.getDestinationAddress());
                    }

                    content.append("</div>")
                            .append("<div class='order-stats'>");

                    if (order.getPrice() != null) {
                        content.append("<span class='order-price'> ").append(String.format("%.2f", order.getPrice())).append(" руб.</span> ");
                        totalWaybillRevenue += order.getPrice();
                    }

                    if (order.getDistanceKm() != null) {
                        content.append("<span class='order-distance'> ").append(String.format("%.1f", order.getDistanceKm())).append(" км</span>");
                        totalWaybillDistance += order.getDistanceKm();
                    }

                    content.append("</div>")
                            .append("</div>"); // закрываем order-item
                }

                // Итоги по путевому листу
                content.append("<div class='waybill-totals mt-20'>")
                        .append("<h4> Итоги смены:</h4>")
                        .append("<div class='totals-grid'>")
                        .append("<div class='total-item'>")
                        .append("<div class='total-label'>Количество заказов</div>")
                        .append("<div class='total-value'>").append(waybillOrders.size()).append("</div>")
                        .append("</div>")
                        .append("<div class='total-item'>")
                        .append("<div class='total-label'>Общая дистанция</div>")
                        .append("<div class='total-value'>").append(String.format("%.1f", totalWaybillDistance)).append(" км</div>")
                        .append("</div>")
                        .append("<div class='total-item'>")
                        .append("<div class='total-label'>Общая выручка</div>")
                        .append("<div class='total-value'>").append(String.format("%.2f", totalWaybillRevenue)).append(" руб.</div>")
                        .append("</div>")
                        .append("<div class='total-item'>")
                        .append("<div class='total-label'>Средний чек</div>")
                        .append("<div class='total-value'>").append(String.format("%.2f",
                                waybillOrders.size() > 0 ? totalWaybillRevenue / waybillOrders.size() : 0)).append(" руб.</div>")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>");

                content.append("</div>"); // закрываем orders-list
            }

            content.append("</div>");
            //  КОНЕЦ СЕКЦИИ С ЗАКАЗАМИ

            // Кнопки действий
            content.append("<div class='action-buttons mt-30'>")
                    .append("<a href='/waybills' class='btn btn-secondary'>← Назад к списку</a>");

            if (waybill.getStatus() == Waybill.WaybillStatus.ACTIVE) {
                content.append("<a href='/waybills/close?id=").append(waybill.getId())
                        .append("' class='btn btn-success'> Завершить смену</a>");
            }

            content.append("</div>")
                    .append("</div>") // закрываем card-body
                    .append("</div>"); // закрываем card

            HtmlUtil.renderFullPage(out, request, "Путевой лист #" + waybill.getId(),
                    "DRIVER".equals(userRole) ? "driver-waybills" : "waybills",
                    content.toString());

        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не удалось загрузить путевой лист: " + e.getMessage());
        }
    }


    private String showFiltersHtml(String driverFilter, String carFilter,
                                   String statusFilter, String dateFilter, String search,
                                   List<Driver> allDrivers, List<Car> allCars) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='card mb-20'>")
                .append("<h3>🔍 Фильтры</h3>")
                .append("<form method='get' action='/waybills' class='form-horizontal'>")
                .append("<div class='info-grid'>");

        // Поиск
        html.append("<div class='form-group'>")
                .append("<label for='search' class='form-label'>Поиск</label>")
                .append("<input type='text' class='form-control' id='search' name='search' ")
                .append("placeholder='ФИО водителя, номер авто...' value='").append(search != null ? search : "").append("'>")
                .append("</div>");

        // Фильтр по водителю
        html.append("<div class='form-group'>")
                .append("<label for='driver' class='form-label'>Водитель</label>")
                .append("<select class='form-control' id='driver' name='driver'>")
                .append("<option value=''>Все водители</option>");

        for (Driver driver : allDrivers) {
            boolean selected = driverFilter != null && driverFilter.equals(driver.getId().toString());
            html.append("<option value='").append(driver.getId()).append("' ")
                    .append(selected ? "selected" : "").append(">")
                    .append(driver.getFullName()).append(" (").append(driver.getLicenseNumber()).append(")</option>");
        }

        html.append("</select>")
                .append("</div>");

        // Фильтр по автомобилю
        html.append("<div class='form-group'>")
                .append("<label for='car' class='form-label'>Автомобиль</label>")
                .append("<select class='form-control' id='car' name='car'>")
                .append("<option value=''>Все автомобили</option>");

        for (Car car : allCars) {
            boolean selected = carFilter != null && carFilter.equals(car.getId().toString());
            html.append("<option value='").append(car.getId()).append("' ")
                    .append(selected ? "selected" : "").append(">")
                    .append(car.getLicensePlate()).append(" (").append(car.getModel()).append(")</option>");
        }

        html.append("</select>")
                .append("</div>");

        // Фильтр по статусу
        html.append("<div class='form-group'>")
                .append("<label for='status' class='form-label'>Статус</label>")
                .append("<select class='form-control' id='status' name='status'>")
                .append("<option value=''>Все статусы</option>")
                .append("<option value='active' ").append("active".equals(statusFilter) ? "selected" : "").append(">Активные</option>")
                .append("<option value='completed' ").append("completed".equals(statusFilter) ? "selected" : "").append(">Завершенные</option>")
                .append("<option value='cancelled' ").append("cancelled".equals(statusFilter) ? "selected" : "").append(">Отмененные</option>")
                .append("</select>")
                .append("</div>");

        // Фильтр по дате
        html.append("<div class='form-group'>")
                .append("<label for='date' class='form-label'>Дата начала</label>")
                .append("<input type='date' class='form-control' id='date' name='date' value='")
                .append(dateFilter != null ? dateFilter : "").append("'>")
                .append("</div>");

        html.append("</div>") // Закрываем info-grid
                .append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-primary'> Применить</button>")
                .append("<a href='/waybills' class='btn btn-secondary'> Сбросить</a>")
                .append("</div>")
                .append("</form>")
                .append("</div>");

        return html.toString();
    }

    private String showStatisticsHtml() {
        List<Waybill> allWaybills = waybillService.getAllWaybills();
        List<Waybill> activeWaybills = waybillService.getActiveWaybills();
        List<Waybill> completedWaybills = waybillService.getCompletedWaybills();

        long total = allWaybills.size();
        long active = activeWaybills.size();
        long completed = completedWaybills.size();
        long cancelled = total - active - completed;

        double totalEarnings = allWaybills.stream()
                .filter(w -> w.getTotalEarnings() != null)
                .mapToDouble(Waybill::getTotalEarnings)
                .sum();

        int totalMileage = allWaybills.stream()
                .mapToInt(Waybill::getShiftMileage)
                .sum();

        double avgEarnings = completed > 0 ? totalEarnings / completed : 0;
        double avgMileage = completed > 0 ? (double) totalMileage / completed : 0;

        StringBuilder html = new StringBuilder();

        html.append("<div class='card fade-in'>")
                .append("<h3> Статистика путевых листов</h3>")
                .append("<div class='stats-grid'>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(total).append("</div>")
                .append("<div class='stat-label'>Всего смен</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(active).append("</div>")
                .append("<div class='stat-label'>Активные</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(completed).append("</div>")
                .append("<div class='stat-label'>Завершены</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(String.format("%.0f", totalEarnings)).append("</div>")
                .append("<div class='stat-label'>Общий доход (₽)</div>")
                .append("</div>");

        if (completed > 0) {
            html.append("<div class='stat-card'>")
                    .append("<div class='stat-icon'></div>")
                    .append("<div class='stat-value'>").append(String.format("%.0f", avgEarnings)).append("</div>")
                    .append("<div class='stat-label'>Ср. доход (₽)</div>")
                    .append("</div>");
        }

        html.append("</div>")
                .append("</div>");

        return html.toString();
    }

    private String showDriverStatistics(List<Waybill> driverWaybills) {
        long total = driverWaybills.size();
        long completed = driverWaybills.stream()
                .filter(w -> w.getStatus() == Waybill.WaybillStatus.COMPLETED)
                .count();
        long active = driverWaybills.stream()
                .filter(w -> w.getStatus() == Waybill.WaybillStatus.ACTIVE)
                .count();

        double totalEarnings = driverWaybills.stream()
                .filter(w -> w.getTotalEarnings() != null)
                .mapToDouble(Waybill::getTotalEarnings)
                .sum();

        int totalMileage = driverWaybills.stream()
                .mapToInt(Waybill::getShiftMileage)
                .sum();

        StringBuilder html = new StringBuilder();

        html.append("<div class='card fade-in'>")
                .append("<h3> Моя статистика</h3>")
                .append("<div class='stats-grid'>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(total).append("</div>")
                .append("<div class='stat-label'>Всего смен</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(String.format("%.0f", totalEarnings)).append("</div>")
                .append("<div class='stat-label'>Общий доход</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalMileage).append("</div>")
                .append("<div class='stat-label'>Общий пробег (км)</div>")
                .append("</div>");

        if (completed > 0) {
            double avgEarnings = totalEarnings / completed;
            html.append("<div class='stat-card'>")
                    .append("<div class='stat-icon'></div>")
                    .append("<div class='stat-value'>").append(String.format("%.0f", avgEarnings)).append("</div>")
                    .append("<div class='stat-label'>Ср. доход за смену</div>")
                    .append("</div>");
        }

        html.append("</div>")
                .append("</div>");

        return html.toString();
    }

    // Вспомогательные методы для получения классов и текста статуса
    private String getStatusClass(Waybill.WaybillStatus status) {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case ACTIVE -> "badge-success";
            case COMPLETED -> "badge-info";
            case CANCELLED -> "badge-danger";
        };
    }

    private String getStatusText(Waybill.WaybillStatus status) {
        if (status == null) return "Неизвестно";
        return switch (status) {
            case ACTIVE -> "Активна";
            case COMPLETED -> "Завершена";
            case CANCELLED -> "Отменена";
        };
    }

    private String getStatusBadge(Waybill.WaybillStatus status) {
        return "<span class='badge " + getStatusClass(status) + "'>" + getStatusText(status) + "</span>";
    }

    // ==================== ОСТАЛЬНЫЕ МЕТОДЫ ====================

    private void showCreateForm(PrintWriter out, HttpServletRequest request) {
        List<Driver> allowedDrivers = medicalCheckService.getAllowedDrivers();
        List<User> technicians = userRepository.findByRole("MECHANIC");

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>")
                .append("<div class='card-header'>")
                .append("<h2 class='card-title'> Открыть новую смену</h2>")
                .append("</div>")
                .append("<div class='card-body'>");

        if (allowedDrivers.isEmpty() || technicians.isEmpty()) {
            content.append("<div class='alert alert-warning'>")
                    .append("<p>⚠ Нельзя открыть смену:</p>");
            if (allowedDrivers.isEmpty()) {
                content.append("<p>• Нет допущенных водителей</p>");
            }
            if (technicians.isEmpty()) {
                content.append("<p>• Нет доступных техников</p>");
            }
            content.append("<p style='margin-top: 10px;'>")
                    .append("<a href='/medical-checks' class='btn btn-sm'> Медосмотры</a>")
                    .append("<a href='/inspections' class='btn btn-sm'>🔧 Техосмотры</a>")
                    .append("</p>")
                    .append("</div>");

            content.append("</div>") // card-body
                    .append("</div>"); // card

            HtmlUtil.renderFullPage(out, request, "Открыть смену", "waybills", content.toString());
            return;
        }

        content.append("<form method='post' action='/waybills/save' class='form-vertical'>")
                .append("<div class='info-grid'>");

        // Водитель
        content.append("<div class='form-group'>")
                .append("<label for='driverId' class='form-label'>Водитель <span class='required'>*</span></label>")
                .append("<select class='form-control' id='driverId' name='driverId' required onchange='updateDriverInfo(this.value)'>")
                .append("<option value=''>-- Выберите водителя --</option>");

        for (Driver driver : allowedDrivers) {
            boolean hasCar = driver.getCurrentCar() != null;
            String carInfo = hasCar ?
                    " ( " + driver.getCurrentCar().getLicensePlate() + ")" :
                    " ( Нет авто)";

            content.append("<option value='").append(driver.getId()).append("' ")
                    .append("data-hascar='").append(hasCar).append("' ")
                    .append("data-carinfo='").append(hasCar ?
                            driver.getCurrentCar().getLicensePlate() + " - " + driver.getCurrentCar().getModel() : "").append("'>")
                    .append(driver.getFullName()).append(" ").append(carInfo).append("</option>");
        }

        content.append("</select>")
                .append("<div id='carInfo' style='margin-top: 10px; display: none;'></div>")
                .append("</div>");

        // Техник
        content.append("<div class='form-group'>")
                .append("<label for='technicianId' class='form-label'>Техник <span class='required'>*</span></label>")
                .append("<select class='form-control' id='technicianId' name='technicianId' required>")
                .append("<option value=''>-- Выберите техника --</option>");

        for (User technician : technicians) {
            content.append("<option value='").append(technician.getId()).append("'>")
                    .append(technician.getFullName()).append(" (").append(technician.getLogin()).append(")</option>");
        }

        content.append("</select>")
                .append("</div>");

        content.append("</div>"); // Закрываем info-grid

        // Начальный пробег
        content.append("<div class='form-group'>")
                .append("<label for='initialMileage' class='form-label'>Начальный пробег (км) <span class='required'>*</span></label>")
                .append("<input type='number' class='form-control' id='initialMileage' name='initialMileage' required min='0' placeholder='Текущий пробег автомобиля'>")
                .append("<p class='form-hint'>Укажите текущий пробег автомобиля на момент начала смены</p>")
                .append("</div>");

        // Примечания
        content.append("<div class='form-group'>")
                .append("<label for='notes' class='form-label'>Примечания</label>")
                .append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Дополнительная информация...'></textarea>")
                .append("</div>");

        // Кнопки
        content.append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-success' id='submitBtn'> Открыть смену</button>")
                .append("<a href='/waybills' class='btn btn-danger'> Отмена</a>")
                .append("</div>")
                .append("</form>");

        content.append("</div>") // закрываем card-body
                .append("</div>"); // закрываем card

        HtmlUtil.renderFullPage(out, request, "Открыть смену", "waybills", content.toString());
    }

    private void showEditForm(long id, PrintWriter out, HttpServletRequest request) {
        // Перенаправляем на новый метод просмотра
        showWaybillDetails(id, out, request);
    }

    private void showCloseForm(long id, PrintWriter out, HttpServletRequest request) {
        try {
            Waybill waybill = waybillService.getWaybillById(id);
            if (waybill == null || waybill.getStatus() != Waybill.WaybillStatus.ACTIVE) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка", "Смена не найдена или уже завершена");
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>")
                    .append("<div class='card-header'>")
                    .append("<h2 class='card-title'> Завершить смену</h2>")
                    .append("</div>")
                    .append("<div class='card-body'>");

            // Информация о текущей смене
            content.append("<div class='info-section mb-30'>")
                    .append("<h4> Информация о смене</h4>")
                    .append("<div class='info-grid'>")
                    .append("<div><strong> Водитель:</strong><br>").append(waybill.getDriver().getFullName()).append("</div>")
                    .append("<div><strong> Автомобиль:</strong><br>").append(waybill.getCar().getLicensePlate())
                    .append(" (").append(waybill.getCar().getModel()).append(")</div>")
                    .append("<div><strong> Начало смены:</strong><br>").append(dateFormatter.format(waybill.getStartTime())).append("</div>")
                    .append("<div><strong> Начальный пробег:</strong><br>").append(waybill.getInitialMileageKm() != null ?
                            waybill.getInitialMileageKm() + " км" : "-").append("</div>")
                    .append("</div>")
                    .append("</div>");

            // Получаем заказы из этой смены для отображения статистики
            List<Order> waybillOrders = orderService.getOrdersByWaybillId(id);

            if (!waybillOrders.isEmpty()) {
                content.append("<div class='info-section mb-30'>")
                        .append("<h4> Статистика заказов</h4>")
                        .append("<div class='info-grid'>")
                        .append("<div><strong> Количество заказов:</strong><br>").append(waybillOrders.size()).append("</div>");

                double totalRevenue = 0.0;
                double totalDistance = 0.0;
                for (Order order : waybillOrders) {
                    if (order.getPrice() != null) totalRevenue += order.getPrice();
                    if (order.getDistanceKm() != null) totalDistance += order.getDistanceKm();
                }

                content.append("<div><strong> Общая выручка:</strong><br>").append(String.format("%.2f", totalRevenue)).append(" руб.</div>")
                        .append("<div><strong> Общая дистанция:</strong><br>").append(String.format("%.1f", totalDistance)).append(" км</div>")
                        .append("<div><strong> Средний чек:</strong><br>").append(String.format("%.2f",
                                waybillOrders.size() > 0 ? totalRevenue / waybillOrders.size() : 0)).append(" руб.</div>")
                        .append("</div>")
                        .append("</div>");
            }

            content.append("<form method='post' action='/waybills/update' class='form-vertical'>")
                    .append("<input type='hidden' name='id' value='").append(waybill.getId()).append("'>");

            content.append("<div class='info-grid'>");

            // Конечный пробег
            int initialMileage = waybill.getInitialMileageKm() != null ? waybill.getInitialMileageKm() : 0;
            int suggestedFinalMileage = initialMileage + 50; // Предлагаем на 50 км больше

            content.append("<div class='form-group'>")
                    .append("<label for='finalMileage' class='form-label'>Конечный пробег (км) <span class='required'>*</span></label>")
                    .append("<input type='number' class='form-control' id='finalMileage' name='finalMileage' required ")
                    .append("min='").append(initialMileage + 1).append("' ")
                    .append("value='").append(suggestedFinalMileage).append("'>")
                    .append("<p class='form-hint'>Должен быть больше начального (").append(initialMileage).append(" км)</p>")
                    .append("</div>");

            // Заработок
            double suggestedEarnings = waybillOrders.isEmpty() ? 2500.00 :
                    waybillOrders.stream()
                            .filter(o -> o.getPrice() != null)
                            .mapToDouble(Order::getPrice)
                            .sum() * 0.8; // 80% водителю, 20% таксопарку

            content.append("<div class='form-group'>")
                    .append("<label for='totalEarnings' class='form-label'>Заработок водителя (₽) <span class='required'>*</span></label>")
                    .append("<input type='number' class='form-control' id='totalEarnings' name='totalEarnings' required ")
                    .append("min='0' step='0.01' value='").append(String.format("%.2f", suggestedEarnings)).append("'>")
//                    .append("<p class='form-hint'>Заработок водителя после вычета комиссии (20%)</p>")
                    .append("</div>");

            content.append("</div>"); // Закрываем info-grid

            // Примечания
            content.append("<div class='form-group'>")
                    .append("<label for='notes' class='form-label'>Примечания по смене</label>")
                    .append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Особенности смены, проблемы и т.д.'></textarea>")
                    .append("</div>");

            content.append("<div class='form-actions'>")
                    .append("<button type='submit' class='btn btn-success'> Завершить смену</button>")
                    .append("<a href='/waybills' class='btn btn-danger'> Отмена</a>")
                    .append("</div>")
                    .append("</form>")
                    .append("</div>") // закрываем card-body
                    .append("</div>"); // закрываем card

            HtmlUtil.renderFullPage(out, request, "Завершить смену", "waybills", content.toString());
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не удалось загрузить информацию о смене: " + e.getMessage());
        }
    }

    private List<Waybill> getFilteredWaybills(String driverFilter, String carFilter,
                                              String statusFilter, String dateFilter,
                                              String search) {
        List<Waybill> allWaybills = waybillService.getAllWaybills();

        return allWaybills.stream()
                .filter(waybill -> {
                    // Фильтр по водителю
                    if (driverFilter != null && !driverFilter.isEmpty()) {
                        try {
                            Long driverId = Long.parseLong(driverFilter);
                            if (!waybill.getDriver().getId().equals(driverId)) {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            // Игнорируем неверный формат
                        }
                    }

                    // Фильтр по автомобилю
                    if (carFilter != null && !carFilter.isEmpty()) {
                        try {
                            Long carId = Long.parseLong(carFilter);
                            if (!waybill.getCar().getId().equals(carId)) {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            // Игнорируем неверный формат
                        }
                    }

                    // Фильтр по статусу
                    if (statusFilter != null && !statusFilter.isEmpty()) {
                        if ("active".equals(statusFilter) && waybill.getStatus() != Waybill.WaybillStatus.ACTIVE) {
                            return false;
                        }
                        if ("completed".equals(statusFilter) && waybill.getStatus() != Waybill.WaybillStatus.COMPLETED) {
                            return false;
                        }
                        if ("cancelled".equals(statusFilter) && waybill.getStatus() != Waybill.WaybillStatus.CANCELLED) {
                            return false;
                        }
                    }

                    // Фильтр по дате
                    if (dateFilter != null && !dateFilter.isEmpty()) {
                        try {
                            LocalDate filterDate = LocalDate.parse(dateFilter, dateOnlyFormatter);
                            LocalDate waybillDate = waybill.getStartTime().toLocalDate();
                            if (!waybillDate.equals(filterDate)) {
                                return false;
                            }
                        } catch (DateTimeParseException e) {
                            // Игнорируем неверный формат даты
                        }
                    }

                    // Поиск по тексту
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = waybill.getDriver().getFullName().toLowerCase().contains(searchLower) ||
                                waybill.getCar().getLicensePlate().toLowerCase().contains(searchLower) ||
                                waybill.getCar().getModel().toLowerCase().contains(searchLower) ||
                                (waybill.getNotes() != null && waybill.getNotes().toLowerCase().contains(searchLower));
                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((w1, w2) -> w2.getStartTime().compareTo(w1.getStartTime()))
                .collect(Collectors.toList());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Проверка авторизации
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();

        // Только ADMIN и MECHANIC могут выполнять POST-запросы
        if (!"ADMIN".equals(userRole) && !"MECHANIC".equals(userRole)) {
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            HtmlUtil.renderAccessDeniedPage(out, request, userRole);
            return;
        }

        String path = request.getPathInfo();

        try {
            if (path == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Не указано действие");
                return;
            }

            switch (path) {
                case "/save":
                    saveWaybill(request, response);
                    break;
                case "/update":
                    updateWaybill(request, response);
                    break;
                case "/delete":
                    deleteWaybill(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/waybills?error=" + e.getMessage());
        }
    }

    // Новый метод для обработки POST-запроса на удаление
    private void deleteWaybill(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String idParam = request.getParameter("id");
            if (idParam == null || idParam.isEmpty()) {
                throw new IllegalArgumentException("Не указан ID путевого листа");
            }

            Long waybillId = Long.parseLong(idParam);

            // Проверяем существование и статус
            Waybill waybill = waybillService.getWaybillById(waybillId);
            if (waybill == null) {
                throw new IllegalArgumentException("Путевой лист не найден");
            }

            if (waybill.getStatus() == Waybill.WaybillStatus.ACTIVE) {
                throw new IllegalStateException("Нельзя удалить активный путевой лист");
            }

            // Удаляем путевой лист
            waybillService.deleteWaybill(waybillId);

            response.sendRedirect(request.getContextPath() + "/waybills?success=deleted");

        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/waybills?error=" +
                    URLEncoder.encode(e.getMessage(), "UTF-8"));
        }
    }

    private void saveWaybill(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String driverIdParam = request.getParameter("driverId");
            String technicianIdParam = request.getParameter("technicianId");
            String initialMileageParam = request.getParameter("initialMileage");
            String notes = request.getParameter("notes");

            if (driverIdParam == null || driverIdParam.isEmpty()) {
                throw new IllegalArgumentException("Не указан водитель");
            }
            if (technicianIdParam == null || technicianIdParam.isEmpty()) {
                throw new IllegalArgumentException("Не указан техник");
            }
            if (initialMileageParam == null || initialMileageParam.isEmpty()) {
                throw new IllegalArgumentException("Не указан начальный пробег");
            }

            Long driverId = Long.parseLong(driverIdParam);

            // Проверяем на сервере, есть ли у водителя автомобиль
            Driver driver = driverRepository.findById(driverId);
            if (driver == null) {
                throw new IllegalArgumentException("Водитель не найден");
            }

            if (driver.getCurrentCar() == null) {
                // Возвращаем на форму с ошибкой
                response.sendRedirect(request.getContextPath() + "/waybills/new?error=" +
                        URLEncoder.encode("У водителя нет назначенного автомобиля", "UTF-8"));
                return;
            }

            Long technicianId = Long.parseLong(technicianIdParam);
            Integer initialMileage = Integer.parseInt(initialMileageParam);

            Waybill waybill = waybillService.createWaybill(driverId, technicianId, initialMileage, notes);

            response.sendRedirect(request.getContextPath() + "/waybills?success=opened");

        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/waybills/new?error=" +
                    URLEncoder.encode(e.getMessage(), "UTF-8"));
        }
    }

    private void updateWaybill(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Long waybillId = Long.parseLong(request.getParameter("id"));
            Integer finalMileage = Integer.parseInt(request.getParameter("finalMileage"));
            Double totalEarnings = Double.parseDouble(request.getParameter("totalEarnings"));
            String notes = request.getParameter("notes");

            // В реальном приложении mechanicId должен браться из сессии
            User currentUser = (User) request.getSession().getAttribute("user");
            Long mechanicId = currentUser.getId();

            Waybill waybill = waybillService.completeWaybill(waybillId, mechanicId, finalMileage, totalEarnings, notes);

            response.sendRedirect(request.getContextPath() + "/waybills?success=closed");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/waybills?error=" + e.getMessage());
        }
    }

    // Надо посмотреть, где используется, так как отображение заказов перенесено теперь в showWaybillDetails
    private void showWaybillOrders(Long waybillId, PrintWriter out, HttpServletRequest request) {
        showWaybillDetails(waybillId, out, request);
    }

    private void showDeleteConfirmation(Long waybillId, PrintWriter out, HttpServletRequest request) {
        try {
            Waybill waybill = waybillService.getWaybillById(waybillId);
            if (waybill == null) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка", "Путевой лист не найден");
                return;
            }

            // Проверяем, можно ли удалять (только завершенные или отмененные)
            if (waybill.getStatus() == Waybill.WaybillStatus.ACTIVE) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка",
                        "Нельзя удалить активный путевой лист. Завершите смену сначала.");
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>")
                    .append("<div class='card-header'>")
                    .append("<h2 class='card-title'>🗑️ Подтверждение удаления</h2>")
                    .append("</div>")
                    .append("<div class='card-body'>");

            content.append("<div style='text-align: center; margin: 30px 0;'>")
                    .append("<div style='font-size: 64px; color: #f44336; margin-bottom: 20px;'>⚠</div>")
                    .append("<h3 style='color: #fff; margin-bottom: 15px;'>Удалить путевой лист?</h3>")
                    .append("<p style='color: #aaa; font-size: 1.1em; max-width: 500px; margin: 0 auto 30px;'>")
                    .append("Вы уверены, что хотите удалить путевой лист #").append(waybillId).append("?<br>")
                    .append("Это действие нельзя отменить.</p>")
                    .append("</div>");

            // Информация о путевом листе
            content.append("<div class='info-section mb-30'>")
                    .append("<h4> Информация о путевом листе</h4>")
                    .append("<div class='info-grid'>")
                    .append("<div><strong> Водитель:</strong><br>").append(waybill.getDriver().getFullName()).append("</div>")
                    .append("<div><strong> Автомобиль:</strong><br>").append(waybill.getCar().getLicensePlate())
                    .append(" (").append(waybill.getCar().getModel()).append(")</div>")
                    .append("<div><strong> Дата начала:</strong><br>").append(dateFormatter.format(waybill.getStartTime())).append("</div>")
                    .append("<div><strong> Статус:</strong><br>").append(getStatusText(waybill.getStatus())).append("</div>")
                    .append("</div>")
                    .append("</div>");

            // Кнопки действий
            content.append("<div class='form-actions' style='justify-content: center;'>")
                    .append("<form method='post' action='/waybills/delete' style='display: inline;'>")
                    .append("<input type='hidden' name='id' value='").append(waybillId).append("'>")
                    .append("<button type='submit' class='btn btn-danger' style='padding: 12px 30px; font-size: 1.1em;'>")
                    .append(" Да, удалить</button>")
                    .append("</form>")
                    .append("<a href='/waybills' class='btn btn-secondary' style='padding: 12px 30px; font-size: 1.1em; margin-left: 15px;'>")
                    .append(" Отмена</a>")
                    .append("</div>");

            content.append("</div>") // закрываем card-body
                    .append("</div>"); // закрываем card

            HtmlUtil.renderFullPage(out, request, "Удаление путевого листа", "waybills", content.toString());

        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка",
                    "Не удалось загрузить информацию о путевом листе: " + e.getMessage());
        }
    }
}