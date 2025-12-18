package com.taxi.controller;

import com.taxi.entity.User;
import com.taxi.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

public class AuthServlet extends HttpServlet {
    private UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.equals("/logout")) {
            // Выход из системы
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Показываем страницу входа
        showLoginPage(req, resp, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.equals("/login")) {
            processLogin(req, resp);
        } else if (path.equals("/register")) {
            processRegistration(req, resp);
        }
    }

    private void processLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String login = req.getParameter("login");
        String password = req.getParameter("password");

        User user = userService.authenticate(login, password);

        if (user != null && user.getIsActive()) {
            // Создаем сессию
            HttpSession session = req.getSession();
            session.setAttribute("user", user);
            session.setAttribute("userRole", user.getUserType());

            // Перенаправляем на панель по роли
            String redirectPath = getDashboardByRole(user.getUserType());
            resp.sendRedirect(req.getContextPath() + redirectPath);
        } else {
            showLoginPage(req, resp, "Неверный логин или пароль");
        }
    }

    private void processRegistration(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String fullName = req.getParameter("fullName");
        String login = req.getParameter("login");
        String password = req.getParameter("password");
        String userType = req.getParameter("userType");

        // Проверка существования пользователя
        if (userService.findByLogin(login) != null) {
            showRegisterPage(req, resp, "Пользователь с таким логином уже существует");
            return;
        }

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setLogin(login);
        newUser.setPassword(password);
        newUser.setUserType(userType);
        newUser.setIsActive(true);

        // Генерируем уникальный код доступа
        newUser.setAccessCode((int) (Math.random() * 9000) + 1000);

        userService.save(newUser);

        showLoginPage(req, resp, "Регистрация успешна. Теперь вы можете войти в систему.");
    }

    private String getDashboardByRole(String role) {
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

    private void showLoginPage(HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Такси-сервис | Вход</title>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <style>");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { ");
        out.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
        out.println("            background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); ");
        out.println("            min-height: 100vh; ");
        out.println("            display: flex; ");
        out.println("            align-items: center; ");
        out.println("            justify-content: center; ");
        out.println("            padding: 20px; ");
        out.println("        }");
        out.println("        .login-container { ");
        out.println("            width: 100%; ");
        out.println("            max-width: 400px; ");
        out.println("            background: rgba(30, 30, 30, 0.95); ");
        out.println("            border-radius: 20px; ");
        out.println("            padding: 40px; ");
        out.println("            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5); ");
        out.println("            border: 1px solid rgba(255, 255, 255, 0.1); ");
        out.println("            backdrop-filter: blur(10px); ");
        out.println("        }");
        out.println("        .logo { ");
        out.println("            text-align: center; ");
        out.println("            margin-bottom: 30px; ");
        out.println("        }");
        out.println("        .logo h1 { ");
        out.println("            color: #ffffff; ");
        out.println("            font-size: 28px; ");
        out.println("            font-weight: 300; ");
        out.println("            letter-spacing: 1px; ");
        out.println("        }");
        out.println("        .logo p { ");
        out.println("            color: #888; ");
        out.println("            font-size: 14px; ");
        out.println("            margin-top: 5px; ");
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
        out.println("        .form-group input { ");
        out.println("            width: 100%; ");
        out.println("            padding: 12px 15px; ");
        out.println("            background: rgba(255, 255, 255, 0.05); ");
        out.println("            border: 1px solid #444; ");
        out.println("            border-radius: 8px; ");
        out.println("            color: #fff; ");
        out.println("            font-size: 16px; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .form-group input:focus { ");
        out.println("            outline: none; ");
        out.println("            border-color: #666; ");
        out.println("            background: rgba(255, 255, 255, 0.1); ");
        out.println("        }");
        out.println("        .btn { ");
        out.println("            width: 100%; ");
        out.println("            padding: 14px; ");
        out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
        out.println("            color: white; ");
        out.println("            border: none; ");
        out.println("            border-radius: 8px; ");
        out.println("            font-size: 16px; ");
        out.println("            font-weight: 500; ");
        out.println("            cursor: pointer; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .btn:hover { ");
        out.println("            background: linear-gradient(135deg, #1976d2 0%, #0d47a1 100%); ");
        out.println("            transform: translateY(-2px); ");
        out.println("            box-shadow: 0 5px 15px rgba(33, 150, 243, 0.3); ");
        out.println("        }");
        out.println("        .message { ");
        out.println("            padding: 12px; ");
        out.println("            margin: 20px 0; ");
        out.println("            border-radius: 8px; ");
        out.println("            text-align: center; ");
        out.println("            font-size: 14px; ");
        out.println("        }");
        out.println("        .success { ");
        out.println("            background: rgba(46, 125, 50, 0.2); ");
        out.println("            color: #81c784; ");
        out.println("            border: 1px solid #2e7d32; ");
        out.println("        }");
        out.println("        .error { ");
        out.println("            background: rgba(211, 47, 47, 0.2); ");
        out.println("            color: #e57373; ");
        out.println("            border: 1px solid #d32f2f; ");
        out.println("        }");
        out.println("        .links { ");
        out.println("            text-align: center; ");
        out.println("            margin-top: 20px; ");
        out.println("        }");
        out.println("        .links a { ");
        out.println("            color: #64b5f6; ");
        out.println("            text-decoration: none; ");
        out.println("            font-size: 14px; ");
        out.println("        }");
        out.println("        .links a:hover { ");
        out.println("            text-decoration: underline; ");
        out.println("        }");
        out.println("        .register-link { ");
        out.println("            display: block; ");
        out.println("            text-align: center; ");
        out.println("            margin-top: 15px; ");
        out.println("            color: #888; ");
        out.println("            font-size: 14px; ");
        out.println("        }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='login-container'>");
        out.println("        <div class='logo'>");
        out.println("            <h1>Такси-сервис</h1>");
        out.println("            <p>Информационно-справочная система</p>");
        out.println("        </div>");

        if (message != null) {
            String messageClass = message.contains("успешн") ? "success" : "error";
            out.println("        <div class='message " + messageClass + "'>" + message + "</div>");
        }

        out.println("        <form method='post' action='login'>");
        out.println("            <div class='form-group'>");
        out.println("                <label for='login'>Логин</label>");
        out.println("                <input type='text' id='login' name='login' required>");
        out.println("            </div>");
        out.println("            <div class='form-group'>");
        out.println("                <label for='password'>Пароль</label>");
        out.println("                <input type='password' id='password' name='password' required>");
        out.println("            </div>");
        out.println("            <button type='submit' class='btn'>Войти</button>");
        out.println("        </form>");
//        out.println("        <div class='links'>");
//        out.println("            <a href='/'>Главная страница</a>");
//        out.println("        </div>");
//        out.println("        <div class='register-link'>");
//        out.println("            Нет аккаунта? <a href='/register'>Зарегистрироваться</a>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }

    private void showRegisterPage(HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

    }
}