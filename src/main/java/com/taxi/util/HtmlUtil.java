package com.taxi.util;

import com.taxi.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HtmlUtil {


    /**
     * Полный рендеринг страницы с меню и контентом
     */
    public static void renderFullPage(PrintWriter out, HttpServletRequest req,
                                      String pageTitle, String activePage,
                                      String content) {
        User user = getUserFromSession(req);
        String userRole = user != null ? user.getUserType() : "GUEST";
        String username = user != null ? user.getLogin() : "Гость";

        startHtml(out, pageTitle);
        renderHeader(out, username, userRole);

        out.println("<div class='layout-container'>");
        if (!"GUEST".equals(userRole)) {
            renderRoleBasedSidebar(out, userRole, activePage);
        }

        out.println("<main class='main-content'>");
        out.println(content);
        out.println("</main>");

        out.println("</div>");
        renderFooter(out);
        endHtml(out);
    }

    /**
     * Рендеринг страницы с ошибкой доступа
     */
    public static void renderAccessDeniedPage(PrintWriter out, HttpServletRequest req, String userRole) {
        User user = getUserFromSession(req);
        String username = user != null ? user.getLogin() : "Гость";

        startHtml(out, "Доступ запрещен");
        renderHeader(out, username, userRole);

        out.println("<div class='layout-container'>");
        if (user != null) {
            renderRoleBasedSidebar(out, userRole, "access-denied");
        }

        out.println("<main class='main-content'>");
        out.println("    <div class='container centered'>");
        out.println("        <div class='error-container'>");
        out.println("            <div class='error-icon'>🚫</div>");
        out.println("            <h1>Доступ запрещен</h1>");
        out.println("            <p class='error-message'>У вас недостаточно прав для доступа к этой странице.</p>");

        // Информация о роли пользователя
        if (userRole != null) {
            out.println("            <div class='info-box'>");
            out.println("                <p>Ваша роль: <strong>" + getRoleDisplayName(userRole) + "</strong></p>");
            out.println("            </div>");
        }

        // Кнопки для перехода
        out.println("            <div class='action-buttons'>");
        out.println("                <a href='/' class='btn btn-primary'> На главную</a>");

        // Динамическая кнопка в зависимости от роли
        String dashboardLink = getDashboardLinkByRole(userRole);
        if (!"/".equals(dashboardLink)) {
            out.println("                <a href='" + dashboardLink + "' class='btn btn-success'> Моя панель</a>");
        }
        out.println("            </div>");

        // Ссылка для выхода
        if (user != null) {
            out.println("            <div class='logout-hint'>");
            out.println("                <p>Если вы считаете, что это ошибка:</p>");
            out.println("                <a href='/logout' class='logout-link'>🔓 Выйти и войти под другой учетной записью</a>");
            out.println("            </div>");
        }

        out.println("        </div>");
        out.println("    </div>");
        out.println("</main>");

        out.println("</div>");
        renderFooter(out);
        endHtml(out);
    }

    /**
     * Рендеринг страницы с ошибкой
     */
    public static void renderErrorPage(PrintWriter out, HttpServletRequest req,
                                       String title, String message) {
        User user = getUserFromSession(req);
        String userRole = user != null ? user.getUserType() : "GUEST";
        String username = user != null ? user.getLogin() : "Гость";

        startHtml(out, title);
        renderHeader(out, username, userRole);

        out.println("<div class='layout-container'>");
        if (user != null) {
            renderRoleBasedSidebar(out, userRole, "error");
        }

        out.println("<main class='main-content'>");
        out.println("    <div class='container centered'>");
        out.println("        <div class='error-container'>");
        out.println("            <div class='error-icon'>️</div>");
        out.println("            <h1>" + title + "</h1>");
        out.println("            <p class='error-message'>" + message + "</p>");
        out.println("            <div class='action-buttons'>");
        out.println("                <a href='/' class='btn btn-primary'> На главную</a>");
        if (user != null) {
            out.println("                <a href='" + getDashboardLinkByRole(userRole) + "' class='btn btn-secondary'> Вернуться</a>");
        }
        out.println("            </div>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("</main>");

        out.println("</div>");
        renderFooter(out);
        endHtml(out);
    }


    /**
     * Получает пользователя из сессии
     */
    private static User getUserFromSession(HttpServletRequest req) {
        if (req == null || req.getSession() == null) {
            return null;
        }
        return (User) req.getSession().getAttribute("user");
    }

    /**
     * Возвращает ссылку на панель по роли
     */
    private static String getDashboardLinkByRole(String role) {
        if (role == null) return "/";
        switch (role) {
            case "DOCTOR":
                return "/doctor";
            case "MECHANIC":
                return "/mechanic";
            case "OPERATOR":
                return "/dispatcher";
            case "DRIVER":
                return "/driver-panel";
            case "ADMIN":
                return "/admin";
            default:
                return "/";
        }
    }

    // Шапка

    /**
     * Рендеринг верхней шапки
     */
    public static void renderHeader(PrintWriter out, String username, String userRole) {
        out.println("<header class='main-header'>");
        out.println("    <div class='header-left'>");
        out.println("        <div class='logo'> ТаксиСервис</div>");

        out.println("    </div>");

        // Правая часть - информация о пользователе
        if (!"GUEST".equals(userRole)) {
            out.println("    <div class='header-right'>");
            out.println("        <div class='user-info'>");
            out.println("            <div class='user-name'>" + username + "</div>");
            out.println("            <div class='user-role'>" + getRoleDisplayName(userRole) + "</div>");
            out.println("        </div>");
            out.println("        <a href='/logout' class='logout-btn'>Выйти</a>");
            out.println("    </div>");
        } else {
            out.println("    <div class='header-right'>");
            out.println("        <a href='/login' class='login-btn'>Войти</a>");
            out.println("    </div>");
        }

        out.println("</header>");
    }

    // Боковое меню

    /**
     * Рендеринг бокового меню в зависимости от роли
     */
    public static void renderRoleBasedSidebar(PrintWriter out, String userRole, String activePage) {
        out.println("<aside class='sidebar'>");

        switch (userRole) {
            case "ADMIN":
                renderAdminSidebar(out, activePage);
                break;
            case "OPERATOR":
                renderOperatorSidebar(out, activePage);
                break;
            case "DRIVER":
                renderDriverSidebar(out, activePage);
                break;
            case "MECHANIC":
                renderMechanicSidebar(out, activePage);
                break;
            case "DOCTOR":
                renderDoctorSidebar(out, activePage);
                break;
        }

        out.println("</aside>");
    }

    // Для АДМИНА
    private static void renderAdminSidebar(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Администрирование</h3>");
        addSidebarItem(out, " Панель управления", "/admin", "admin", activePage);
        addSidebarItem(out, " Пользователи", "/admin/users", "users", activePage);
        out.println("    </div>");

        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Управление</h3>");
        addSidebarItem(out, " Заказы", "/orders", "orders", activePage);
        addSidebarItem(out, " Водители", "/drivers", "drivers", activePage);
        addSidebarItem(out, " Автомобили", "/cars", "cars", activePage);
        out.println("    </div>");

        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Техническое</h3>");
        addSidebarItem(out, " Путевые листы", "/waybills", "waybills", activePage);
        addSidebarItem(out, " Техосмотры", "/inspections", "inspections", activePage);
        addSidebarItem(out, " Медосмотры", "/medical-checks", "medical-checks", activePage);
        out.println("    </div>");

        renderCommonSidebarItems(out, activePage);
    }

    // Для ОПЕРАТОРА
    private static void renderOperatorSidebar(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Диспетчерская</h3>");
        addSidebarItem(out, " Панель", "/dispatcher", "dispatcher", activePage);
        addSidebarItem(out, " Заказы", "/orders", "orders", activePage);
        addSidebarItem(out, " Водители", "/drivers", "drivers", activePage);
        out.println("    </div>");

        renderCommonSidebarItems(out, activePage);
    }

    // для ВОДИТЕЛЯ
    private static void renderDriverSidebar(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Водитель</h3>");
        addSidebarItem(out, " Моя панель", "/driver-panel", "driver-panel", activePage);
        addSidebarItem(out, " Мои заказы", "/orders?my=true", "my-orders", activePage);
        addSidebarItem(out, " Мой профиль", "/driver-panel/profile", "profile", activePage);
        out.println("    </div>");

        renderCommonSidebarItems(out, activePage);
    }

    // Для МЕХАНИКА
    private static void renderMechanicSidebar(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Механик</h3>");
        addSidebarItem(out, " Панель", "/mechanic", "mechanic", activePage);
        addSidebarItem(out, " Водители", "/drivers", "drivers", activePage); // ← ДОБАВЬ ЭТУ СТРОЧКУ
        addSidebarItem(out, " Автомобили", "/cars", "cars", activePage);
        addSidebarItem(out, " Техосмотры", "/inspections", "inspections", activePage);
        addSidebarItem(out, " Путевые листы", "/waybills", "waybills", activePage);
        out.println("    </div>");

        renderCommonSidebarItems(out, activePage);
    }

    // Для ДОКТОРА
    private static void renderDoctorSidebar(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Медицинский отдел</h3>");
        addSidebarItem(out, " Панель", "/doctor", "doctor", activePage);
        addSidebarItem(out, " Медосмотры", "/medical-checks", "medical-checks", activePage);
        addSidebarItem(out, " Водители", "/drivers", "drivers", activePage);
        out.println("    </div>");

        renderCommonSidebarItems(out, activePage);
    }

    /**
     * Добавляет пункт в боковое меню
     */
    private static void addSidebarItem(PrintWriter out, String title, String link,
                                       String pageId, String activePage) {
        boolean isActive = pageId.equals(activePage);
        String cssClass = isActive ? "sidebar-item active" : "sidebar-item";

        out.println("        <a href='" + link + "' class='" + cssClass + "'>" + title + "</a>");
    }

    /**
     * Получает отображаемое имя роли
     */
    private static String getRoleDisplayName(String role) {
        switch (role) {
            case "ADMIN":
                return "Администратор";
            case "OPERATOR":
                return "Диспетчер";
            case "DRIVER":
                return "Водитель";
            case "MECHANIC":
                return "Механик";
            case "DOCTOR":
                return "Врач";
            default:
                return "Гость";
        }
    }

    /**
     * Получает основную ссылку по роли
     */
    private static String getMainLinkByRole(String role) {
        switch (role) {
            case "ADMIN":
                return "/admin";
            case "OPERATOR":
                return "/dispatcher";
            case "DRIVER":
                return "/driver-panel";
            case "MECHANIC":
                return "/mechanic";
            case "DOCTOR":
                return "/doctor";
            default:
                return "/";
        }
    }

//    /**
//     * Получает название основной кнопки по роли
//     */
//    private static String getMainTitleByRole(String role) {
//        switch (role) {
//            case "ADMIN":
//                return " Админ";
//            case "OPERATOR":
//                return " Диспетчер";
//            case "DRIVER":
//                return " Водитель";
//            case "MECHANIC":
//                return " Механик";
//            case "DOCTOR":
//                return " Врач";
//            default:
//                return "Главная";
//        }
//    }


    /**
     * Начало HTML документа
     */
    public static void startHtml(PrintWriter out, String title) {
        out.println("<!DOCTYPE html>");
        out.println("<html lang='ru'>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <title>" + title + " | ТаксиСервис</title>");
        out.println("    <style>");
        out.println(getCommonStyles());
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
    }

    /**
     * Общие стили
     */
    public static String getCommonStyles() {
        return """
                    /*  ОСНОВНЫЕ СТИЛИ  */
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                
                    body {
                        font-family: 'Segoe UI', 'Roboto', 'Arial', sans-serif;
                        background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
                        color: #e0e0e0;
                        min-height: 100vh;
                        line-height: 1.6;
                    }
                
                    /*  ШАПКА  */
                    .main-header {
                        background: rgba(25, 25, 25, 0.95);
                        backdrop-filter: blur(10px);
                        padding: 15px 30px;
                        border-bottom: 1px solid #333;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        position: sticky;
                        top: 0;
                        z-index: 1000;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
                    }
                
                    .header-left {
                        display: flex;
                        align-items: center;
                        gap: 30px;
                    }
                
                    .logo {
                        color: #fff;
                        font-size: 1.5em;
                        font-weight: 600;
                        letter-spacing: 1px;
                    }
                
                    .header-nav {
                        display: flex;
                        gap: 15px;
                        align-items: center;
                    }
                
                    .nav-link {
                        color: #ccc;
                        text-decoration: none;
                        padding: 8px 15px;
                        border-radius: 6px;
                        transition: all 0.3s;
                        font-size: 0.95em;
                        border: 1px solid transparent;
                    }
                
                    .nav-link:hover {
                        background: rgba(255, 255, 255, 0.05);
                        color: #fff;
                        border-color: #444;
                    }
                
                    .header-right {
                        display: flex;
                        align-items: center;
                        gap: 20px;
                    }
                
                    .user-info {
                        text-align: right;
                    }
                
                    .user-name {
                        color: #fff;
                        font-weight: 500;
                        font-size: 0.95em;
                    }
                
                    .user-role {
                        color: #888;
                        font-size: 0.85em;
                        margin-top: 2px;
                    }
                
                    .logout-btn, .login-btn {
                        color: #ff6b6b;
                        text-decoration: none;
                        padding: 8px 20px;
                        border-radius: 6px;
                        background: rgba(255, 107, 107, 0.1);
                        border: 1px solid rgba(255, 107, 107, 0.3);
                        transition: all 0.3s;
                        font-size: 0.9em;
                    }
                
                    .logout-btn:hover, .login-btn:hover {
                        background: rgba(255, 107, 107, 0.2);
                        color: #ff5252;
                    }
                
                    /*  СТИЛИ ДЛЯ ЗАКАЗОВ В ПУТЕВОМ ЛИСТЕ  */
                            .orders-list {
                                margin-top: 20px;
                            }
                
                            .order-item {
                                background: rgba(255, 255, 255, 0.05);
                                border-radius: 8px;
                                padding: 15px;
                                margin-bottom: 15px;
                                border-left: 4px solid #2196f3;
                                transition: all 0.3s ease;
                            }
                
                            .order-item:hover {
                                background: rgba(255, 255, 255, 0.08);
                                transform: translateY(-2px);
                                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
                            }
                
                            .order-header {
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                margin-bottom: 10px;
                            }
                
                            .order-time {
                                color: #888;
                                font-size: 0.9em;
                            }
                
                            .order-route {
                                color: #ccc;
                                margin-bottom: 10px;
                                font-size: 0.95em;
                                line-height: 1.5;
                            }
                
                            .order-stats {
                                display: flex;
                                gap: 20px;
                                font-size: 0.9em;
                            }
                
                            .order-price {
                                color: #4caf50;
                                font-weight: 500;
                            }
                
                            .order-distance {
                                color: #2196f3;
                                font-weight: 500;
                            }
                
                            .waybill-totals {
                                background: rgba(255, 255, 255, 0.03);
                                border-radius: 8px;
                                padding: 20px;
                                border: 1px solid #333;
                                margin-top: 20px;
                            }
                
                            .waybill-totals h4 {
                                color: #fff;
                                margin-bottom: 15px;
                                font-size: 1.1em;
                            }
                
                            .totals-grid {
                                display: grid;
                                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                                gap: 15px;
                                margin-top: 15px;
                            }
                
                            .total-item {
                                text-align: center;
                                padding: 15px;
                                background: rgba(40, 40, 40, 0.8);
                                border-radius: 8px;
                                border: 1px solid #333;
                                transition: all 0.3s ease;
                            }
                
                            .total-item:hover {
                                background: rgba(40, 40, 40, 0.9);
                                transform: translateY(-2px);
                                box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
                            }
                
                            .total-label {
                                color: #888;
                                font-size: 0.85em;
                                margin-bottom: 5px;
                                text-transform: uppercase;
                                letter-spacing: 0.5px;
                            }
                
                            .total-value {
                                color: #fff;
                                font-size: 1.5em;
                                font-weight: 500;
                            }
                
                    /*  МАКЕТ САЙТА  */
                    .layout-container {
                        display: flex;
                        min-height: calc(100vh - 70px);
                    }
                
                    /*  БОКОВОЕ МЕНЮ  */
                    .sidebar {
                        width: 260px;
                        background: rgba(30, 30, 30, 0.9);
                        border-right: 1px solid #333;
                        padding: 25px 0;
                        flex-shrink: 0;
                        backdrop-filter: blur(10px);
                    }
                
                    .sidebar-section {
                        margin-bottom: 30px;
                        padding: 0 20px;
                    }
                
                    .sidebar-section:last-child {
                        margin-bottom: 0;
                    }
                
                    .sidebar-section h3 {
                        color: #888;
                        font-size: 0.8em;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 15px;
                        padding-bottom: 10px;
                        border-bottom: 1px solid #333;
                    }
                
                    .sidebar-item {
                        display: block;
                        padding: 12px 15px;
                        color: #ccc;
                        text-decoration: none;
                        border-radius: 8px;
                        margin-bottom: 5px;
                        transition: all 0.3s;
                        font-size: 0.95em;
                        border-left: 3px solid transparent;
                    }
                
                    .sidebar-item:hover {
                        background: rgba(255, 255, 255, 0.05);
                        color: #fff;
                        border-left-color: #444;
                    }
                
                    .sidebar-item.active {
                        background: linear-gradient(135deg, rgba(33, 150, 243, 0.15) 0%, rgba(25, 118, 210, 0.15) 100%);
                        color: #64b5f6;
                        border-left-color: #2196f3;
                        font-weight: 500;
                    }
                
                    /*  ОСНОВНОЙ КОНТЕНТ  */
                    .main-content {
                        flex: 1;
                        padding: 30px;
                        background: rgba(10, 10, 10, 0.5);
                        min-height: calc(100vh - 70px);
                    }
                
                    /*  СТРАНИЦЫ ОШИБОК  */
                    .container.centered {
                        max-width: 600px;
                        margin: 100px auto;
                        text-align: center;
                    }
                
                    .error-container {
                        background: rgba(45, 45, 45, 0.8);
                        border-radius: 12px;
                        padding: 40px;
                        border: 1px solid #333;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
                    }
                
                    .error-icon {
                        font-size: 80px;
                        margin-bottom: 20px;
                        color: #ff9800;
                    }
                
                    .error-container h1 {
                        color: #fff;
                        margin-bottom: 20px;
                        font-size: 2em;
                    }
                
                    .error-message {
                        color: #aaa;
                        margin-bottom: 30px;
                        font-size: 1.1em;
                        line-height: 1.5;
                    }
                
                    .info-box {
                        background: rgba(255, 68, 68, 0.1);
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 30px;
                        border: 1px solid rgba(255, 68, 68, 0.3);
                    }
                
                    .info-box p {
                        color: #ff8888;
                        margin: 5px 0;
                    }
                
                    .logout-hint {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #333;
                    }
                
                    .logout-hint p {
                        color: #888;
                        font-size: 14px;
                        margin-bottom: 10px;
                    }
                
                    .logout-link {
                        color: #64b5f6;
                        text-decoration: none;
                        font-size: 14px;
                    }
                
                    .logout-link:hover {
                        text-decoration: underline;
                    }
                
                    /*  КАРТОЧКИ  */
                    .card {
                        background: rgba(45, 45, 45, 0.8);
                        border-radius: 12px;
                        padding: 25px;
                        margin-bottom: 25px;
                        border: 1px solid #333;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
                        backdrop-filter: blur(10px);
                    }
                
                    .card-header {
                        margin-bottom: 20px;
                        padding-bottom: 15px;
                        border-bottom: 1px solid #333;
                    }
                
                    .card-title {
                        color: #fff;
                        font-size: 1.4em;
                        font-weight: 400;
                        margin: 0;
                    }
                
                    /*  ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ  */
                    .action-buttons { display: flex; gap: 10px; flex-wrap: wrap; }
                    .action-buttons-small { display: flex; gap: 5px; }
                    .page-title { color: #fff; font-size: 2em; margin-bottom: 5px; }
                    .page-subtitle { color: #888; margin-bottom: 20px; }
                    .empty-state { text-align: center; padding: 40px; }
                    .empty-icon { font-size: 60px; margin-bottom: 20px; }
                    .info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
                    .info-section { background: rgba(255, 255, 255, 0.03); padding: 20px; border-radius: 8px; border: 1px solid #333; }
                    .required { color: #f44336; }
                    .form-hint { color: #888; font-size: 0.9em; margin-top: 5px; }
                    .form-actions { display: flex; gap: 10px; margin-top: 30px; }
                
                    /* ТАБЛИЦЫ  */
                    .table-container {
                        overflow-x: auto;
                        border-radius: 8px;
                        border: 1px solid #333;
                        background: rgba(40, 40, 40, 0.8);
                    }
                
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        min-width: 600px;
                    }
                
                    th {
                        background: rgba(50, 50, 50, 0.9);
                        color: #aaa;
                        text-align: left;
                        padding: 15px;
                        font-weight: 500;
                        font-size: 0.9em;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        border-bottom: 1px solid #333;
                    }
                
                    td {
                        padding: 15px;
                        border-bottom: 1px solid #333;
                        color: #ccc;
                    }
                
                    tr:hover {
                        background: rgba(255, 255, 255, 0.02);
                    }
                
                    /*  КНОПКИ  */
                    .btn {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                        padding: 10px 20px;
                        background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        cursor: pointer;
                        font-size: 0.95em;
                        text-decoration: none;
                        transition: all 0.3s;
                        font-weight: 500;
                    }
                
                    .btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(33, 150, 243, 0.3);
                    }
                
                    .btn-sm {
                        padding: 6px 12px;
                        font-size: 0.85em;
                    }
                
                    .btn-success {
                        background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
                    }
                
                    .btn-warning {
                        background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
                    }
                
                    .btn-danger {
                        background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%);
                    }
                
                    .btn-secondary {
                        background: linear-gradient(135deg, #666 0%, #444 100%);
                    }
                
                    .btn-info {
                        background: linear-gradient(135deg, #00bcd4 0%, #0097a7 100%);
                    }
                
                    .btn-primary {
                        background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
                    }
                
                    /*  ФОРМЫ  */
                    .form-group {
                        margin-bottom: 20px;
                    }
                
                    .form-label {
                        display: block;
                        margin-bottom: 8px;
                        color: #aaa;
                        font-size: 0.9em;
                        font-weight: 500;
                    }
                
                    .form-control {
                        width: 100%;
                        padding: 12px 15px;
                        background: rgba(255, 255, 255, 0.05);
                        border: 1px solid #444;
                        border-radius: 8px;
                        color: #fff;
                        font-size: 1em;
                        transition: all 0.3s;
                    }
                
                    .form-control:focus {
                        outline: none;
                        border-color: #2196f3;
                        background: rgba(255, 255, 255, 0.08);
                        box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
                    }
                
                    /*  БЕЙДЖИ  */
                    .badge {
                        display: inline-block;
                        padding: 4px 12px;
                        border-radius: 20px;
                        font-size: 0.8em;
                        font-weight: 500;
                        letter-spacing: 0.3px;
                    }
                
                    .badge-success {
                        background: rgba(76, 175, 80, 0.15);
                        color: #4caf50;
                        border: 1px solid #4caf50;
                    }
                
                    .badge-warning {
                        background: rgba(255, 152, 0, 0.15);
                        color: #ff9800;
                        border: 1px solid #ff9800;
                    }
                
                    .badge-danger {
                        background: rgba(244, 67, 54, 0.15);
                        color: #f44336;
                        border: 1px solid #f44336;
                    }
                
                    .badge-info {
                        background: rgba(33, 150, 243, 0.15);
                        color: #2196f3;
                        border: 1px solid #2196f3;
                    }
                
                    .badge-secondary {
                        background: rgba(158, 158, 158, 0.15);
                        color: #9e9e9e;
                        border: 1px solid #9e9e9e;
                    }
                
                    /*  СТАТИСТИКА  */
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                
                    .stat-card {
                        background: rgba(40, 40, 40, 0.8);
                        border-radius: 10px;
                        padding: 20px;
                        text-align: center;
                        border: 1px solid #333;
                        transition: all 0.3s;
                    }
                
                    .stat-card:hover {
                        transform: translateY(-5px);
                        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
                    }
                
                    .stat-icon {
                        font-size: 2em;
                        margin-bottom: 10px;
                    }
                
                    .stat-value {
                        font-size: 2.2em;
                        font-weight: 300;
                        margin: 10px 0;
                        color: #fff;
                    }
                
                    .stat-label {
                        color: #888;
                        font-size: 0.9em;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                
                    /*  ПОДВАЛ  */
                    .main-footer {
                        background: rgba(20, 20, 20, 0.9);
                        padding: 20px 30px;
                        border-top: 1px solid #333;
                        text-align: center;
                        color: #666;
                        font-size: 0.9em;
                        margin-top: auto;
                    }
                
                    /*  УТИЛИТЫ  */
                    .text-center { text-align: center; }
                    .text-right { text-align: right; }
                    .mb-20 { margin-bottom: 20px; }
                    .mb-30 { margin-bottom: 30px; }
                    .mt-20 { margin-top: 20px; }
                    .mt-30 { margin-top: 30px; }
                
                    /*  АДАПТИВНОСТЬ  */
                    @media (max-width: 1024px) {
                        .sidebar {
                            width: 220px;
                        }
                
                        .main-header {
                            padding: 15px 20px;
                        }
                
                        .main-content {
                            padding: 20px;
                        }
                    }
                
                    @media (max-width: 768px) {
                        .layout-container {
                            flex-direction: column;
                        }
                
                        .sidebar {
                            width: 100%;
                            position: static;
                            border-right: none;
                            border-bottom: 1px solid #333;
                            padding: 15px 0;
                        }
                
                        .sidebar-section {
                            padding: 0 15px;
                        }
                
                        .main-header {
                            flex-direction: column;
                            gap: 15px;
                            padding: 15px;
                        }
                
                        .header-left, .header-right {
                            width: 100%;
                            justify-content: center;
                        }
                
                        .header-nav {
                            flex-wrap: wrap;
                            justify-content: center;
                        }
                
                        .stats-grid {
                            grid-template-columns: 1fr;
                        }
                
                        .table-container {
                            border-radius: 0;
                            border-left: none;
                            border-right: none;
                        }
                
                        .container.centered {
                            margin: 50px auto;
                            padding: 15px;
                        }
                    }
                
                    /*  АНИМАЦИИ  */
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                
                    .fade-in {
                        animation: fadeIn 0.5s ease-out;
                    }
                
                    /* Стиль для активной кнопки фильтра */
                            .btn-secondary.active {
                                background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
                                color: white;
                                border: 1px solid #2196f3;
                            }
                """;
    }

    // Подвал

    /**
     * Рендерит подвал
     */
    public static void renderFooter(PrintWriter out) {
        out.println("<footer class='main-footer'>");
        out.println("    <div>© 2025 ТаксиСервис • Информационно-справочная система Такси </div>");
        out.println("    <div style='margin-top: 10px; color: #444; font-size: 0.85em;'>");
        out.println("        " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        out.println("    </div>");
        out.println("</footer>");
    }

    /**
     * Завершает HTML документ
     */
    public static void endHtml(PrintWriter out) {
        out.println("</body>");
        out.println("</html>");
    }

    /**
     * Генерирует HTML для карточки
     */
    public static String generateCard(String title, String content, String cssClass) {
        return "<div class='card " + (cssClass != null ? cssClass : "") + "'>" +
                "<div class='card-header'><h3 class='card-title'>" + title + "</h3></div>" +
                content +
                "</div>";
    }

    /**
     * Генерирует HTML для кнопки
     */
    public static String generateButton(String text, String url, String type) {
        String btnClass = "btn";
        if (type != null) {
            btnClass += " btn-" + type;
        }
        return "<a href='" + url + "' class='" + btnClass + "'>" + text + "</a>";
    }

    private static void renderCommonSidebarItems(PrintWriter out, String activePage) {
        out.println("    <div class='sidebar-section'>");
        out.println("        <h3> Информация</h3>");
        addSidebarItem(out, " Об авторе", "/about", "about", activePage);
        out.println("    </div>");
    }
}
