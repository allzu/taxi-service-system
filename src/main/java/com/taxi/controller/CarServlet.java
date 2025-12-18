package com.taxi.controller;

import com.taxi.entity.User;
import com.taxi.entity.Car;
import com.taxi.service.CarService;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/cars/*")
public class CarServlet extends BaseServlet {
    private CarService carService = new CarService();
    private HttpServletRequest currentRequest;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.currentRequest = req;

        User currentUser = (User) req.getSession().getAttribute("user");
        if (currentUser == null) {
            resp.sendRedirect("/login");
            return;
        }

        // Проверяем права доступа к странице автомобилей
        String userRole = currentUser.getUserType();
        if (!"ADMIN".equals(userRole) && !"MECHANIC".equals(userRole)) {
            resp.setContentType("text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            HtmlUtil.renderAccessDeniedPage(out, req, userRole);
            return;
        }

        String path = req.getPathInfo();
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            if (path == null || path.equals("/") || path.isEmpty()) {
                // Основной список автомобилей
                showCarsList(req, out, currentUser);
            } else if (path.equals("/new")) {
                // Проверка доступа для создания
                if (!"MECHANIC".equals(userRole) && !"ADMIN".equals(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, req, userRole);
                    return;
                }
                showCarForm(out, currentUser, null);
            } else if (path.equals("/edit")) {
                // Проверка доступа для редактирования
                if (!"MECHANIC".equals(userRole) && !"ADMIN".equals(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, req, userRole);
                    return;
                }
                String idParam = req.getParameter("id");
                if (idParam != null) {
                    Long carId = Long.parseLong(idParam);
                    Car car = carService.getCarById(carId);
                    if (car != null) {
                        showCarForm(out, currentUser, car);
                    } else {
                        HtmlUtil.renderErrorPage(out, req, "Ошибка", "Автомобиль не найден");
                    }
                } else {
                    HtmlUtil.renderErrorPage(out, req, "Ошибка", "Не указан ID автомобиля");
                }
            } else if (path.equals("/delete")) {
                // Проверка доступа для удаления
                if (!"MECHANIC".equals(userRole) && !"ADMIN".equals(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, req, userRole);
                    return;
                }
                String idParam = req.getParameter("id");
                if (idParam != null) {
                    Long carId = Long.parseLong(idParam);
                    deleteCar(carId, resp);
                    return;
                } else {
                    HtmlUtil.renderErrorPage(out, req, "Ошибка", "Не указан ID автомобиля");
                }
            } else {
                HtmlUtil.renderErrorPage(out, req, "Страница не найдена",
                        "Запрашиваемая страница не существует или была перемещена.");
            }
        } catch (NumberFormatException e) {
            HtmlUtil.renderErrorPage(out, req, "Ошибка формата", "Неверный формат ID");
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, req, "Ошибка сервера", e.getMessage());
        }
    }

    private void showCarsList(HttpServletRequest req, PrintWriter out, User user) {
        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Заголовок
        content.append("<div class='mb-30'>");
        content.append("<h1 class='page-title'>Автомобили</h1>");
        content.append("<p class='page-subtitle'>Управление автопарком такси</p>");
        content.append("</div>");

        // Получаем все автомобили
        List<Car> cars = carService.getAllCars();

        // Фильтруем если нужно
        String filter = req.getParameter("filter");
        List<Car> filteredCars = cars;

        // Показываем сообщения об успехе/ошибке
        String success = req.getParameter("success");
        String error = req.getParameter("error");

        if (success != null) {
            String message = "";
            switch (success) {
                case "created":
                    message = "Автомобиль успешно добавлен!";
                    break;
                case "updated":
                    message = "Автомобиль успешно обновлен!";
                    break;
                case "deleted":
                    message = "Автомобиль успешно удален!";
                    break;
            }
            content.append("<div class='alert alert-success'>").append(message).append("</div>");
        }

        if (error != null) {
            content.append("<div class='alert alert-danger'>Ошибка: ").append(error).append("</div>");
        }

        // Фильтрация и сообщения
        if ("active".equals(filter)) {
            filteredCars = carService.getActiveCars();
            content.append("<div class='alert alert-success'>Показаны только активные автомобили</div>");
        } else if ("repair".equals(filter)) {
            filteredCars = carService.getCarsInRepair();
            content.append("<div class='alert alert-warning'>Показаны автомобили в ремонте</div>");
        } else if ("available".equals(filter)) {
            filteredCars = carService.getAvailableCars();
            content.append("<div class='alert alert-info'>Показаны только доступные автомобили</div>");
        }

        // Кнопки действий
        content.append("<div class='action-buttons mb-30'>");
        if ("MECHANIC".equals(user.getUserType()) || "ADMIN".equals(user.getUserType())) {
            content.append("<a href='/cars/new' class='btn btn-success'> Добавить автомобиль</a>");
        }
        content.append("<a href='?filter=active' class='btn btn-secondary'>Активные</a>");
        content.append("<a href='?filter=available' class='btn btn-secondary'>Доступные</a>");
        content.append("<a href='?filter=repair' class='btn btn-secondary'>В ремонте</a>");
        content.append("<a href='?' class='btn btn-secondary'>Все автомобили</a>");
        content.append("</div>");

        // Таблица автомобилей
        content.append("<div class='card mb-30'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'>Список автомобилей</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        if (filteredCars.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<div class='empty-icon'>-</div>");
            content.append("<h3>Нет автомобилей</h3>");
            content.append("<p>Попробуйте изменить фильтры или добавить новый автомобиль</p>");
            if ("MECHANIC".equals(user.getUserType()) || "ADMIN".equals(user.getUserType())) {
                content.append("<a href='/cars/new' class='btn btn-success mt-20'>Добавить первый автомобиль</a>");
            }
            content.append("</div>");
        } else {
            content.append("<div class='table-container'>");
            content.append("<table>");
            content.append("<thead>");
            content.append("<tr>");
            content.append("<th>Марка/Модель</th>");
            content.append("<th>Гос. номер</th>");
            content.append("<th>VIN</th>");
            content.append("<th>Год</th>");
            content.append("<th>Пробег (км)</th>");
            content.append("<th>Статус</th>");
            content.append("<th>Действия</th>");
            content.append("</tr>");
            content.append("</thead>");
            content.append("<tbody>");

            for (Car car : filteredCars) {
                content.append("<tr>");
                content.append("<td><strong>").append(car.getBrand()).append(" ").append(car.getModel()).append("</strong></td>");
                content.append("<td>").append(car.getLicensePlate()).append("</td>");
                content.append("<td><small>").append(car.getVin() != null ? car.getVin() : "—").append("</small></td>");
                content.append("<td>").append(car.getYearOfManufacture() != null ? car.getYearOfManufacture() : "—").append("</td>");
                content.append("<td>").append(car.getMileageKm() != null ? car.getMileageKm() : "—").append("</td>");

                // Статус с бейджами
                content.append("<td>");
                content.append("<span class='badge ").append(Boolean.TRUE.equals(car.getIsActive()) ? "badge-success" : "badge-danger").append("'>");
                content.append(Boolean.TRUE.equals(car.getIsActive()) ? "Активен" : "Неактивен");
                content.append("</span>");

                if (Boolean.TRUE.equals(car.getInRepair())) {
                    content.append("<span class='badge badge-warning' style='margin-left: 5px;'>В ремонте</span>");
                }

                // Технический статус
                if (car.getTechnicalStatus() != null) {
                    String statusClass = "";
                    String statusText = "";
                    switch (car.getTechnicalStatus()) {
                        case OK:
                            statusClass = "badge-success";
                            statusText = "Исправен";
                            break;
                        case NEEDS_INSPECTION:
                            statusClass = "badge-warning";
                            statusText = "Нужен осмотр";
                            break;
                        case NEEDS_REPAIR:
                            statusClass = "badge-danger";
                            statusText = "Нужен ремонт";
                            break;
                        case UNKNOWN:
                            statusClass = "badge-secondary";
                            statusText = "Неизвестно";
                            break;
                    }
                    content.append("<span class='badge ").append(statusClass).append("' style='margin-left: 5px;'>").append(statusText).append("</span>");
                }
                content.append("</td>");

                // Кнопки действий
                content.append("<td>");
                content.append("<div class='action-buttons-small'>");
                content.append("<a href='/cars/edit?id=").append(car.getId())
                        .append("' class='btn btn-sm' title='Редактировать'>Ред.️</a>");

                if ("MECHANIC".equals(user.getUserType()) || "ADMIN".equals(user.getUserType())) {
                    content.append("<a href='/cars/delete?id=").append(car.getId())
                            .append("' class='btn btn-sm btn-danger' title='Удалить' onclick='return confirm(\"Удалить автомобиль?\");'>Удалить</a>");
                }
                content.append("</div>");
                content.append("</td>");
                content.append("</tr>");
            }

            content.append("</tbody>");
            content.append("</table>");
            content.append("</div>");
        }

        content.append("</div>");
        content.append("</div>");

        // Статистика в новом стиле
        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'> Статистика автопарка</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");
        content.append("<div class='stats-grid'>");

        long totalCars = carService.getAllCars().size();
        long activeCars = carService.getActiveCars().size();
        long availableCars = carService.getAvailableCars().size();
        long carsInRepair = carService.getCarsInRepair().size();

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(totalCars).append("</div>");
        content.append("<div class='stat-label'>Всего автомобилей</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(activeCars).append("</div>");
        content.append("<div class='stat-label'>Активных</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(availableCars).append("</div>");
        content.append("<div class='stat-label'>Доступно</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(carsInRepair).append("</div>");
        content.append("<div class='stat-label'>В ремонте</div>");
        content.append("</div>");

        content.append("</div>");
        content.append("</div>");
        content.append("</div>");

        // Рендерим полную страницу
        HtmlUtil.renderFullPage(out, req, "Автомобили", "cars", content.toString());
    }

    private void showCarForm(PrintWriter out, User currentUser, Car car) {
        String formTitle = car == null ? "Добавление автомобиля" : "Редактирование автомобиля";
        String submitAction = car == null ? "/cars/save" : "/cars/update";

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h2 class='card-title'>").append(formTitle).append("</h2>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        content.append("<form method='post' action='").append(submitAction).append("' class='form'>");
        if (car != null) {
            content.append("<input type='hidden' name='id' value='").append(car.getId()).append("'>");
        }

        content.append("<div class='grid grid-2'>");

        content.append("<div class='form-group'>");
        content.append("<label for='brand' class='form-label'>Марка <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='brand' name='brand' value='")
                .append(car != null ? car.getBrand() : "").append("' required placeholder='Например: Toyota'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='model' class='form-label'>Модель <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='model' name='model' value='")
                .append(car != null ? car.getModel() : "").append("' required placeholder='Например: Camry'>");
        content.append("</div>");

        content.append("</div>");

        content.append("<div class='grid grid-2'>");

        content.append("<div class='form-group'>");
        content.append("<label for='licensePlate' class='form-label'>Гос. номер <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='licensePlate' name='licensePlate' value='")
                .append(car != null ? car.getLicensePlate() : "").append("' required placeholder='Например: А123БВ77'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='vin' class='form-label'>VIN номер <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='vin' name='vin' value='")
                .append(car != null ? car.getVin() : "").append("' required placeholder='Например: JTDBR32E160023456'>");
        content.append("</div>");

        content.append("</div>");

        content.append("<div class='grid grid-3'>");

        content.append("<div class='form-group'>");
        content.append("<label for='yearOfManufacture' class='form-label'>Год выпуска</label>");
        content.append("<input type='number' class='form-control' id='yearOfManufacture' name='yearOfManufacture' value='")
                .append(car != null && car.getYearOfManufacture() != null ? car.getYearOfManufacture() : "2020")
                .append("' min='1900' max='2030'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='color' class='form-label'>Цвет</label>");
        content.append("<input type='text' class='form-control' id='color' name='color' value='")
                .append(car != null ? car.getColor() : "").append("' placeholder='Например: Черный'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='mileageKm' class='form-label'>Пробег (км)</label>");
        content.append("<input type='number' class='form-control' id='mileageKm' name='mileageKm' value='")
                .append(car != null && car.getMileageKm() != null ? car.getMileageKm() : "0")
                .append("' min='0'>");
        content.append("</div>");

        content.append("</div>");

        content.append("<div class='grid grid-2'>");

        content.append("<div class='form-group'>");
        content.append("<label for='isActive' class='form-label'>Статус</label>");
        content.append("<select class='form-control' id='isActive' name='isActive'>");
        content.append("<option value='true'")
                .append(car == null || Boolean.TRUE.equals(car.getIsActive()) ? " selected" : "")
                .append(">Активен</option>");
        content.append("<option value='false'")
                .append(car != null && Boolean.FALSE.equals(car.getIsActive()) ? " selected" : "")
                .append(">Неактивен</option>");
        content.append("</select>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='inRepair' class='form-label'>Состояние</label>");
        content.append("<select class='form-control' id='inRepair' name='inRepair'>");
        content.append("<option value='false'")
                .append(car == null || Boolean.FALSE.equals(car.getInRepair()) ? " selected" : "")
                .append(">Исправен</option>");
        content.append("<option value='true'")
                .append(car != null && Boolean.TRUE.equals(car.getInRepair()) ? " selected" : "")
                .append(">В ремонте</option>");
        content.append("</select>");
        content.append("</div>");

        content.append("</div>");

        content.append("<div class='form-actions'>");
        content.append("<button type='submit' class='btn btn-success'> Сохранить</button>");
        content.append("<a href='/cars' class='btn btn-danger'> Отмена</a>");
        content.append("</div>");

        content.append("</form>");
        content.append("</div>");
        content.append("</div>");

        HtmlUtil.renderFullPage(out, currentRequest, formTitle, "cars", content.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User currentUser = (User) req.getSession().getAttribute("user");
        if (currentUser == null) {
            resp.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();
        if (!"MECHANIC".equals(userRole) && !"ADMIN".equals(userRole)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
            return;
        }

        String path = req.getPathInfo();

        try {
            if (path == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Не указано действие");
                return;
            }

            switch (path) {
                case "/save":
                    createCar(req, resp, currentUser);
                    break;
                case "/update":
                    updateCar(req, resp, currentUser);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/cars?error=" + e.getMessage());
        }
    }

    private void createCar(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws IOException {
        try {
            Car car = new Car();
            fillCarFromRequest(car, req);

            boolean created = carService.createCar(car);

            if (created) {
                resp.sendRedirect("/cars?success=created");
            } else {
                resp.sendRedirect("/cars?error=Ошибка при создании автомобиля");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/cars?error=" + e.getMessage());
        }
    }

    private void updateCar(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws IOException {
        try {
            Long id = Long.parseLong(req.getParameter("id"));
            Car existingCar = carService.getCarById(id);

            if (existingCar == null) {
                resp.sendRedirect("/cars?error=Автомобиль не найден");
                return;
            }

            fillCarFromRequest(existingCar, req);
            carService.updateCar(existingCar);
            resp.sendRedirect("/cars?success=updated");

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/cars?error=" + e.getMessage());
        }
    }

    private void deleteCar(Long carId, HttpServletResponse resp) throws IOException {
        try {
            boolean deleted = carService.deleteCar(carId);
            if (deleted) {
                resp.sendRedirect("/cars?success=deleted");
            } else {
                resp.sendRedirect("/cars?error=Ошибка при удалении автомобиля");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/cars?error=" + e.getMessage());
        }
    }

    private void fillCarFromRequest(Car car, HttpServletRequest req) {
        car.setBrand(req.getParameter("brand"));
        car.setModel(req.getParameter("model"));
        car.setLicensePlate(req.getParameter("licensePlate"));
        car.setVin(req.getParameter("vin"));

        if (req.getParameter("yearOfManufacture") != null && !req.getParameter("yearOfManufacture").isEmpty()) {
            try {
                car.setYearOfManufacture(Integer.parseInt(req.getParameter("yearOfManufacture")));
            } catch (NumberFormatException e) {
                car.setYearOfManufacture(2020);
            }
        }

        car.setColor(req.getParameter("color"));

        if (req.getParameter("mileageKm") != null && !req.getParameter("mileageKm").isEmpty()) {
            try {
                car.setMileageKm(Integer.parseInt(req.getParameter("mileageKm")));
            } catch (NumberFormatException e) {
                car.setMileageKm(0);
            }
        }

        car.setIsActive("true".equals(req.getParameter("isActive")));
        car.setInRepair("true".equals(req.getParameter("inRepair")));
    }
}


