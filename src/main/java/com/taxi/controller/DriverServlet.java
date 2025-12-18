package com.taxi.controller;

import com.taxi.entity.Car;
import com.taxi.entity.Driver;
import com.taxi.entity.User;
import com.taxi.service.DriverService;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.List;

@WebServlet("/drivers/*")
public class DriverServlet extends HttpServlet {

    private DriverService driverService = new DriverService();
    private HttpServletRequest currentRequest;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        currentRequest = request;

        // Проверка прав
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();

        // ДЕБАГ: выводим информацию о пользователе
        System.out.println(" ДЕБАГ ДОСТУПА К /drivers ===");
        System.out.println("Пользователь: " + currentUser.getLogin() + " (" + currentUser.getFullName() + ")");
        System.out.println("Роль из БД: '" + userRole + "'");
        System.out.println("isActive: " + currentUser.getIsActive());

        // Проверка доступа
        boolean hasAccess = "ADMIN".equals(userRole) || "MECHANIC".equals(userRole) || "DOCTOR".equals(userRole);
        System.out.println("Доступ разрешен: " + hasAccess);
//        System.out.println("=======================");

        if (!hasAccess) {
            // Устанавливаем ContentType и выводим простую страницу ошибки
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>Доступ запрещен | Такси-сервис</title>");
            out.println("    <meta charset='UTF-8'>");
            out.println("    <style>");
            out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
            out.println("        body { ");
            out.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
            out.println("            background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); ");
            out.println("            color: #e0e0e0; ");
            out.println("            min-height: 100vh; ");
            out.println("            display: flex; ");
            out.println("            align-items: center; ");
            out.println("            justify-content: center; ");
            out.println("            padding: 20px; ");
            out.println("        }");
            out.println("        .error-container { ");
            out.println("            max-width: 500px; ");
            out.println("            width: 100%; ");
            out.println("            background: rgba(30, 30, 30, 0.9); ");
            out.println("            border-radius: 15px; ");
            out.println("            padding: 40px; ");
            out.println("            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); ");
            out.println("            text-align: center; ");
            out.println("        }");
            out.println("        .error-icon { ");
            out.println("            font-size: 4em; ");
            out.println("            margin-bottom: 20px; ");
            out.println("        }");
            out.println("        h1 { ");
            out.println("            color: #ff4444; ");
            out.println("            margin-bottom: 15px; ");
            out.println("            font-size: 2em; ");
            out.println("        }");
            out.println("        p { ");
            out.println("            margin-bottom: 10px; ");
            out.println("            line-height: 1.6; ");
            out.println("        }");
            out.println("        .role-info { ");
            out.println("            background: rgba(255, 255, 255, 0.05); ");
            out.println("            padding: 15px; ");
            out.println("            border-radius: 8px; ");
            out.println("            margin: 20px 0; ");
            out.println("            border-left: 4px solid #ff9800; ");
            out.println("        }");
            out.println("        .required-roles { ");
            out.println("            background: rgba(255, 255, 255, 0.05); ");
            out.println("            padding: 15px; ");
            out.println("            border-radius: 8px; ");
            out.println("            margin: 20px 0; ");
            out.println("            border-left: 4px solid #4caf50; ");
            out.println("        }");
            out.println("        .btn { ");
            out.println("            display: inline-block; ");
            out.println("            padding: 12px 30px; ");
            out.println("            margin: 10px; ");
            out.println("            text-decoration: none; ");
            out.println("            border-radius: 25px; ");
            out.println("            font-weight: 500; ");
            out.println("            transition: all 0.3s ease; ");
            out.println("            border: none; ");
            out.println("            cursor: pointer; ");
            out.println("            font-size: 1em; ");
            out.println("        }");
            out.println("        .btn-primary { ");
            out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
            out.println("            color: white; ");
            out.println("        }");
            out.println("        .btn-primary:hover { ");
            out.println("            background: linear-gradient(135deg, #1976d2 0%, #0d47a1 100%); ");
            out.println("            transform: scale(1.05); ");
            out.println("        }");
            out.println("        .btn-success { ");
            out.println("            background: linear-gradient(135deg, #4caf50 0%, #2e7d32 100%); ");
            out.println("            color: white; ");
            out.println("        }");
            out.println("        .btn-success:hover { ");
            out.println("            background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%); ");
            out.println("            transform: scale(1.05); ");
            out.println("        }");
            out.println("        strong { ");
            out.println("            color: #ffffff; ");
            out.println("        }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='error-container'>");
            out.println("        <div class='error-icon'>🚫</div>");
            out.println("        <h1>Доступ запрещен</h1>");
            out.println("        <p>У вас недостаточно прав для доступа к этой странице.</p>");
            out.println("        ");
            out.println("        <div class='role-info'>");
            out.println("            <p>Ваша роль: <strong>" + userRole + "</strong></p>");
            out.println("            <p>Статус: <strong>" + (currentUser.getIsActive() ? "✅ Активен" : "❌ Неактивен") + "</strong></p>");
            out.println("        </div>");
            out.println("        ");
            out.println("        <div class='required-roles'>");
            out.println("            <p>Требуемые роли для доступа:</p>");
            out.println("            <p><strong> Администратор (ADMIN)</strong></p>");
            out.println("            <p><strong> Механик (MECHANIC)</strong></p>");
            out.println("            <p><strong> Врач (DOCTOR)</strong></p>");
            out.println("        </div>");
            out.println("        ");
            out.println("        <div class='action-buttons'>");

            // Определяем ссылку на панель по роли
            String dashboardLink = "/";
            switch (userRole) {
                case "ADMIN":
                    dashboardLink = "/admin";
                    break;
                case "MECHANIC":
                    dashboardLink = "/mechanic";
                    break;
                case "DOCTOR":
                    dashboardLink = "/doctor";
                    break;
                case "OPERATOR":
                    dashboardLink = "/dispatcher";
                    break;
                case "DRIVER":
                    dashboardLink = "/driver-panel";
                    break;
            }

            out.println("            <a href='/' class='btn btn-primary'> На главную</a>");
            if (!"/".equals(dashboardLink)) {
                out.println("            <a href='" + dashboardLink + "' class='btn btn-success'> Моя панель</a>");
            }
            out.println("        </div>");
            out.println("        ");
            out.println("        <div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #333;'>");
            out.println("            <p style='font-size: 0.9em; color: #888;'>Если вы считаете, что это ошибка:</p>");
            out.println("            <a href='/logout' style='color: #ff9800; text-decoration: none;'> Выйти и войти под другой учетной записью</a>");
            out.println("        </div>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
            return;
        }

        // Если доступ есть, продолжаем обычную обработку
        String path = request.getPathInfo();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if (path == null || path.equals("/") || path.isEmpty()) {
                showDriversList(request, out, currentUser);
            } else if (path.equals("/new")) {
                showAddDriverForm(out, currentUser);
            } else if (path.equals("/edit")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long driverId = Long.parseLong(idParam);
                    showEditDriverForm(driverId, out, currentUser);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID водителя");
                }
            } else if (path.equals("/assign-car")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long driverId = Long.parseLong(idParam);
                    showAssignCarForm(driverId, out, currentUser);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID водителя");
                }
            } else if (path.equals("/unassign-car")) {
                // Снятие автомобиля
                String driverIdParam = request.getParameter("driverId");
                if (driverIdParam != null) {
                    Long driverId = Long.parseLong(driverIdParam);
                    driverService.unassignCarFromDriver(driverId);
                    response.sendRedirect("/drivers");
                    return;
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID водителя");
                }
            } else if (path.equals("/delete")) {
                // Удаление водителя
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long driverId = Long.parseLong(idParam);
                    driverService.deleteDriver(driverId);
                    response.sendRedirect("/drivers?success=deleted");
                    return;
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID водителя");
                }
            } else {
                HtmlUtil.renderErrorPage(out, request, "Страница не найдена",
                        "Запрашиваемая страница не существует или была перемещена.");
            }
        } catch (NumberFormatException e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка формата", "Неверный формат ID");
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка сервера", e.getMessage());
        }
    }

    private void showDriversList(HttpServletRequest request, PrintWriter out, User currentUser) {
        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Показываем сообщения об успехе/ошибке
        String success = request.getParameter("success");
        String error = request.getParameter("error");

        if (success != null) {
            String message = switch (success) {
                case "created" -> " Водитель успешно создан!";
                case "updated" -> " Водитель успешно обновлен!";
                case "deleted" -> " Водитель успешно удален!";
                case "car_assigned" -> " Автомобиль успешно назначен!";
                case "car_unassigned" -> " Автомобиль успешно снят!";
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

        // Заголовок
        content.append("<div class='mb-30'>");
        content.append("<h1 class='page-title'>Водители</h1>");
        content.append("<p class='page-subtitle'>Управление водителями такси</p>");
        content.append("</div>");

        // Получаем фильтр
        String filter = request.getParameter("filter");
        List<Driver> drivers = driverService.getAllDrivers();

        // Применяем фильтр
        if (filter != null) {
            switch (filter) {
                case "active":
                    drivers = drivers.stream().filter(Driver::getIsActive).toList();
                    content.append("<div class='alert alert-success'>Внимание! Показаны только активные водители</div>");
                    break;
                case "inactive":
                    drivers = drivers.stream().filter(d -> !d.getIsActive()).toList();
                    content.append("<div class='alert alert-warning'>Внимание! Показаны только неактивные водители</div>");
                    break;
                case "with-car":
                    drivers = drivers.stream().filter(d -> d.getCurrentCar() != null).toList();
                    content.append("<div class='alert alert-info'>Внимание! Показаны водители с автомобилем</div>");
                    break;
                case "without-car":
                    drivers = drivers.stream().filter(d -> d.getCurrentCar() == null).toList();
                    content.append("<div class='alert alert-info'>Внимание! Показаны водители без автомобиля</div>");
                    break;
                case "with-user":
                    drivers = drivers.stream().filter(d -> d.getUser() != null).toList();
                    content.append("<div class='alert alert-info'>Внимание! Показаны водители с учетной записью</div>");
                    break;
                case "without-user":
                    drivers = drivers.stream().filter(d -> d.getUser() == null).toList();
                    content.append("<div class='alert alert-info'>Внимание! Показаны водители без учетной записи</div>");
                    break;
            }
        }

        // Кнопки действий
        content.append("<div class='action-buttons mb-30'>");
        content.append("<a href='/drivers/new' class='btn btn-success'> Добавить водителя</a>");
        content.append("<a href='?filter=with-car' class='btn btn-secondary'> С автомобилем</a>");
        content.append("<a href='?filter=without-car' class='btn btn-secondary'> Без автомобиля</a>");
        content.append("<a href='?filter=with-user' class='btn btn-secondary'> С учетной записью</a>");
        content.append("<a href='?filter=without-user' class='btn btn-secondary'> Без учетной записи</a>");
        content.append("<a href='?' class='btn btn-secondary'> Все водители</a>");
        content.append("</div>");

        // Таблица водителей
        content.append("<div class='card mb-30'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'>Список водителей (всего: " + drivers.size() + ")</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        if (drivers.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<div class='empty-icon'>-</div>");
            content.append("<h3>Нет данных о водителях</h3>");
            content.append("<p>Попробуйте изменить фильтры или добавить нового водителя</p>");
            content.append("<a href='/drivers/new' class='btn btn-success mt-20'>Добавить первого водителя</a>");
            content.append("</div>");
        } else {
            content.append("<div class='table-container'>");
            content.append("<table>");
            content.append("<thead>");
            content.append("<tr>");
            content.append("<th>ID</th>");
            content.append("<th>ФИО</th>");
            content.append("<th>В/у</th>");
            content.append("<th>Телефон</th>");
            content.append("<th>Пользователь</th>");
            content.append("<th>Автомобиль</th>");
            content.append("<th>Мед. статус</th>");
//            content.append("<th>Активность</th>");
            content.append("<th>Действия</th>");
            content.append("</tr>");
            content.append("</thead>");
            content.append("<tbody>");

            for (Driver driver : drivers) {
                // Информация о пользователе
                String userInfo = "Нет";
                if (driver.getUser() != null) {
                    userInfo = "<strong>" + driver.getUser().getFullName() + "</strong><br>" +
                            "<small>" + driver.getUser().getLogin() + "</small>";
                }

                // Информация об автомобиле
                Car currentCar = driver.getCurrentCar();
                String carInfo = "Нет";
                if (currentCar != null) {
                    carInfo = "<strong>" + currentCar.getLicensePlate() + "</strong><br>" +
                            "<small>" + currentCar.getModel() + "</small>";
                }

                String medStatus = driver.getMedicalStatus() != null ?
                        driver.getMedicalStatus().name() : "НЕИЗВЕСТНО";
                String medStatusColor = "badge-warning";
                if ("PASSED".equals(medStatus)) medStatusColor = "badge-success";
                if ("FAILED".equals(medStatus)) medStatusColor = "badge-danger";

                content.append("<tr>");
                content.append("<td>").append(driver.getId()).append("</td>");
                content.append("<td><strong>").append(driver.getFullName() != null ? driver.getFullName() : "—").append("</strong></td>");
                content.append("<td>").append(driver.getLicenseNumber() != null ? driver.getLicenseNumber() : "—").append("</td>");
                content.append("<td>").append(driver.getPhone() != null ? driver.getPhone() : "—").append("</td>");
                content.append("<td>").append(userInfo).append("</td>");
                content.append("<td>").append(carInfo).append("</td>");
                content.append("<td><span class='badge ").append(medStatusColor).append("'>").append(medStatus).append("</span></td>");
//                content.append("<td>");
//                content.append("<span class='badge ").append(driver.getIsActive() ? "badge-success" : "badge-danger").append("'>");
//                content.append(driver.getIsActive() ? "Активен" : "Неактивен");
                content.append("</span>");
                content.append("</td>");

                // Кнопки действий
                content.append("<td>");
                content.append("<div class='action-buttons-small'>");
                content.append("<a href='/drivers/edit?id=").append(driver.getId())
                        .append("' class='btn btn-sm btn-primary' title='Редактировать'>Ред.️</a>");
                content.append("<a href='/drivers/assign-car?id=").append(driver.getId())
                        .append("' class='btn btn-sm btn-info' title='Назначить авто'>Назначить а/м</a>");
                content.append("<a href='/drivers/delete?id=").append(driver.getId())
                        .append("' class='btn btn-sm btn-danger' onclick='return confirm(\"Удалить водителя "
                                + driver.getFullName() + "?\\nВсе связанные данные (медосмотры, путевые листы) будут удалены!\");' title='Удалить'>Удалить</a>");
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

        // Статистика (только если есть водители)
        if (!drivers.isEmpty()) {
            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h3 class='card-title'> Статистика по водителям</h3>");
            content.append("</div>");
            content.append("<div class='card-body'>");
            content.append("<div class='stats-grid'>");

            long totalDrivers = drivers.size();
            long activeDrivers = drivers.stream().filter(Driver::getIsActive).count();
            long driversWithUser = drivers.stream().filter(d -> d.getUser() != null).count();
            long driversWithCar = drivers.stream().filter(d -> d.getCurrentCar() != null).count();
            long driversWithMedicalPassed = drivers.stream().filter(d -> d.getMedicalStatus() != null && d.getMedicalStatus().name().equals("PASSED")).count();

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(totalDrivers).append("</div>");
            content.append("<div class='stat-label'>Всего водителей</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(activeDrivers).append("</div>");
            content.append("<div class='stat-label'>Активных</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(driversWithUser).append("</div>");
            content.append("<div class='stat-label'>С учетной записью</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(driversWithCar).append("</div>");
            content.append("<div class='stat-label'>С автомобилем</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(driversWithMedicalPassed).append("</div>");
            content.append("<div class='stat-label'>С медосмотром</div>");
            content.append("</div>");

            content.append("</div>");
            content.append("</div>");
            content.append("</div>");
        }

        // Рендерим полную страницу
        HtmlUtil.renderFullPage(out, currentRequest, "Водители", "drivers", content.toString());
    }

    private void showAddDriverForm(PrintWriter out, User currentUser) {
        // Получаем доступных пользователей
        List<User> availableUsers = driverService.getAvailableUsersForDriver();

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h2 class='card-title'>➕ Добавить нового водителя</h2>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        content.append("<form method='POST' action='/drivers/save' class='form'>");
        content.append("<div class='form-group'>");
        content.append("<label for='fullName' class='form-label'>Полное имя: <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='fullName' name='fullName' required placeholder='Например: Иванов Иван Иванович'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='licenseNumber' class='form-label'>Номер в/у: <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='licenseNumber' name='licenseNumber' required placeholder='Например: АВ1234567'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='phone' class='form-label'>Телефон:</label>");
        content.append("<input type='tel' class='form-control' id='phone' name='phone' placeholder='+7 (999) 123-45-67'>");
        content.append("</div>");

        // ДОБАВЛЯЕМ ВЫБОР ПОЛЬЗОВАТЕЛЯ
        content.append("<div class='form-group'>");
        content.append("<label for='userId' class='form-label'>Привязать к учетной записи:</label>");
        content.append("<select class='form-control' id='userId' name='userId'>");
        content.append("<option value=''>-- Без привязки --</option>");

        for (User user : availableUsers) {
            content.append("<option value='").append(user.getId()).append("'>")
                    .append(user.getFullName()).append(" (Логин: ").append(user.getLogin()).append(")")
                    .append("</option>");
        }

        content.append("</select>");
        content.append("<p class='form-hint'>Выберите существующего пользователя с ролью DRIVER</p>");

        if (availableUsers.isEmpty()) {
            content.append("<div class='alert alert-warning mt-10'>");
            content.append("Нет доступных пользователей с ролью DRIVER. ");
            content.append("<a href='/admin/users/new' class='alert-link'>Создать нового пользователя</a>");
            content.append("</div>");
        }
        content.append("</div>");

        content.append("<div class='form-check mb-20'>");
        content.append("<input type='checkbox' class='form-check-input' id='isActive' name='isActive' checked>");
        content.append("<label for='isActive' class='form-check-label'>Активен в системе</label>");
        content.append("</div>");

        content.append("<div class='form-actions'>");
        content.append("<button type='submit' class='btn btn-success'> Сохранить водителя</button>");
        content.append("<a href='/drivers' class='btn btn-danger'> Отмена</a>");
        content.append("</div>");
        content.append("</form>");

        content.append("</div>");
        content.append("</div>");

        HtmlUtil.renderFullPage(out, currentRequest, "Добавить водителя", "drivers", content.toString());
    }

    private void showEditDriverForm(Long driverId, PrintWriter out, User currentUser) {
        try {
            Driver driver = driverService.getDriverById(driverId);
            if (driver == null) {
                HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Водитель не найден");
                return;
            }

            // Получаем доступных пользователей (роль DRIVER без водителя)
            List<User> availableUsers = driverService.getAvailableUsersForDriver();
            // Добавляем текущего пользователя, если он есть (для отображения в списке)
            if (driver.getUser() != null && !availableUsers.contains(driver.getUser())) {
                availableUsers.add(driver.getUser());
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'>✏ Редактировать водителя</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            content.append("<div class='alert alert-info mb-30'>");
            content.append("<p><strong>").append(driver.getFullName()).append("</strong> - ").append(driver.getLicenseNumber()).append("</p>");
            Car currentCar = driver.getCurrentCar();
            if (currentCar != null) {
                content.append("<p style='margin-top: 5px;'><small> Текущий автомобиль: ")
                        .append(currentCar.getBrand()).append(" ").append(currentCar.getModel())
                        .append(" (").append(currentCar.getLicensePlate()).append(")</small></p>");
            }
            content.append("</div>");

            content.append("<form method='POST' action='/drivers/update' class='form'>");
            content.append("<input type='hidden' name='id' value='").append(driver.getId()).append("'>");

            content.append("<div class='form-group'>");
            content.append("<label for='fullName' class='form-label'>Полное имя: <span class='required'>*</span></label>");
            content.append("<input type='text' class='form-control' id='fullName' name='fullName' value='")
                    .append(driver.getFullName() != null ? driver.getFullName() : "").append("' required>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='licenseNumber' class='form-label'>Номер в/у: <span class='required'>*</span></label>");
            content.append("<input type='text' class='form-control' id='licenseNumber' name='licenseNumber' value='")
                    .append(driver.getLicenseNumber() != null ? driver.getLicenseNumber() : "").append("' required>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='phone' class='form-label'>Телефон:</label>");
            content.append("<input type='tel' class='form-control' id='phone' name='phone' value='")
                    .append(driver.getPhone() != null ? driver.getPhone() : "").append("'>");
            content.append("</div>");

            // ДОБАВЛЯЕМ ВЫБОР ПОЛЬЗОВАТЕЛЯ
            content.append("<div class='form-group'>");
            content.append("<label for='userId' class='form-label'>Привязанная учетная запись:</label>");
            content.append("<select class='form-control' id='userId' name='userId'>");
            content.append("<option value=''>-- Без привязки --</option>");

            Long currentUserId = driver.getUser() != null ? driver.getUser().getId() : null;
            for (User user : availableUsers) {
                String selected = (currentUserId != null && currentUserId.equals(user.getId())) ? "selected" : "";
                content.append("<option value='").append(user.getId()).append("' ").append(selected).append(">")
                        .append(user.getFullName()).append(" (Логин: ").append(user.getLogin()).append(")")
                        .append("</option>");
            }

            content.append("</select>");
            content.append("<p class='form-hint'>Можно привязать или отвязать учетную запись</p>");

            if (driver.getUser() != null) {
                content.append("<div class='alert alert-success mt-10'>")
                        .append("Сейчас привязан: <strong>").append(driver.getUser().getFullName())
                        .append("</strong> (Логин: ").append(driver.getUser().getLogin()).append(")")
                        .append("</div>");
            }
            content.append("</div>");

            content.append("<div class='form-check mb-20'>");
            content.append("<input type='checkbox' class='form-check-input' id='isActive' name='isActive' ")
                    .append(driver.getIsActive() ? "checked" : "").append(">");
            content.append("<label for='isActive' class='form-check-label'>Активен в системе</label>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-success'> Сохранить изменения</button>");
            content.append("<a href='/drivers' class='btn btn-danger'> Отмена</a>");
            content.append("</div>");
            content.append("</form>");

            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Редактировать водителя", "drivers", content.toString());

        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Ошибка при загрузке данных водителя: " + e.getMessage());
        }
    }

    private void showAssignCarForm(Long driverId, PrintWriter out, User currentUser) {
        try {
            Driver driver = driverService.getDriverById(driverId);
            if (driver == null) {
                HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Водитель не найден");
                return;
            }

            List<Car> allCars = driverService.getAvailableCars();

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'> Назначить автомобиль водителю</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            content.append("<div class='alert alert-info mb-30'>");
            content.append("<p><strong>Водитель:</strong> ").append(driver.getFullName()).append("</p>");
            content.append("<p><strong>Текущий автомобиль:</strong> ")
                    .append(driver.getCurrentCar() != null ?
                            driver.getCurrentCar().getLicensePlate() + " (" + driver.getCurrentCar().getModel() + ")" :
                            " Не назначен")
                    .append("</p>");
            content.append("</div>");

            content.append("<form method='post' action='/drivers/assign-car' class='form'>");
            content.append("<input type='hidden' name='driverId' value='").append(driver.getId()).append("'>");

            content.append("<div class='form-group'>");
            content.append("<label for='carId' class='form-label'>Выберите автомобиль</label>");
            content.append("<select class='form-control' id='carId' name='carId'>");
            content.append("<option value=''>-- Снять автомобиль --</option>");

            for (Car car : allCars) {
                String status = "";
                String disabled = "";

                if (!car.isOperational()) {
                    status = " (⚠ Требуется техосмотр)";
                    disabled = "disabled";
                } else if (car.getCurrentDriver() != null &&
                        !car.getCurrentDriver().getId().equals(driverId)) {
                    status = " ( Занят: " + car.getCurrentDriver().getFullName() + ")";
                    disabled = "disabled";
                } else if (car.getCurrentDriver() != null &&
                        car.getCurrentDriver().getId().equals(driverId)) {
                    status = " ( Текущий)";
                }

                content.append("<option value='").append(car.getId()).append("' ").append(disabled).append(">")
                        .append(car.getLicensePlate()).append(" - ").append(car.getModel())
                        .append(" (").append(car.getMileageKm() != null ? car.getMileageKm() : "0").append(" км)")
                        .append(status).append("</option>");
            }
            content.append("</select>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-success'> Сохранить</button>");
            content.append("<a href='/drivers' class='btn btn-danger'> Отмена</a>");
            content.append("</div>");

            content.append("<div class='form-hint mt-20'>");
            content.append("<p>ℹ️ Можно назначить только автомобили:</p>");
            content.append("<ul>");
            content.append("<li>С пройденным техосмотром</li>");
            content.append("<li>Не занятые другими водителями</li>");
            content.append("</ul>");
            content.append("</div>");

            content.append("</form>");
            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Назначить автомобиль", "drivers", content.toString());

        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Ошибка при загрузке данных: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        currentRequest = request;

        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();
        if (!"ADMIN".equals(userRole) && !"MECHANIC".equals(userRole) && !"DOCTOR".equals(userRole)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
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
                    saveDriver(request, response, currentUser);
                    break;
                case "/update":
                    updateDriver(request, response, currentUser);
                    break;
                case "/assign":
                    assignCar(request, response, currentUser);
                    break;
                case "/unassign":
                    unassignCar(request, response, currentUser);
                    break;
                case "/assign-car":
                    handleAssignCar(request, response, currentUser);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/drivers?error=" + e.getMessage());
        }
    }

    private void handleAssignCar(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        Long driverId = Long.parseLong(request.getParameter("driverId"));
        String carIdParam = request.getParameter("carId");

        try {
            if (carIdParam == null || carIdParam.isEmpty()) {
                driverService.unassignCarFromDriver(driverId);
                response.sendRedirect("/drivers?success=car_unassigned");
            } else {
                Long carId = Long.parseLong(carIdParam);
                driverService.assignCarToDriver(driverId, carId);
                response.sendRedirect("/drivers?success=car_assigned");
            }
        } catch (Exception e) {
            response.sendRedirect("/drivers?error=" + e.getMessage());
        }
    }

    private void saveDriver(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            String fullName = request.getParameter("fullName");
            String licenseNumber = request.getParameter("licenseNumber").trim();
            String phone = request.getParameter("phone");
            boolean isActive = request.getParameter("isActive") != null;
            String userIdParam = request.getParameter("userId");

            // ПРОВЕРКА НА УНИКАЛЬНОСТЬ НОМЕРА ПРАВ
            Driver existingDriver = driverService.findDriverByLicenseNumber(licenseNumber);
            if (existingDriver != null) {
                String errorMsg = "Водитель с номером прав '" + licenseNumber + "' уже существует (ID: " +
                        existingDriver.getId() + ", ФИО: " + existingDriver.getFullName() + ")";
                System.out.println(" " + errorMsg);
                response.sendRedirect("/drivers?error=" + URLEncoder.encode(errorMsg, "UTF-8"));
                return;
            }

            Driver driver = new Driver();
            driver.setFullName(fullName);
            driver.setLicenseNumber(licenseNumber);
            driver.setPhone(phone);
            driver.setIsActive(isActive);

            System.out.println(" Создаем водителя: " + fullName + " (права: " + licenseNumber + ")");

            // Привязываем пользователя если выбран
            if (userIdParam != null && !userIdParam.isEmpty()) {
                Long userId = Long.parseLong(userIdParam);
                driverService.saveDriverWithUser(driver, userId);
            } else {
                driverService.createDriver(driver);
            }

            System.out.println(" Водитель успешно создан, перенаправляем на список");
            response.sendRedirect("/drivers?success=created");

        } catch (Exception e) {
            System.err.println(" Ошибка при создании водителя: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/drivers?error=" + URLEncoder.encode("Ошибка при создании: " + e.getMessage(), "UTF-8"));
        }
    }

    private void updateDriver(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Long driverId = Long.parseLong(request.getParameter("id"));
            String fullName = request.getParameter("fullName");
            String licenseNumber = request.getParameter("licenseNumber");
            String phone = request.getParameter("phone");
            boolean isActive = request.getParameter("isActive") != null;
            String userIdParam = request.getParameter("userId");

            Driver existingDriver = driverService.getDriverById(driverId);
            if (existingDriver == null) {
                response.sendRedirect("/drivers?error=Водитель не найден");
                return;
            }

            existingDriver.setFullName(fullName);
            existingDriver.setLicenseNumber(licenseNumber);
            existingDriver.setPhone(phone);
            existingDriver.setIsActive(isActive);

            // Обновляем привязку к пользователю
            if (userIdParam != null && !userIdParam.isEmpty()) {
                Long userId = Long.parseLong(userIdParam);
                driverService.updateDriverWithUser(existingDriver, userId);
            } else {
                // Отвязать пользователя
                existingDriver.setUser(null);
                driverService.updateDriver(existingDriver);
            }

            response.sendRedirect("/drivers?success=updated");

        } catch (Exception e) {
            response.sendRedirect("/drivers?error=" + e.getMessage());
        }
    }

    private void assignCar(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Long driverId = Long.parseLong(request.getParameter("driverId"));
            Long carId = Long.parseLong(request.getParameter("carId"));

            driverService.assignCarToDriver(driverId, carId);
            response.sendRedirect("/drivers?success=car_assigned");
        } catch (Exception e) {
            response.sendRedirect("/drivers?error=" + e.getMessage());
        }
    }

    private void unassignCar(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Long driverId = Long.parseLong(request.getParameter("driverId"));
            driverService.unassignCarFromDriver(driverId);
            response.sendRedirect("/drivers?success=car_unassigned");
        } catch (Exception e) {
            response.sendRedirect("/drivers?error=" + e.getMessage());
        }
    }
}


