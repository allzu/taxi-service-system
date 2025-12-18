package com.taxi.controller;

import com.taxi.entity.User;
import com.taxi.service.UserService;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/admin/users/*")
public class UserManagementServlet extends HttpServlet {

    private UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Проверка авторизации и роли
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        // Только ADMIN имеет доступ
        if (!"ADMIN".equals(currentUser.getUserType())) {
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            HtmlUtil.renderAccessDeniedPage(out, request, currentUser.getUserType());
            return;
        }

        String path = request.getPathInfo();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if (path == null || path.equals("/") || path.isEmpty()) {
                showUsersList(request, out);
            } else if (path.equals("/new")) {
                showCreateForm(out, request);
            } else if (path.equals("/edit")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long userId = Long.parseLong(idParam);
                    showEditForm(userId, out, request);
                } else {
                    HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не указан ID пользователя");
                }
            } else if (path.equals("/delete")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long userId = Long.parseLong(idParam);

                    // Проверяем, не пытается ли админ удалить сам себя
                    if (userId.equals(currentUser.getId())) {
                        response.sendRedirect(request.getContextPath() + "/admin/users?error=Нельзя удалить свою учетную запись");
                        return;
                    }

                    userService.delete(userId);
                    response.sendRedirect(request.getContextPath() + "/admin/users?success=deleted");
                    return;
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Проверка авторизации и роли
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            response.sendRedirect("/login");
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
                    saveUser(request, response);
                    break;
                case "/update":
                    updateUser(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/admin/users?error=" + e.getMessage());
        }
    }

    private void showUsersList(HttpServletRequest request, PrintWriter out) {
        // Получаем параметры фильтрации
        String roleFilter = request.getParameter("role");
        String search = request.getParameter("search");
        String driverUserId = request.getParameter("driver_user_id");

        // Получаем отфильтрованный список пользователей
        List<User> users = getFilteredUsers(roleFilter, search);

        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Показываем сообщения об успехе/ошибке
        String success = request.getParameter("success");
        String error = request.getParameter("error");

        if (success != null) {
            String message = switch (success) {
                case "created" -> " Пользователь успешно создан!";
                case "updated" -> " Пользователь успешно обновлен!";
                case "deleted" -> " Пользователь успешно удален!";
                default -> "";
            };
            if (!message.isEmpty()) {
                content.append("<div class='card fade-in'>")
                        .append("<div class='alert alert-success'>").append(message).append("</div>")
                        .append("</div>");
            }
        }

        // Подсказка для водителя (если только что создан)
        if (driverUserId != null) {
            content.append("<div class='card fade-in'>")
                    .append("<div class='alert alert-info'>")
                    .append(" <strong>Пользователь-водитель создан!</strong><br>")
                    .append("ID пользователя: <strong>").append(driverUserId).append("</strong><br><br>")
                    .append("Чтобы он мог работать в системе, нужно создать карточку водителя:<br>")
                    .append("1. Перейдите в <a href='/drivers/new' class='alert-link'>раздел Водители</a><br>")
                    .append("2. Нажмите кнопку ' Добавить водителя'<br>")
                    .append("3. В форме выберите этого пользователя из списка 'Привязать к учетной записи'")
                    .append("</div>")
                    .append("</div>");
        }

        if (error != null) {
            content.append("<div class='card fade-in'>")
                    .append("<div class='alert alert-danger'>Ошибка: ").append(error).append("</div>")
                    .append("</div>");
        }

        // Заголовок страницы
        content.append("<div class='card mb-20'>")
                .append("<h1 class='page-title'>Управление пользователями</h1>")
                .append("<p class='page-subtitle'>Создание, редактирование и удаление учетных записей</p>")
                .append("</div>");

        // Кнопка добавления
        content.append("<div class='card mb-20'>")
                .append("<div class='action-buttons'>")
                .append("<a href='/admin/users/new' class='btn btn-success'> Добавить пользователя</a>")
                .append("</div>")
                .append("</div>");

        // Блок фильтров
        content.append(showFiltersHtml(roleFilter, search));

        // Таблица пользователей
        if (users.isEmpty()) {
            content.append("<div class='card text-center fade-in'>")
                    .append("<div class='empty-state'>")
                    .append("<div class='empty-icon'></div>")
                    .append("<h3>Нет пользователей</h3>")
                    .append("<p>По выбранным фильтрам ничего не найдено</p>")
                    .append("<a href='/admin/users/new' class='btn btn-success mt-20'>Добавить первого пользователя</a>")
                    .append("</div>")
                    .append("</div>");
        } else {
            content.append("<div class='card fade-in'>")
                    .append("<div class='table-container'>")
                    .append("<table>")
                    .append("<thead>")
                    .append("<tr>")
                    .append("<th>ID</th>")
                    .append("<th>Логин</th>")
                    .append("<th>ФИО</th>")
                    .append("<th>Роль</th>")
                    .append("<th>Код доступа</th>")
                    .append("<th>Статус</th>")
                    .append("<th>Действия</th>")
                    .append("</tr>")
                    .append("</thead>")
                    .append("<tbody>");

            for (User user : users) {
                String roleClass = getRoleBadgeClass(user.getUserType());
                String roleText = getRoleDisplayName(user.getUserType());
                String statusClass = user.getIsActive() ? "badge-success" : "badge-danger";
                String statusText = user.getIsActive() ? "Активен" : "Неактивен";

                content.append("<tr>")
                        .append("<td><strong>#").append(user.getId()).append("</strong></td>")
                        .append("<td><strong>").append(user.getLogin()).append("</strong></td>")
                        .append("<td>").append(user.getFullName()).append("</td>")
                        .append("<td><span class='badge ").append(roleClass).append("'>").append(roleText).append("</span></td>")
                        .append("<td>").append(user.getAccessCode() != null ? user.getAccessCode() : "-").append("</td>")
                        .append("<td><span class='badge ").append(statusClass).append("'>").append(statusText).append("</span></td>")
                        .append("<td>")
                        .append("<div class='action-buttons-small'>")
                        .append("<a href='/admin/users/edit?id=").append(user.getId())
                        .append("' class='btn btn-sm btn-primary' title='Редактировать'>Ред.</a>")
                        .append("<a href='/admin/users/delete?id=").append(user.getId())
                        .append("' class='btn btn-sm btn-danger' onclick='return confirm(\\\"Удалить пользователя ").append(user.getLogin()).append("?\\\");' title='Удалить'>Удалить</a>")
                        .append("</div>")
                        .append("</td>")
                        .append("</tr>");
            }

            content.append("</tbody>")
                    .append("</table>")
                    .append("</div>");

            // Количество записей
            content.append("<div class='mt-20' style='padding-top: 15px; border-top: 1px solid #333;'>")
                    .append("<div style='color: #888; font-size: 0.9em;'>Показано: ").append(users.size()).append(" пользователей</div>")
                    .append("</div>")
                    .append("</div>");
        }

        // Статистика
        content.append(showStatisticsHtml());

        HtmlUtil.renderFullPage(out, request, "Управление пользователями", "users", content.toString());
    }

    private String showFiltersHtml(String roleFilter, String search) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='card mb-20'>")
                .append("<h3>Фильтры</h3>")
                .append("<form method='get' action='/admin/users' class='form-horizontal'>")
                .append("<div class='info-grid'>");

        // Поиск
        html.append("<div class='form-group'>")
                .append("<label for='search' class='form-label'>Поиск</label>")
                .append("<input type='text' class='form-control' id='search' name='search' ")
                .append("placeholder='Логин, ФИО...' value='").append(search != null ? search : "").append("'>")
                .append("</div>");

        // Фильтр по роли
        html.append("<div class='form-group'>")
                .append("<label for='role' class='form-label'>Роль</label>")
                .append("<select class='form-control' id='role' name='role'>")
                .append("<option value=''>Все роли</option>")
                .append("<option value='ADMIN' ").append("ADMIN".equals(roleFilter) ? "selected" : "").append("> Администратор</option>")
                .append("<option value='OPERATOR' ").append("OPERATOR".equals(roleFilter) ? "selected" : "").append("> Диспетчер</option>")
                .append("<option value='DOCTOR' ").append("DOCTOR".equals(roleFilter) ? "selected" : "").append("> Врач</option>")
                .append("<option value='MECHANIC' ").append("MECHANIC".equals(roleFilter) ? "selected" : "").append("> Механик</option>")
                .append("<option value='DRIVER' ").append("DRIVER".equals(roleFilter) ? "selected" : "").append("> Водитель</option>")
                .append("</select>")
                .append("</div>");

        html.append("</div>") // Закрываем info-grid
                .append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-primary'> Применить</button>")
                .append("<a href='/admin/users' class='btn btn-secondary'> Сбросить</a>")
                .append("</div>")
                .append("</form>")
                .append("</div>");

        return html.toString();
    }

    private String showStatisticsHtml() {
        long totalUsers = userService.getTotalUsers();
        long activeUsers = userService.getActiveUsersCount();

        StringBuilder html = new StringBuilder();

        html.append("<div class='card fade-in'>")
                .append("<h3> Статистика пользователей</h3>")
                .append("<div class='stats-grid'>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(totalUsers).append("</div>")
                .append("<div class='stat-label'>Всего пользователей</div>")
                .append("</div>");

        html.append("<div class='stat-card'>")
                .append("<div class='stat-icon'></div>")
                .append("<div class='stat-value'>").append(activeUsers).append("</div>")
                .append("<div class='stat-label'>Активных</div>")
                .append("</div>");

        // Статистика по ролям
        String[] roles = {"ADMIN", "OPERATOR", "DOCTOR", "MECHANIC", "DRIVER"};
        String[] roleIcons = {"", "", "", "", ""};

        for (int i = 0; i < roles.length; i++) {
            List<User> roleUsers = userService.getUsersByRole(roles[i]);
            if (!roleUsers.isEmpty()) {
                html.append("<div class='stat-card'>")
                        .append("<div class='stat-icon'>").append(roleIcons[i]).append("</div>")
                        .append("<div class='stat-value'>").append(roleUsers.size()).append("</div>")
                        .append("<div class='stat-label'>").append(getRoleDisplayName(roles[i])).append("</div>")
                        .append("</div>");
            }
        }

        html.append("</div>")
                .append("<div class='mt-20'>")
                .append("<p><strong>Примечание:</strong> Пользователи с ролью <strong> Водитель</strong> требуют дополнительной настройки:</p>")
                .append("<ol style='margin-left: 20px;'>")
                .append("<li>Создайте пользователя с ролью Водитель</li>")
                .append("<li>Перейдите в раздел <a href='/drivers' class='alert-link'>Водители</a></li>")
                .append("<li>Создайте карточку водителя и привяжите к учетной записи</li>")
                .append("</ol>")
                .append("</div>")
                .append("</div>");

        return html.toString();
    }

    private List<User> getFilteredUsers(String roleFilter, String search) {
        List<User> allUsers = userService.getAllUsers();

        return allUsers.stream()
                .filter(user -> {
                    // Фильтр по роли
                    if (roleFilter != null && !roleFilter.isEmpty()) {
                        if (!user.getUserType().equals(roleFilter)) {
                            return false;
                        }
                    }

                    // Поиск по тексту
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = user.getLogin().toLowerCase().contains(searchLower) ||
                                user.getFullName().toLowerCase().contains(searchLower);
                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((u1, u2) -> u1.getId().compareTo(u2.getId())) // Сортировка по ID
                .toList();
    }

    private void showCreateForm(PrintWriter out, HttpServletRequest request) {
        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>")
                .append("<h2 class='page-title'> Добавить пользователя</h2>")
                .append("<form method='post' action='/admin/users/save' class='form-vertical'>");

        content.append("<div class='info-grid'>");

        // Логин
        content.append("<div class='form-group'>")
                .append("<label for='login' class='form-label'>Логин <span class='required'>*</span></label>")
                .append("<input type='text' class='form-control' id='login' name='login' required ")
                .append("placeholder='Уникальный логин для входа'>")
                .append("<p class='form-hint'>Только латинские буквы и цифры</p>")
                .append("</div>");

        // Пароль
        content.append("<div class='form-group'>")
                .append("<label for='password' class='form-label'>Пароль <span class='required'>*</span></label>")
                .append("<input type='password' class='form-control' id='password' name='password' required ")
                .append("placeholder='Не менее 6 символов'>")
                .append("</div>");

        content.append("</div>"); // Закрываем info-grid

        // ФИО
        content.append("<div class='form-group'>")
                .append("<label for='fullName' class='form-label'>ФИО <span class='required'>*</span></label>")
                .append("<input type='text' class='form-control' id='fullName' name='fullName' required ")
                .append("placeholder='Иванов Иван Иванович'>")
                .append("</div>");

        // Роль
        content.append("<div class='form-group'>")
                .append("<label for='userType' class='form-label'>Роль <span class='required'>*</span></label>")
                .append("<select class='form-control' id='userType' name='userType' required>")
                .append("<option value=''>-- Выберите роль --</option>")
                .append("<option value='ADMIN'> Администратор</option>")
                .append("<option value='OPERATOR'> Диспетчер</option>")
                .append("<option value='DOCTOR'> Врач</option>")
                .append("<option value='MECHANIC'> Механик</option>")
                .append("<option value='DRIVER'> Водитель</option>")
                .append("</select>")
                .append("<p class='form-hint'>Определяет права доступа в системе</p>")
                .append("<div class='alert alert-warning mt-10' id='driver-hint' style='display: none;'>")
                .append(" <strong>Для водителя нужно дополнительное действие:</strong><br>")
                .append("После создания учетной записи перейдите в раздел ")
                .append("<a href='/drivers' class='alert-link'>Водители</a> и создайте карточку водителя, ")
                .append("привязав её к этой учетной записи.")
                .append("</div>")
                .append("</div>");

        // Код доступа (опционально)
        content.append("<div class='form-group'>")
                .append("<label for='accessCode' class='form-label'>Код доступа</label>")
                .append("<input type='number' class='form-control' id='accessCode' name='accessCode' ")
                .append("placeholder='Числовой код (опционально)'>")
                .append("<p class='form-hint'>Используется для дополнительной идентификации</p>")
                .append("</div>");

        // Кнопки
        content.append("<div class='form-actions'>")
                .append("<button type='submit' class='btn btn-success'> Создать пользователя</button>")
                .append("<a href='/admin/users' class='btn btn-danger'> Отмена</a>")
                .append("</div>")
                .append("</form>")

                // Простой JavaScript для показа подсказки водителя
                .append("<script>")
                .append("document.getElementById('userType').addEventListener('change', function() {")
                .append("  var driverHint = document.getElementById('driver-hint');")
                .append("  driverHint.style.display = this.value === 'DRIVER' ? 'block' : 'none';")
                .append("});")
                .append("</script>")
                .append("</div>");

        HtmlUtil.renderFullPage(out, request, "Добавить пользователя", "users", content.toString());
    }

    private void showEditForm(Long userId, PrintWriter out, HttpServletRequest request) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                HtmlUtil.renderErrorPage(out, request, "Ошибка", "Пользователь не найден");
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>")
                    .append("<h2 class='page-title'>️ Редактировать пользователя</h2>")
                    .append("<form method='post' action='/admin/users/update' class='form-vertical'>")
                    .append("<input type='hidden' name='id' value='").append(user.getId()).append("'>");

            // Информация о текущем пользователе
            content.append("<div class='info-section mb-30'>")
                    .append("<h4>Текущие данные</h4>")
                    .append("<div class='info-grid'>")
                    .append("<div><strong>ID:</strong><br>#").append(user.getId()).append("</div>")
                    .append("<div><strong>Логин:</strong><br>").append(user.getLogin()).append("</div>")
                    .append("<div><strong>Создан:</strong><br>").append(user.getIsActive() ? " Активен" : " Неактивен").append("</div>")
                    .append("</div>")
                    .append("</div>");

            content.append("<div class='info-grid'>");

            // ФИО
            content.append("<div class='form-group'>")
                    .append("<label for='fullName' class='form-label'>ФИО <span class='required'>*</span></label>")
                    .append("<input type='text' class='form-control' id='fullName' name='fullName' required ")
                    .append("value='").append(user.getFullName()).append("'>")
                    .append("</div>");

            // Роль
            content.append("<div class='form-group'>")
                    .append("<label for='userType' class='form-label'>Роль <span class='required'>*</span></label>")
                    .append("<select class='form-control' id='userType' name='userType' required>")
                    .append("<option value='ADMIN' ").append("ADMIN".equals(user.getUserType()) ? "selected" : "").append("> Администратор</option>")
                    .append("<option value='OPERATOR' ").append("OPERATOR".equals(user.getUserType()) ? "selected" : "").append("> Диспетчер</option>")
                    .append("<option value='DOCTOR' ").append("DOCTOR".equals(user.getUserType()) ? "selected" : "").append("> Врач</option>")
                    .append("<option value='MECHANIC' ").append("MECHANIC".equals(user.getUserType()) ? "selected" : "").append("> Механик</option>")
                    .append("<option value='DRIVER' ").append("DRIVER".equals(user.getUserType()) ? "selected" : "").append("> Водитель</option>")
                    .append("</select>")
                    .append("</div>");

            content.append("</div>"); // Закрываем info-grid

            // Код доступа
            content.append("<div class='form-group'>")
                    .append("<label for='accessCode' class='form-label'>Код доступа</label>")
                    .append("<input type='number' class='form-control' id='accessCode' name='accessCode' ")
                    .append("value='").append(user.getAccessCode() != null ? user.getAccessCode() : "").append("'>")
                    .append("</div>");

            // Статус
            content.append("<div class='form-group'>")
                    .append("<label class='form-label'>Статус учетной записи</label>")
                    .append("<div style='display: flex; gap: 20px; margin-top: 10px;'>")
                    .append("<label class='radio-label'>")
                    .append("<input type='radio' name='isActive' value='true' ")
                    .append(user.getIsActive() ? "checked" : "").append(">  Активен")
                    .append("</label>")
                    .append("<label class='radio-label'>")
                    .append("<input type='radio' name='isActive' value='false' ")
                    .append(!user.getIsActive() ? "checked" : "").append(">  Неактивен")
                    .append("</label>")
                    .append("</div>")
                    .append("</div>");

            // Кнопки
            content.append("<div class='form-actions'>")
                    .append("<button type='submit' class='btn btn-success'> Сохранить изменения</button>")
                    .append("<a href='/admin/users' class='btn btn-danger'> Отмена</a>")
                    .append("</div>")
                    .append("</form>")
                    .append("</div>");

            HtmlUtil.renderFullPage(out, request, "Редактировать пользователя", "users", content.toString());
        } catch (Exception e) {
            HtmlUtil.renderErrorPage(out, request, "Ошибка", "Не удалось загрузить пользователя: " + e.getMessage());
        }
    }

    private void saveUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Получаем данные из формы
            String login = request.getParameter("login");
            String password = request.getParameter("password");
            String fullName = request.getParameter("fullName");
            String userType = request.getParameter("userType");
            String accessCodeParam = request.getParameter("accessCode");

            // Проверка на уникальность логина
            User existingUser = userService.findByLogin(login);
            if (existingUser != null) {
                throw new IllegalArgumentException("Пользователь с логином '" + login + "' уже существует");
            }

            // Создаем нового пользователя
            User user = new User();
            user.setLogin(login);
            user.setPassword(password); // В реальном приложении нужно хэшировать пароль!
            user.setFullName(fullName);
            user.setUserType(userType);
            user.setIsActive(true); // Новые пользователи активны по умолчанию

            if (accessCodeParam != null && !accessCodeParam.isEmpty()) {
                user.setAccessCode(Integer.parseInt(accessCodeParam));
            }

            // Сохраняем в базу
            userService.save(user);

            // Если создан пользователь-водитель, показываем подсказку
            if ("DRIVER".equals(userType)) {
                response.sendRedirect(request.getContextPath() + "/admin/users?success=created&driver_user_id=" + user.getId());
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/users?success=created");
            }

        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath() + "/admin/users?error=" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/admin/users?error=Ошибка при создании пользователя");
        }
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Получаем данные из формы
            Long userId = Long.parseLong(request.getParameter("id"));
            String fullName = request.getParameter("fullName");
            String userType = request.getParameter("userType");
            String accessCodeParam = request.getParameter("accessCode");
            Boolean isActive = "true".equals(request.getParameter("isActive"));

            // Получаем существующего пользователя
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new IllegalArgumentException("Пользователь не найден");
            }

            // Обновляем данные
            user.setFullName(fullName);
            user.setUserType(userType);
            user.setIsActive(isActive);

            if (accessCodeParam != null && !accessCodeParam.isEmpty()) {
                user.setAccessCode(Integer.parseInt(accessCodeParam));
            } else {
                user.setAccessCode(null);
            }

            // Сохраняем изменения
            userService.update(user);

            response.sendRedirect(request.getContextPath() + "/admin/users?success=updated");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/admin/users?error=Ошибка при обновлении пользователя");
        }
    }

    // Вспомогательные методы для стилей
    private String getRoleBadgeClass(String role) {
        return switch (role) {
            case "ADMIN" -> "badge-danger";
            case "OPERATOR" -> "badge-secondary";
            case "DOCTOR" -> "badge-success";
            case "MECHANIC" -> "badge-warning";
            case "DRIVER" -> "badge-info";
            default -> "badge-secondary";
        };
    }

    private String getRoleDisplayName(String role) {
        return switch (role) {
            case "ADMIN" -> " Администратор";
            case "OPERATOR" -> " Диспетчер";
            case "DOCTOR" -> " Врач";
            case "MECHANIC" -> " Механик";
            case "DRIVER" -> " Водитель";
            default -> role;
        };
    }
}