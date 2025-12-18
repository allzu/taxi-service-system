package com.taxi.controller;

import com.taxi.entity.User;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.PrintWriter;

public abstract class BaseServlet extends HttpServlet {

    protected User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    protected String getUserRole(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user != null ? user.getUserType() : null;
    }

    protected void renderHeader(PrintWriter out, String title, HttpServletRequest request) {
        User user = getCurrentUser(request);

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Такси-сервис | " + title + "</title>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <style>");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { ");
        out.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
        out.println("            background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); ");
        out.println("            color: #e0e0e0; ");
        out.println("            min-height: 100vh; ");
        out.println("        }");
        out.println("        .header { ");
        out.println("            background: rgba(30, 30, 30, 0.95); ");
        out.println("            padding: 20px 40px; ");
        out.println("            border-bottom: 1px solid #333; ");
        out.println("            display: flex; ");
        out.println("            justify-content: space-between; ");
        out.println("            align-items: center; ");
        out.println("            backdrop-filter: blur(10px); ");
        out.println("            position: fixed; ");
        out.println("            top: 0; ");
        out.println("            left: 0; ");
        out.println("            right: 0; ");
        out.println("            z-index: 1000; ");
        out.println("            heigh% 70px; ");
        out.println("        }");
        out.println("        .logo { ");
        out.println("            color: #fff; ");
        out.println("            font-size: 24px; ");
        out.println("            font-weight: 300; ");
        out.println("        }");
        out.println("        .user-info { ");
        out.println("            display: flex; ");
        out.println("            align-items: center; ");
        out.println("            gap: 20px; ");
        out.println("        }");
        out.println("        .user-name { ");
        out.println("            color: #aaa; ");
        out.println("        }");
        out.println("        .user-role { ");
        out.println("            background: rgba(255, 255, 255, 0.1); ");
        out.println("            padding: 5px 15px; ");
        out.println("            border-radius: 20px; ");
        out.println("            font-size: 12px; ");
        out.println("            color: #888; ");
        out.println("        }");
        out.println("        .logout-btn { ");
        out.println("            background: rgba(255, 255, 255, 0.05); ");
        out.println("            border: 1px solid #444; ");
        out.println("            color: #aaa; ");
        out.println("            padding: 8px 20px; ");
        out.println("            border-radius: 5px; ");
        out.println("            text-decoration: none; ");
        out.println("            font-size: 14px; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .logout-btn:hover { ");
        out.println("            background: rgba(255, 255, 255, 0.1); ");
        out.println("            border-color: #666; ");
        out.println("            color: #fff; ");
        out.println("        }");
        out.println("        .sidebar { ");
        out.println("            width: 250px; ");
        out.println("            background: rgba(25, 25, 25, 0.9); ");
        out.println("            height: calc(100vh - 70px); ");
        out.println("            position: fixed; ");
        out.println("            padding: 20px; ");
        out.println("            border-right: 1px solid #333; ");
        out.println("            top: 70px; ");
        out.println("            left: 0; ");
        out.println("            z-index: 999; ");
        out.println("        }");
        out.println("        .nav-menu { ");
        out.println("            list-style: none; ");
        out.println("        }");
        out.println("        .nav-item { ");
        out.println("            margin-bottom: 10px; ");
        out.println("        }");
        out.println("        .nav-link { ");
        out.println("            display: block; ");
        out.println("            padding: 12px 15px; ");
        out.println("            color: #aaa; ");
        out.println("            text-decoration: none; ");
        out.println("            border-radius: 8px; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .nav-link:hover { ");
        out.println("            background: rgba(255, 255, 255, 0.05); ");
        out.println("            color: #fff; ");
        out.println("        }");
        out.println("        .nav-link.active { ");
        out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
        out.println("            color: #fff; ");
        out.println("        }");
        out.println("        .content { ");
        out.println("            margin-left: 250px; ");
        out.println("            padding: 40px; ");
        out.println("            margin-top: 70px; ");
        out.println("            min-height: calc(100vh - 70px) ");
        out.println("        }");
        out.println("        .page-title { ");
        out.println("            color: #fff; ");
        out.println("            font-size: 32px; ");
        out.println("            font-weight: 300; ");
        out.println("            margin-bottom: 30px; ");
        out.println("        }");
        out.println("        .card { ");
        out.println("            background: rgba(30, 30, 30, 0.8); ");
        out.println("            border-radius: 15px; ");
        out.println("            padding: 30px; ");
        out.println("            margin-bottom: 20px; ");
        out.println("            border: 1px solid #333; ");
        out.println("            backdrop-filter: blur(10px); ");
        out.println("        }");
        out.println("        .btn { ");
        out.println("            display: inline-block; ");
        out.println("            padding: 12px 25px; ");
        out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
        out.println("            color: white; ");
        out.println("            border: none; ");
        out.println("            border-radius: 8px; ");
        out.println("            text-decoration: none; ");
        out.println("            font-size: 14px; ");
        out.println("            font-weight: 500; ");
        out.println("            cursor: pointer; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .btn:hover { ");
        out.println("            background: linear-gradient(135deg, #1976d2 0%, #0d47a1 100%); ");
        out.println("            transform: translateY(-2px); ");
        out.println("            box-shadow: 0 5px 15px rgba(33, 150, 243, 0.3); ");
        out.println("        }");
        out.println("        .btn-secondary { ");
        out.println("            background: linear-gradient(135deg, #757575 0%, #616161 100%); ");
        out.println("        }");
        out.println("        .btn-secondary:hover { ");
        out.println("            background: linear-gradient(135deg, #616161 0%, #424242 100%); ");
        out.println("        }");
        out.println("        table { ");
        out.println("            width: 100%; ");
        out.println("            border-collapse: collapse; ");
        out.println("            margin: 20px 0; ");
        out.println("        }");
        out.println("        th { ");
        out.println("            background: rgba(255, 255, 255, 0.05); ");
        out.println("            padding: 15px; ");
        out.println("            text-align: left; ");
        out.println("            color: #aaa; ");
        out.println("            font-weight: 500; ");
        out.println("            border-bottom: 1px solid #333; ");
        out.println("        }");
        out.println("        td { ");
        out.println("            padding: 15px; ");
        out.println("            border-bottom: 1px solid rgba(255, 255, 255, 0.05); ");
        out.println("        }");
        out.println("        tr:hover { ");
        out.println("            background: rgba(255, 255, 255, 0.02); ");
        out.println("        }");
        out.println("        .form-group { ");
        out.println("            margin-bottom: 20px; ");
        out.println("        }");
        out.println("        .form-group label { ");
        out.println("            display: block; ");
        out.println("            color: #aaa; ");
        out.println("            margin-bottom: 8px; ");
        out.println("            font-size: 14px; ");
        out.println("        }");
        out.println("        .form-control { ");
        out.println("            width: 100%; ");
        out.println("            padding: 12px 15px; ");
        out.println("            background: rgba(255, 255, 255, 0.05); ");
        out.println("            border: 1px solid #444; ");
        out.println("            border-radius: 8px; ");
        out.println("            color: #fff; ");
        out.println("            font-size: 16px; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .form-control:focus { ");
        out.println("            outline: none; ");
        out.println("            border-color: #666; ");
        out.println("            background: rgba(255, 255, 255, 0.1); ");
        out.println("        }");
        out.println("        .alert { ");
        out.println("            padding: 15px; ");
        out.println("            margin: 20px 0; ");
        out.println("            border-radius: 8px; ");
        out.println("        }");
        out.println("        .alert-success { ");
        out.println("            background: rgba(46, 125, 50, 0.2); ");
        out.println("            color: #81c784; ");
        out.println("            border: 1px solid #2e7d32; ");
        out.println("        }");
        out.println("        .alert-danger { ");
        out.println("            background: rgba(211, 47, 47, 0.2); ");
        out.println("            color: #e57373; ");
        out.println("            border: 1px solid #d32f2f; ");
        out.println("        }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");

