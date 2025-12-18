package com.taxi.controller;

import com.taxi.entity.*;
import com.taxi.service.TechnicalInspectionService;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/inspections/*")
public class TechnicalInspectionServlet extends HttpServlet {

    private TechnicalInspectionService inspectionService = new TechnicalInspectionService();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // === ПРОВЕРКА АВТОРИЗАЦИИ И ПРАВ ===
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        // Проверяем права доступа - только ADMIN и MECHANIC
        String userRole = currentUser.getUserType();
        if (!"ADMIN".equals(userRole) && !"MECHANIC".equals(userRole)) {
            // Показываем страницу "Доступ запрещен" с новым дизайном
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
                // Основной список техосмотров с фильтрами
                showTechnicalInspectionsList(request, out);
            } else if (path.equals("/new")) {
                // Форма добавления
                showCreateForm(out, request);
            } else if (path.equals("/edit")) {
                // Форма редактирования
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long inspectionId = Long.parseLong(idParam);
                    showEditForm(inspectionId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID техосмотра");
                }
            } else if (path.equals("/delete")) {
                // Удаление
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long inspectionId = Long.parseLong(idParam);
                    inspectionService.delete(inspectionId);
                    response.sendRedirect(request.getContextPath() + "/inspections?success=deleted");
                    return;
                }
            } else {
                // Неизвестный путь
                HtmlUtil.renderErrorPage(out, request, "Страница не найдена", "Запрошенная страница не существует");
            }
        } catch (NumberFormatException e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка формата", "Неверный формат ID");
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showTechnicalInspectionsList(HttpServletRequest request, PrintWriter out) {
        // Получаем параметры фильтрации
        String carFilter = request.getParameter("car");
        String dateFilter = request.getParameter("date");
        String statusFilter = request.getParameter("status");
        String search = request.getParameter("search");

        // Получаем отфильтрованный список техосмотров
        List<TechnicalInspection> inspections = getFilteredInspections(carFilter, dateFilter, statusFilter, search);

        // Получаем данные для фильтров
        List<Car> allCars = inspectionService.getAllCars();

        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Показываем сообщения об успехе/ошибке
        String success = request.getParameter("success");
        String error = request.getParameter("error");

        if (success != null) {
            String message = switch (success) {
                case "created" -> " Техосмотр успешно добавлен!";
                case "updated" -> " Техосмотр успешно обновлен!";
                case "deleted" -> "️ Техосмотр успешно удален!";
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
                .append("<h1 class='page-title'> Техосмотры</h1>")
                .append("<p class='page-subtitle'>Управление техническими осмотрами автомобилей</p>")
                .append("</div>");

        // Кнопка добавления
        content.append("<div class='card mb-20'>")
                .append("<div class='action-buttons'>")
                .append("<a href='/inspections/new' class='btn btn-success'> Добавить техосмотр</a>")
                .append("</div>")
                .append("</div>");

        // Блок фильтров
        content.append(showFiltersHtml(carFilter, dateFilter, statusFilter, search, allCars));

        // Таблица техосмотров
        if (inspections.isEmpty()) {
            content.append("<div class='card text-center fade-in'>")
                    .append("<div class='empty-state'>")
                    .append("<div class='empty-icon'></div>")
                    .append("<h3>Нет записей о техосмотрах</h3>")
                    .append("<p>По выбранным фильтрам ничего не найдено</p>")
                    .append("<a href='/inspections/new' class='btn btn-success mt-20'>Добавить первый техосмотр</a>")
                    .append("</div>")
                    .append("</div>");
        } else {
            content.append("<div class='card fade-in'>")
                    .append("<div class='table-container'>")
                    .append("<table>")
                    .append("<thead>")
                    .append("<tr>")
                    .append("<th>Автомобиль</th>")
                    .append("<th>Механик</th>")
                    .append("<th>Дата</th>")
                    .append("<th>Пробег</th>")
                    .append("<th>Статус</th>")
                    .append("<th>Примечания</th>")
                    .append("<th>Действия</th>")
                    .append("</tr>")
                    .append("</thead>")
                    .append("<tbody>");

            for (TechnicalInspection inspection : inspections) {
                String carInfo = inspection.getCar() != null ?
                        inspection.getCar().getBrand() + " " + inspection.getCar().getModel() +
                                " (" + inspection.getCar().getLicensePlate() + ")" : "Неизвестно";

                String mechanicInfo = inspection.getMechanic() != null ?
                        inspection.getMechanic().getFullName() : "Неизвестно";

                String mileage = inspection.getMileageKm() != null ?
                        inspection.getMileageKm() + " км" : "-";

                String badgeClass = inspection.getIsPassed() ? "badge-success" : "badge-danger";
                String statusText = inspection.getIsPassed() ? " Исправен" : " Неисправен";

                content.append("<tr>")
                        .append("<td><strong>").append(carInfo).append("</strong></td>")
                        .append("<td>").append(mechanicInfo).append("</td>")
                        .append("<td>").append(dateFormatter.format(inspection.getInspectionDate())).append("</td>")
                        .append("<td>").append(mileage).append("</td>")
                        .append("<td><span class='badge ").append(badgeClass).append("'>").append(statusText).append("</span></td>")
                        .append("<td style='max-width: 200px;'>")
                        .append(inspection.getNotes() != null && !inspection.getNotes().isEmpty() ?
                                inspection.getNotes() : "<span style='color: #888;'>-</span>")
                        .append("</td>")
                        .append("<td>")
                        .append("<div class='action-buttons-small'>")
                        .append("<a href='/inspections/edit?id=").append(inspection.getId())
                        .append("' class='btn btn-sm btn-primary' title='Редактировать'>Ред.</a>")
                        .append("<a href='/inspections/delete?id=").append(inspection.getId())
                        .append("' class='btn btn-sm btn-danger' onclick='return confirm(\\\"Удалить техосмотр?\\\");' title='Удалить'>Удалить</a>")
                        .append("</div>")
                        .append("</td>")
                        .append("</tr>");
            }

            content.append("</tbody>")
                    .append("</table>")
                    .append("</div>");

            // Количество записей
            content.append("<div class='mt-20' style='padding-top: 15px; border-top: 1px solid #333;'>")
                    .append("<div style='color: #888; font-size: 0.9em;'>Показано: ").append(inspections.size()).append(" записей</div>")
                    .append("</div>")
                    .append("</div>");
        }

        // Статистика
        content.append(showStatisticsHtml());

        // Рендерим полную страницу
        HtmlUtil.renderFullPage(out, request, "Техосмотры", "inspections", content.toString());
    }

    private String showFiltersHtml(String carFilter, String dateFilter,
                                   String statusFilter, String search, List<Car> allCars) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='card mb-20'>")
                .append("<h3> Фильтры</h3>")
                .append("<form method='get' action='/inspections' class='form-horizontal'>")
                .append("<div class='info-grid'>");

        // Поиск
        html.append("<div class='form-group'>")
                .append("<label for='search' class='form-label'>Поиск</label>")
                .append("<input type='text' class='form-control' id='search' name='search' ")
                .append("placeholder='Номер, модель, механик...' value='").append(search != null ? search : "").append("'>")
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
                    .append(car.getBrand()).append(" ").append(car.getModel())
                    .append(" (").append(car.getLicensePlate()).append(")</option>");
        }

        html.append("</select>")
                .append("</div>");

        // Фильтр по дате
        html.append("<div class='form-group'>")
                .append("<label for='date' class='form-label'>Дата осмотра</label>")
                .append("<input type='date' class='form-control' id='date' name='date' value='")
                .append(dateFilter != null ? dateFilter : "").append("'>")
                .append("</div>");

        // Фильтр по статусу
        html.append("<div class='form-group'>")
                .append("<label for='status' class='form-label'>Статус</label>")
                .append("<select class='form-control' id='status' name='status'>")
                .append("<option value=''>Все статусы</option>")
                .append("<option value='passed' ").append("passed".equals(statusFilter) ? "selected" : "").append("> Исправен</option>")
                .append("<option value='failed' ").append("failed".equals(statusFilter) ? "selected" : "").append("> Неисправен</option>")
                .append("</select>")
                .append("</div>");

        html.append("</div>") // Закрываем info-grid
                .append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-primary'> Применить</button>")
                .append("<a href='/inspections' class='btn btn-secondary'> Сбросить</a>")
                .append("</div>")
                .append("</form>")
                .append("</div>");

        return html.toString();
    }

    private String showStatisticsHtml() {
        TechnicalInspectionService.InspectionStats stats = inspectionService.getStatistics();
        long carsWithoutInspection = inspectionService.getCarsWithoutInspectionCount();

        StringBuilder html = new StringBuilder();

        html.append("<div class='card fade-in'>")
                .append("<h3>Статистика техосмотров</h3>")
                .append("<div class='stats-grid'>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(stats.total).append("</div>")
                .append("<div class='stat-label'>Всего техосмотров</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(stats.passed).append("</div>")
                .append("<div class='stat-label'>Исправные</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(stats.failed).append("</div>")
                .append("<div class='stat-label'>Неисправные</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-value'>").append(carsWithoutInspection).append("</div>")
                .append("<div class='stat-label'>Осмотр не пройден</div>")
                .append("</div>");

        html.append("</div>")
                .append("</div>");

        return html.toString();
    }

    private void showCreateForm(PrintWriter out, HttpServletRequest request) {
        List<Car> cars = inspectionService.getAllCars();
        List<User> mechanics = inspectionService.getAllMechanics();

        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>")
                .append("<h2 class='page-title'>➕ Добавить техосмотр</h2>")
                .append("<form method='post' action='/inspections/save' class='form-vertical'>");

        content.append("<div class='info-grid'>");

        content.append("<div class='form-group'>")
                .append("<label for='carId' class='form-label'>Автомобиль <span class='required'>*</span></label>")
                .append("<select class='form-control' id='carId' name='carId' required>")
                .append("<option value=''>-- Выберите автомобиль --</option>");

        for (Car car : cars) {
            content.append("<option value='").append(car.getId()).append("'>")
                    .append(car.getBrand()).append(" ").append(car.getModel())
                    .append(" (").append(car.getLicensePlate()).append(")</option>");
        }

        content.append("</select>")
                .append("</div>");

        content.append("<div class='form-group'>")
                .append("<label for='mechanicId' class='form-label'>Механик <span class='required'>*</span></label>")
                .append("<select class='form-control' id='mechanicId' name='mechanicId' required>")
                .append("<option value=''>-- Выберите механика --</option>");

        if (mechanics.isEmpty()) {
            content.append("<option value='' disabled>Нет доступных механиков</option>");
        } else {
            for (User mechanic : mechanics) {
                content.append("<option value='").append(mechanic.getId()).append("'>")
                        .append(mechanic.getFullName()).append(" (").append(mechanic.getLogin()).append(")</option>");
            }
        }

        content.append("</select>")
                .append("</div>");

        content.append("</div>"); // Закрываем info-grid

        content.append("<div class='form-group'>")
                .append("<label for='mileageKm' class='form-label'>Пробег (км)</label>")
                .append("<input type='number' class='form-control' id='mileageKm' name='mileageKm' placeholder='Текущий пробег автомобиля'>")
                .append("<p class='form-hint'>Оставьте пустым, если неизвестно</p>")
                .append("</div>");

        content.append("<div class='form-group'>")
                .append("<label class='form-label'>Статус осмотра</label>")
                .append("<div style='display: flex; gap: 20px; margin-top: 10px;'>")
                .append("<label class='radio-label'>")
                .append("<input type='radio' name='isPassed' value='true' checked>  Исправен")
                .append("</label>")
                .append("<label class='radio-label'>")
                .append("<input type='radio' name='isPassed' value='false'>  Неисправен")
                .append("</label>")
                .append("</div>")
                .append("</div>");

        content.append("<div class='form-group'>")
                .append("<label for='notes' class='form-label'>Примечания</label>")
                .append("<textarea class='form-control' id='notes' name='notes' rows='3' ")
                .append("placeholder='Обнаруженные проблемы, замечания, рекомендации...'></textarea>")
                .append("<p class='form-hint'>Максимум 1000 символов</p>")
                .append("</div>");

        content.append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-success'> Сохранить</button>")
                .append("<a href='/inspections' class='btn btn-danger'> Отмена</a>")
                .append("</div>");

        content.append("</form>")
                .append("</div>");

        HtmlUtil.renderFullPage(out, request, "Добавить техосмотр", "inspections", content.toString());
    }

    private void showEditForm(long id, PrintWriter out, HttpServletRequest request) {
        try {
            TechnicalInspection inspection = inspectionService.findById(id);
            if (inspection == null) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка", "Техосмотр не найден");
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>")
                    .append("<h2 class='page-title'> Редактировать техосмотр</h2>");

            // Информация о текущем техосмотре
            content.append("<div class='info-section mb-30'>")
                    .append("<h4>Информация о техосмотре</h4>")
                    .append("<div class='info-grid'>")
                    .append("<div><strong>Автомобиль:</strong><br>")
                    .append(inspection.getCar().getBrand()).append(" ").append(inspection.getCar().getModel())
                    .append(" (").append(inspection.getCar().getLicensePlate()).append(")</div>")
                    .append("<div><strong>Механик:</strong><br>").append(inspection.getMechanic().getFullName()).append("</div>")
                    .append("<div><strong>Дата осмотра:</strong><br>")
                    .append(dateFormatter.format(inspection.getInspectionDate())).append("</div>")
                    .append("</div>")
                    .append("</div>");

            content.append("<form method='post' action='/inspections/update' class='form-vertical'>")
                    .append("<input type='hidden' name='id' value='").append(inspection.getId()).append("'>");

            content.append("<div class='form-group'>")
                    .append("<label for='mileageKm' class='form-label'>Пробег (км)</label>")
                    .append("<input type='number' class='form-control' id='mileageKm' name='mileageKm' ")
                    .append("value='").append(inspection.getMileageKm() != null ? inspection.getMileageKm() : "").append("'>")
                    .append("</div>");

            content.append("<div class='form-group'>")
                    .append("<label class='form-label'>Статус осмотра</label>")
                    .append("<div style='display: flex; gap: 20px; margin-top: 10px;'>")
                    .append("<label class='radio-label'>")
                    .append("<input type='radio' name='isPassed' value='true' ")
                    .append(inspection.getIsPassed() ? "checked" : "").append(">  Исправен")
                    .append("</label>")
                    .append("<label class='radio-label'>")
                    .append("<input type='radio' name='isPassed' value='false' ")
                    .append(!inspection.getIsPassed() ? "checked" : "").append(">  Неисправен")
                    .append("</label>")
                    .append("</div>")
                    .append("</div>");

            content.append("<div class='form-group'>")
                    .append("<label for='notes' class='form-label'>Примечания</label>")
                    .append("<textarea class='form-control' id='notes' name='notes' rows='3'>")
                    .append(inspection.getNotes() != null ? inspection.getNotes() : "")
                    .append("</textarea>")
                    .append("</div>");

            content.append("<div class='form-actions'>")
                    .append("<button type='submit' class='btn btn-success'> Сохранить изменения</button>")
                    .append("<a href='/inspections' class='btn btn-danger'> Отмена</a>")
                    .append("</div>")
                    .append("</form>")
                    .append("</div>");

            HtmlUtil.renderFullPage(out, request, "Редактировать техосмотр", "inspections", content.toString());
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не удалось загрузить техосмотр: " + e.getMessage());
        }
    }

    // Остальные методы остаются без изменений
    private List<TechnicalInspection> getFilteredInspections(String carFilter, String dateFilter,
                                                             String statusFilter, String search) {
        List<TechnicalInspection> allInspections = inspectionService.findAll();

        return allInspections.stream()
                .filter(inspection -> {
                    // Фильтр по автомобилю
                    if (carFilter != null && !carFilter.isEmpty()) {
                        try {
                            Long carId = Long.parseLong(carFilter);
                            if (inspection.getCar() == null || !inspection.getCar().getId().equals(carId)) {
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
                            LocalDate inspectionDate = inspection.getInspectionDate().toLocalDate();
                            if (!inspectionDate.equals(filterDate)) {
                                return false;
                            }
                        } catch (DateTimeParseException e) {
                            // Игнорируем неверный формат даты
                        }
                    }

                    // Фильтр по статусу
                    if (statusFilter != null && !statusFilter.isEmpty()) {
                        if ("passed".equals(statusFilter) && !inspection.getIsPassed()) {
                            return false;
                        }
                        if ("failed".equals(statusFilter) && inspection.getIsPassed()) {
                            return false;
                        }
                    }

                    // Поиск по тексту
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = false;

                        if (inspection.getCar() != null) {
                            matches = inspection.getCar().getLicensePlate().toLowerCase().contains(searchLower) ||
                                    inspection.getCar().getModel().toLowerCase().contains(searchLower) ||
                                    inspection.getCar().getBrand().toLowerCase().contains(searchLower);
                        }

                        if (!matches && inspection.getMechanic() != null) {
                            matches = inspection.getMechanic().getFullName().toLowerCase().contains(searchLower);
                        }

                        if (!matches && inspection.getNotes() != null) {
                            matches = inspection.getNotes().toLowerCase().contains(searchLower);
                        }

                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((i1, i2) -> i2.getInspectionDate().compareTo(i1.getInspectionDate()))
                .toList();
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

        // Проверяем права доступа
        String userRole = currentUser.getUserType();
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
                    saveInspection(request, response);
                    break;
                case "/update":
                    updateInspection(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/inspections?error=" + e.getMessage());
        }
    }

    private void saveInspection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Long carId = Long.parseLong(request.getParameter("carId"));
            Long mechanicId = Long.parseLong(request.getParameter("mechanicId"));
            Boolean isPassed = "true".equals(request.getParameter("isPassed"));
            String notes = request.getParameter("notes");

            Integer mileage = null;
            String mileageParam = request.getParameter("mileageKm");
            if (mileageParam != null && !mileageParam.isEmpty()) {
                mileage = Integer.parseInt(mileageParam);
            }

            TechnicalInspection inspection = inspectionService.createInspection(
                    carId, mechanicId, isPassed, mileage, notes
            );

            response.sendRedirect(request.getContextPath() + "/inspections?success=created");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/inspections?error=" + e.getMessage());
        }
    }

    private void updateInspection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Long inspectionId = Long.parseLong(request.getParameter("id"));
            Boolean isPassed = "true".equals(request.getParameter("isPassed"));
            String notes = request.getParameter("notes");

            Integer mileage = null;
            String mileageParam = request.getParameter("mileageKm");
            if (mileageParam != null && !mileageParam.isEmpty()) {
                mileage = Integer.parseInt(mileageParam);
            }

            TechnicalInspection inspection = inspectionService.findById(inspectionId);
            if (inspection != null) {
                inspection.setIsPassed(isPassed);
                inspection.setNotes(notes);
                if (mileage != null) {
                    inspection.setMileageKm(mileage);
                }
                inspectionService.update(inspection);
            }

            response.sendRedirect(request.getContextPath() + "/inspections?success=updated");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/inspections?error=" + e.getMessage());
        }
    }
}