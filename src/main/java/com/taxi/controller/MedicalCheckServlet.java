package com.taxi.controller;

import com.taxi.entity.Driver;
import com.taxi.entity.MedicalCheck;
import com.taxi.entity.User;
import com.taxi.service.MedicalCheckService;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/medical-checks/*")
public class MedicalCheckServlet extends HttpServlet {

    private MedicalCheckService medicalCheckService = new MedicalCheckService();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private HttpServletRequest currentRequest; // Для доступа в методах

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        currentRequest = request; // Сохраняем request

        // === ПРОВЕРКА АВТОРИЗАЦИИ И ПРАВ ===
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        // Проверяем права доступа к странице медосмотров
        String userRole = currentUser.getUserType();
        if (!"ADMIN".equals(userRole) && !"DOCTOR".equals(userRole)) {
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            HtmlUtil.renderAccessDeniedPage(out, request, userRole);
            return;
        }
        // === КОНЕЦ ПРОВЕРКИ ПРАВ ===

        String path = request.getPathInfo();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if (path == null || path.equals("/") || path.isEmpty()) {
                // Основной список медосмотров с фильтрами
                showMedicalChecksList(request, out, currentUser);
            } else if (path.equals("/new")) {
                // Форма добавления
                showCreateForm(out, currentUser);
            } else if (path.equals("/edit")) {
                // Форма редактирования
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long checkId = Long.parseLong(idParam);
                    showEditForm(checkId, out, currentUser);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID медосмотра");
                }
            } else if (path.equals("/delete")) {
                // Удаление
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long checkId = Long.parseLong(idParam);
                    medicalCheckService.deleteMedicalCheck(checkId);
                    response.sendRedirect("/medical-checks?success=deleted");
                    return;
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID медосмотра");
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

    private void showMedicalChecksList(HttpServletRequest request, PrintWriter out, User currentUser) {
        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Заголовок
        content.append("<div class='mb-30'>");
        content.append("<h1 class='page-title'>Медосмотры</h1>");
        content.append("<p class='page-subtitle'>Управление медицинскими осмотрами водителей</p>");
        content.append("</div>");

        // Получаем параметры фильтрации
        String driverFilter = request.getParameter("driver");
        String dateFilter = request.getParameter("date");
        String statusFilter = request.getParameter("status");
        String shiftFilter = request.getParameter("shift");
        String search = request.getParameter("search");

        // Получаем отфильтрованный список медосмотров
        List<MedicalCheck> medicalChecks = getFilteredMedicalChecks(
                driverFilter, dateFilter, statusFilter, shiftFilter, search);

        // Получаем данные для фильтров
        List<Driver> allDrivers = medicalCheckService.getAllDrivers();

        // Показываем сообщения об успехе/ошибке
        String success = request.getParameter("success");
        String error = request.getParameter("error");

        if (success != null) {
            String message = "";
            switch (success) {
                case "created":
                    message = " Медосмотр успешно добавлен!";
                    break;
                case "updated":
                    message = " Медосмотр успешно обновлен!";
                    break;
                case "deleted":
                    message = " Медосмотр успешно удален!";
                    break;
            }
            content.append("<div class='alert alert-success'>").append(message).append("</div>");
        }

        if (error != null) {
            content.append("<div class='alert alert-danger'>Ошибка: ").append(error).append("</div>");
        }

        // Кнопка добавления
        content.append("<div class='action-buttons mb-30'>");
        content.append("<a href='/medical-checks/new' class='btn btn-success'> Добавить медосмотр</a>");
        content.append("</div>");

        // Блок фильтров
        content.append("<div class='card mb-30'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'> Фильтры</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");
        content.append("<form method='get' action='/medical-checks' class='filter-form'>");

        // Поиск
        content.append("<div class='form-group'>");
        content.append("<label for='search' class='form-label'>Поиск</label>");
        content.append("<input type='text' class='form-control' id='search' name='search' placeholder='ФИО, врач, примечания...' value='")
                .append(search != null ? search : "").append("'>");
        content.append("</div>");

        // Фильтр по водителю
        content.append("<div class='form-group'>");
        content.append("<label for='driver' class='form-label'>Водитель</label>");
        content.append("<select class='form-control' id='driver' name='driver'>");
        content.append("<option value=''>Все водители</option>");
        for (Driver driver : allDrivers) {
            boolean selected = driverFilter != null && driverFilter.equals(driver.getId().toString());
            content.append("<option value='").append(driver.getId()).append("' ")
                    .append(selected ? "selected" : "").append(">")
                    .append(driver.getFullName()).append(" (").append(driver.getLicenseNumber()).append(")</option>");
        }
        content.append("</select>");
        content.append("</div>");

        // Фильтр по дате
        content.append("<div class='form-group'>");
        content.append("<label for='date' class='form-label'>Дата осмотра</label>");
        content.append("<input type='date' class='form-control' id='date' name='date' value='")
                .append(dateFilter != null ? dateFilter : "").append("'>");
        content.append("</div>");

        // Фильтр по статусу
        content.append("<div class='form-group'>");
        content.append("<label for='status' class='form-label'>Статус</label>");
        content.append("<select class='form-control' id='status' name='status'>");
        content.append("<option value=''>Все статусы</option>");
        content.append("<option value='passed' ").append("passed".equals(statusFilter) ? "selected" : "").append("> Допущен</option>");
        content.append("<option value='failed' ").append("failed".equals(statusFilter) ? "selected" : "").append("> Не допущен</option>");
        content.append("</select>");
        content.append("</div>");

        // Фильтр по смене
        content.append("<div class='form-group'>");
        content.append("<label for='shift' class='form-label'>Смена открыта</label>");
        content.append("<select class='form-control' id='shift' name='shift'>");
        content.append("<option value=''>Все</option>");
        content.append("<option value='yes' ").append("yes".equals(shiftFilter) ? "selected" : "").append(">Да</option>");
        content.append("<option value='no' ").append("no".equals(shiftFilter) ? "selected" : "").append(">Нет</option>");
        content.append("</select>");
        content.append("</div>");

        // Кнопки фильтрации
        content.append("<div class='form-actions'>");
        content.append("<button type='submit' class='btn btn-primary'>Применить</button>");
        content.append("<a href='/medical-checks' class='btn btn-secondary'>Сбросить</a>");
        content.append("</div>");

        content.append("</form>");
        content.append("</div>");
        content.append("</div>");

        // Таблица медосмотров (СОХРАНИЛ ВСЕ КОЛОНКИ И ФОРМАТИРОВАНИЕ)
        content.append("<div class='card mb-30'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'>Список медосмотров</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        if (medicalChecks.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<div class='empty-icon'>🩺</div>");
            content.append("<h3>Нет записей о медосмотрах</h3>");
            content.append("<p>Попробуйте изменить фильтры или добавить новый медосмотр</p>");
            content.append("<a href='/medical-checks/new' class='btn btn-success mt-20'>Добавить первый медосмотр</a>");
            content.append("</div>");
        } else {
            content.append("<div class='table-container'>");
            content.append("<table>");
            content.append("<thead>");
            content.append("<tr>");
            content.append("<th>Водитель</th>");
            content.append("<th>Врач</th>");
            content.append("<th>Дата</th>");
            content.append("<th>Статус</th>");
            content.append("<th>Смена</th>");
            content.append("<th>Примечания</th>");
            content.append("<th>Действия</th>");
            content.append("</tr>");
            content.append("</thead>");
            content.append("<tbody>");

            for (MedicalCheck check : medicalChecks) {
                content.append("<tr>");
                content.append("<td><strong>").append(check.getDriver().getFullName()).append("</strong><br>")
                        .append("<small style='color: #888;'>").append(check.getDriver().getLicenseNumber()).append("</small></td>");
                content.append("<td>").append(check.getDoctor().getFullName()).append("</td>");
                content.append("<td>").append(dateFormatter.format(check.getCheckDate())).append("</td>");
                content.append("<td>");
                content.append("<span class='badge ").append(check.getIsPassed() ? "badge-success" : "badge-danger").append("'>");
                content.append(check.getIsPassed() ? " Допущен" : " Не допущен");
                content.append("</span>");
                content.append("</td>");
                content.append("<td>");
                content.append("<span class='badge ").append(Boolean.TRUE.equals(check.getOpensShift()) ? "badge-success" : "badge-secondary").append("'>");
                content.append(Boolean.TRUE.equals(check.getOpensShift()) ? " Открыта" : "—");
                content.append("</span>");
                content.append("</td>");
                content.append("<td style='max-width: 200px;'>");
                content.append(check.getNotes() != null && !check.getNotes().isEmpty() ?
                        check.getNotes() : "<span style='color: #888;'>—</span>");
                content.append("</td>");

                // Кнопки действий (СОХРАНИЛ ВСЕ КНОПКИ)
                content.append("<td>");
                content.append("<div class='action-buttons-small'>");
                content.append("<a href='/medical-checks/edit?id=").append(check.getId())
                        .append("' class='btn btn-sm' title='Редактировать'>Ред.</a>");
                content.append("<a href='/medical-checks/delete?id=").append(check.getId())
                        .append("' class='btn btn-sm btn-danger' title='Удалить' onclick='return confirm(\"Удалить медосмотр?\");'>Удалить️</a>");
                content.append("</div>");
                content.append("</td>");
                content.append("</tr>");
            }

            content.append("</tbody>");
            content.append("</table>");
            content.append("</div>");

            // Количество записей
            content.append("<div class='table-footer'>");
            content.append("<div class='table-count'>Показано: ").append(medicalChecks.size()).append(" записей</div>");
            content.append("</div>");
        }

        content.append("</div>");
        content.append("</div>");

        // Краткая статистика (СОХРАНИЛ СТИЛИ КВАДРАТИКОВ)
        MedicalCheckService.MedicalCheckStats stats = medicalCheckService.getStatistics();
        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'> Статистика медосмотров</h3>");
        content.append("</div>");
        content.append("<div class='card-body'>");
        content.append("<div class='stats-grid'>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(stats.total).append("</div>");
        content.append("<div class='stat-label'>Всего осмотров</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(stats.passed).append("</div>");
        content.append("<div class='stat-label'>Допущены</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(stats.failed).append("</div>");
        content.append("<div class='stat-label'>Не допущены</div>");
        content.append("</div>");

        content.append("<div class='stat-card'>");
        content.append("<div class='stat-icon'></div>");
        content.append("<div class='stat-value'>").append(stats.opensShift).append("</div>");
        content.append("<div class='stat-label'>Открыли смену</div>");
        content.append("</div>");

        content.append("</div>");
        content.append("</div>");
        content.append("</div>");

        // Рендерим полную страницу
        HtmlUtil.renderFullPage(out, currentRequest, "Медосмотры", "medical-checks", content.toString());
    }

    private List<MedicalCheck> getFilteredMedicalChecks(String driverFilter, String dateFilter,
                                                        String statusFilter, String shiftFilter,
                                                        String search) {
        List<MedicalCheck> allChecks = medicalCheckService.getAllMedicalChecks();

        return allChecks.stream()
                .filter(check -> {
                    // Фильтр по водителю
                    if (driverFilter != null && !driverFilter.isEmpty()) {
                        try {
                            Long driverId = Long.parseLong(driverFilter);
                            if (!check.getDriver().getId().equals(driverId)) {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            // Игнорируем неверный формат
                        }
                    }

                    // Фильтр по дате
                    if (dateFilter != null && !dateFilter.isEmpty()) {
                        try {
                            LocalDate filterDate = LocalDate.parse(dateFilter, dateOnlyFormatter);
                            LocalDate checkDate = check.getCheckDate().toLocalDate();
                            if (!checkDate.equals(filterDate)) {
                                return false;
                            }
                        } catch (DateTimeParseException e) {
                            // Игнорируем неверный формат даты
                        }
                    }

                    // Фильтр по статусу
                    if (statusFilter != null && !statusFilter.isEmpty()) {
                        if ("passed".equals(statusFilter) && !check.getIsPassed()) {
                            return false;
                        }
                        if ("failed".equals(statusFilter) && check.getIsPassed()) {
                            return false;
                        }
                    }

                    // Фильтр по смене
                    if (shiftFilter != null && !shiftFilter.isEmpty()) {
                        if ("yes".equals(shiftFilter) && !Boolean.TRUE.equals(check.getOpensShift())) {
                            return false;
                        }
                        if ("no".equals(shiftFilter) && Boolean.TRUE.equals(check.getOpensShift())) {
                            return false;
                        }
                    }

                    // Поиск по тексту
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = check.getDriver().getFullName().toLowerCase().contains(searchLower) ||
                                check.getDoctor().getFullName().toLowerCase().contains(searchLower) ||
                                (check.getNotes() != null && check.getNotes().toLowerCase().contains(searchLower)) ||
                                check.getDriver().getLicenseNumber().toLowerCase().contains(searchLower);
                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((c1, c2) -> c2.getCheckDate().compareTo(c1.getCheckDate())) // Сортировка по дате (новые сначала)
                .toList();
    }

    private void showCreateForm(PrintWriter out, User currentUser) {
        List<Driver> drivers = medicalCheckService.getAllDrivers();
        List<User> doctors = medicalCheckService.getAllDoctors();

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h2 class='card-title'> Добавить медосмотр</h2>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        content.append("<form method='post' action='/medical-checks/save' class='form'>");

        content.append("<div class='grid grid-2'>");

        content.append("<div class='form-group'>");
        content.append("<label for='driverId' class='form-label'>Водитель <span class='required'>*</span></label>");
        content.append("<select class='form-control' id='driverId' name='driverId' required>");
        content.append("<option value=''>-- Выберите водителя --</option>");
        for (Driver driver : drivers) {
            content.append("<option value='").append(driver.getId()).append("'>")
                    .append(driver.getFullName()).append(" (").append(driver.getLicenseNumber()).append(")</option>");
        }
        content.append("</select>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='doctorId' class='form-label'>Врач <span class='required'>*</span></label>");
        content.append("<select class='form-control' id='doctorId' name='doctorId' required>");
        content.append("<option value=''>-- Выберите врача --</option>");
        for (User doctor : doctors) {
            content.append("<option value='").append(doctor.getId()).append("'>").append(doctor.getFullName()).append("</option>");
        }
        content.append("</select>");
        content.append("</div>");

        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label class='form-label'>Статус медосмотра</label>");
        content.append("<div class='radio-group'>");
        content.append("<label class='radio-label'>");
        content.append("<input type='radio' name='isPassed' value='true' checked>  Допущен к работе");
        content.append("</label>");
        content.append("<label class='radio-label'>");
        content.append("<input type='radio' name='isPassed' value='false'>  Не допущен");
        content.append("</label>");
        content.append("</div>");
        content.append("</div>");

        content.append("<div class='form-check mb-20'>");
        content.append("<input type='checkbox' class='form-check-input' id='opensShift' name='opensShift' value='true'>");
        content.append("<label for='opensShift' class='form-check-label'>Сразу открыть смену для водителя</label>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='notes' class='form-label'>Примечания</label>");
        content.append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Дополнительная информация...'></textarea>");
        content.append("</div>");

        content.append("<div class='form-actions'>");
        content.append("<button type='submit' class='btn btn-success'> Сохранить</button>");
        content.append("<a href='/medical-checks' class='btn btn-danger'> Отмена</a>");
        content.append("</div>");

        content.append("</form>");
        content.append("</div>");
        content.append("</div>");

        HtmlUtil.renderFullPage(out, currentRequest, "Добавить медосмотр", "medical-checks", content.toString());
    }

    private void showEditForm(long id, PrintWriter out, User currentUser) {
        try {
            MedicalCheck medicalCheck = medicalCheckService.getMedicalCheckById(id);
            if (medicalCheck == null) {
                HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Медосмотр не найден");
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'>✏ Редактировать медосмотр</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            // Информация о текущем медосмотре
            content.append("<div class='alert alert-info mb-30'>");
            content.append("<p><strong>Водитель:</strong> ").append(medicalCheck.getDriver().getFullName()).append("</p>");
            content.append("<p><strong>Врач:</strong> ").append(medicalCheck.getDoctor().getFullName()).append("</p>");
            content.append("<p><strong>Дата осмотра:</strong> ").append(dateFormatter.format(medicalCheck.getCheckDate())).append("</p>");
            content.append("</div>");

            content.append("<form method='post' action='/medical-checks/update' class='form'>");
            content.append("<input type='hidden' name='id' value='").append(medicalCheck.getId()).append("'>");

            content.append("<div class='form-group'>");
            content.append("<label class='form-label'>Статус медосмотра</label>");
            content.append("<div class='radio-group'>");
            content.append("<label class='radio-label'>");
            content.append("<input type='radio' name='isPassed' value='true' ")
                    .append(medicalCheck.getIsPassed() ? "checked" : "").append(">  Допущен к работе");
            content.append("</label>");
            content.append("<label class='radio-label'>");
            content.append("<input type='radio' name='isPassed' value='false' ")
                    .append(!medicalCheck.getIsPassed() ? "checked" : "").append(">  Не допущен");
            content.append("</label>");
            content.append("</div>");
            content.append("</div>");

            content.append("<div class='form-check mb-20'>");
            content.append("<input type='checkbox' class='form-check-input' id='opensShift' name='opensShift' value='true' ")
                    .append(Boolean.TRUE.equals(medicalCheck.getOpensShift()) ? "checked" : "").append(">");
            content.append("<label for='opensShift' class='form-check-label'>Открыть смену для водителя</label>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='notes' class='form-label'>Примечания</label>");
            content.append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Дополнительная информация...'>");
            content.append(medicalCheck.getNotes() != null ? medicalCheck.getNotes() : "");
            content.append("</textarea>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-success'> Сохранить изменения</button>");
            content.append("<a href='/medical-checks' class='btn btn-danger'> Отмена</a>");
            content.append("</div>");
            content.append("</form>");

            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Редактировать медосмотр", "medical-checks", content.toString());

        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, currentRequest, "Ошибка", "Ошибка при загрузке медосмотра: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        currentRequest = request; // Сохраняем request

        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        // Проверяем права доступа для POST операций
        String userRole = currentUser.getUserType();
        if (!"ADMIN".equals(userRole) && !"DOCTOR".equals(userRole)) {
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
                    saveMedicalCheck(request, response, currentUser);
                    break;
                case "/update":
                    updateMedicalCheck(request, response, currentUser);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/medical-checks?error=" + e.getMessage());
        }
    }

    private void saveMedicalCheck(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Long driverId = Long.parseLong(request.getParameter("driverId"));
            Long doctorId = Long.parseLong(request.getParameter("doctorId"));
            Boolean isPassed = "true".equals(request.getParameter("isPassed"));
            String notes = request.getParameter("notes");
            Boolean opensShift = "true".equals(request.getParameter("opensShift"));

            MedicalCheck check = medicalCheckService.createMedicalCheck(driverId, doctorId, isPassed, notes);

            if (opensShift) {
                medicalCheckService.openShiftForDriver(driverId);
            }

            response.sendRedirect("/medical-checks?success=created");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/medical-checks?error=" + e.getMessage());
        }
    }

    private void updateMedicalCheck(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Long checkId = Long.parseLong(request.getParameter("id"));
            Boolean isPassed = "true".equals(request.getParameter("isPassed"));
            String notes = request.getParameter("notes");
            Boolean opensShift = "true".equals(request.getParameter("opensShift"));

            medicalCheckService.updateMedicalCheck(checkId, isPassed, notes, opensShift);
            response.sendRedirect("/medical-checks?success=updated");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/medical-checks?error=" + e.getMessage());
        }
    }
}