        // Шапка
        out.println("    <div class='header'>");
        out.println("        <div class='logo'>Такси-сервис</div>");
        if (user != null) {
            out.println("        <div class='user-info'>");
            out.println("            <div class='user-name'>" + user.getFullName() + "</div>");
            out.println("            <div class='user-role'>" + getRoleDisplay(user.getUserType()) + "</div>");
            out.println("            <a href='/logout' class='logout-btn'>Выйти</a>");
            out.println("        </div>");
        }
        out.println("    </div>");
    }

    protected void renderFooter(PrintWriter out) {
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }

    protected void renderSidebar(PrintWriter out, String currentPage, String userRole) {
        out.println("    <div class='sidebar'>");
        out.println("        <ul class='nav-menu'>");

        // Главная страница (ссылка на панель по роли)
        String dashboardLink = getDashboardByRole(userRole);
        String dashboardText = getDashboardTextByRole(userRole);
        out.println("            <li class='nav-item'><a href='" + dashboardLink + "' class='nav-link" +
                (currentPage.equals("dashboard") ? " active" : "") + "'>" + dashboardText + "</a></li>");

        // Ссылки по ролям
        if ("DOCTOR".equals(userRole)) {
            out.println("            <li class='nav-item'><a href='/medical-checks' class='nav-link" +
                    (currentPage.equals("medical-checks") ? " active" : "") + "'>Медосмотры</a></li>");
            out.println("            <li class='nav-item'><a href='/drivers' class='nav-link" +
                    (currentPage.equals("drivers") ? " active" : "") + "'>Водители</a></li>");
        }
        else if ("MECHANIC".equals(userRole)) {
            out.println("            <li class='nav-item'><a href='/inspections' class='nav-link" +
                    (currentPage.equals("inspections") ? " active" : "") + "'>Техосмотры</a></li>");
            out.println("            <li class='nav-item'><a href='/cars' class='nav-link" +
                    (currentPage.equals("cars") ? " active" : "") + "'>Автомобили</a></li>");
            out.println("            <li class='nav-item'><a href='/drivers' class='nav-link" +  // ← ДОБАВЬ ЭТУ СТРОЧКУ
                    (currentPage.equals("drivers") ? " active" : "") + "'>Водители</a></li>");    // ← ДОБАВЬ ЭТУ СТРОЧКУ
            out.println("            <li class='nav-item'><a href='/waybills' class='nav-link" +
                    (currentPage.equals("waybills") ? " active" : "") + "'>Путевые листы</a></li>");
        }
        else if ("OPERATOR".equals(userRole)) {
            out.println("            <li class='nav-item'><a href='/orders' class='nav-link" +
                    (currentPage.equals("orders") ? " active" : "") + "'>Заказы</a></li>");
            out.println("            <li class='nav-item'><a href='/drivers' class='nav-link" +
                    (currentPage.equals("drivers") ? " active" : "") + "'>Водители</a></li>");
            out.println("            <li class='nav-item'><a href='/waybills' class='nav-link" +
                    (currentPage.equals("waybills") ? " active" : "") + "'>Путевые листы</a></li>");
        }
        else if ("DRIVER".equals(userRole)) {
            out.println("            <li class='nav-item'><a href='/orders?my=true' class='nav-link" +
                    (currentPage.equals("orders") ? " active" : "") + "'>Мои заказы</a></li>");
            out.println("            <li class='nav-item'><a href='/waybills' class='nav-link" +
                    (currentPage.equals("waybills") ? " active" : "") + "'>Мои смены</a></li>");
            out.println("            <li class='nav-item'><a href='/driver-panel/profile' class='nav-link" +
                    (currentPage.equals("profile") ? " active" : "") + "'>Мой профиль</a></li>");
        }
        else if ("ADMIN".equals(userRole)) {
            out.println("            <li class='nav-item'><a href='/admin/users' class='nav-link" +
                    (currentPage.equals("users") ? " active" : "") + "'>Пользователи</a></li>");
            out.println("            <li class='nav-item'><a href='/orders' class='nav-link" +
                    (currentPage.equals("orders") ? " active" : "") + "'>Заказы</a></li>");
            out.println("            <li class='nav-item'><a href='/drivers' class='nav-link" +
                    (currentPage.equals("drivers") ? " active" : "") + "'>Водители</a></li>");
            out.println("            <li class='nav-item'><a href='/cars' class='nav-link" +
                    (currentPage.equals("cars") ? " active" : "") + "'>Автомобили</a></li>");
            out.println("            <li class='nav-item'><a href='/waybills' class='nav-link" +
                    (currentPage.equals("waybills") ? " active" : "") + "'>Путевые листы</a></li>");
            out.println("            <li class='nav-item'><a href='/inspections' class='nav-link" +
                    (currentPage.equals("inspections") ? " active" : "") + "'>Техосмотры</a></li>");
            out.println("            <li class='nav-item'><a href='/medical-checks' class='nav-link" +
                    (currentPage.equals("medical-checks") ? " active" : "") + "'>Медосмотры</a></li>");
        }

        out.println("        </ul>");
        out.println("    </div>");
        out.println("    <div class='content'>");
    }

    private String getRoleDisplay(String role) {
        switch (role) {
            case "DOCTOR": return "Врач";
            case "MECHANIC": return "Механик";
            case "OPERATOR": return "Диспетчер";
            case "DRIVER": return "Водитель";
            case "ADMIN": return "Администратор";
            default: return "Пользователь";
        }
    }

    private String getDashboardByRole(String role) {
        switch (role) {
            case "DOCTOR": return "/doctor";
            case "MECHANIC": return "/mechanic";
            case "OPERATOR": return "/dispatcher";
            case "DRIVER": return "/driver-panel";
            case "ADMIN": return "/admin";
            default: return "/";
        }
    }

    private String getDashboardTextByRole(String role) {
        switch (role) {
            case "DOCTOR": return "Панель мед.работника";
            case "MECHANIC": return "Панель механика";
            case "OPERATOR": return "Панель диспетчера";
            case "DRIVER": return "Моя панель";
            case "ADMIN": return "Панель администратора";
            default: return "Главная";
        }
    }

    protected boolean checkRole(HttpServletRequest request, String... allowedRoles) {
        String userRole = getUserRole(request);
        if (userRole == null) return false;

        for (String allowedRole : allowedRoles) {
            if (userRole.equals(allowedRole)) {
                return true;
            }
        }
        return false;
    }
}