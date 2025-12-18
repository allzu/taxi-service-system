package com.taxi.util;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.FilterHolder;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;

public class JettyLauncher {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("ИНФОРМАЦИОННО-СПРАВОЧНАЯ СИСТЕМА СЛУЖБЫ ТАКСИ");
        System.out.println("=".repeat(60));

        // Пробуем подключиться к БД
        boolean dbConnected = false;
        try {
            var sessionFactory = HibernateUtil.getSessionFactory();
            var session = sessionFactory.openSession();
            session.close();
            dbConnected = true;
            System.out.println("Подключение к БД: УСПЕШНО");
        } catch (Exception e) {
            System.out.println("Подключение к БД: ОШИБКА");
            System.out.println("Сообщение: " + e.getMessage());
            System.out.println("Приложение запустится, но работа с БД будет недоступна");
        }

        // Запуск сервера
        int port = 8080;
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");



        // фильтр безопасности
        context.addFilter(new FilterHolder(new AuthFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));

        // Регистрируем все сервлеты
        registerAllServlets(context);

        server.setHandler(context);
        server.start();

        System.out.println("\n" + "=".repeat(30));
        System.out.println("СЕРВЕР ЗАПУЩЕН УСПЕШНО!");
        System.out.println("=".repeat(30));
        System.out.println();
        System.out.println("Главная страница: http://localhost:" + port + "/");
        System.out.println("Вход в систему:  http://localhost:" + port + "/login");
        System.out.println();
        System.out.println("Доступные разделы:");
        System.out.println("  Автомобили:        http://localhost:" + port + "/cars");
        System.out.println("  Водители:          http://localhost:" + port + "/drivers");
        System.out.println("  Медосмотры:        http://localhost:" + port + "/medical-checks");
        System.out.println("  Техосмотры:        http://localhost:" + port + "/inspections");
        System.out.println("  Путевые листы:     http://localhost:" + port + "/waybills");
        System.out.println("  Заказы:            http://localhost:" + port + "/orders");
        System.out.println();

        if (!dbConnected) {
            System.out.println("ВНИМАНИЕ: База данных недоступна!");
        }

        System.out.println("Для остановки нажмите Ctrl+C");
        System.out.println("=".repeat(60));

        server.join();
    }

    private static void registerAllServlets(ServletContextHandler context) {
        System.out.println("\nРегистрация сервлетов:");

        try {
            // 1. Главная страница
            context.addServlet(new ServletHolder(new com.taxi.controller.IndexServlet()), "/");
            context.addServlet(new ServletHolder(new com.taxi.controller.IndexServlet()), "/index");
            System.out.println("   IndexServlet -> /, /index");

            // 2. Сервлеты авторизации
            context.addServlet(new ServletHolder(new com.taxi.controller.AuthServlet()), "/login");
            context.addServlet(new ServletHolder(new com.taxi.controller.AuthServlet()), "/logout");
            context.addServlet(new ServletHolder(new com.taxi.controller.AuthServlet()), "/register");
            System.out.println("   AuthServlet -> /login, /logout, /register");

            // 3. Автомобили
            context.addServlet(new ServletHolder(new com.taxi.controller.CarServlet()), "/cars");
            context.addServlet(new ServletHolder(new com.taxi.controller.CarServlet()), "/cars/*");
            System.out.println("   CarServlet -> /cars, /cars/*");

            // 4. Водители
            context.addServlet(new ServletHolder(new com.taxi.controller.DriverServlet()), "/drivers");
            context.addServlet(new ServletHolder(new com.taxi.controller.DriverServlet()), "/drivers/*");
            System.out.println("   DriverServlet -> /drivers, /drivers/*");

            // 5. Медосмотры
            context.addServlet(new ServletHolder(new com.taxi.controller.MedicalCheckServlet()), "/medical-checks");
            context.addServlet(new ServletHolder(new com.taxi.controller.MedicalCheckServlet()), "/medical-checks/*");
            System.out.println("   MedicalCheckServlet -> /medical-checks, /medical-checks/*");

            // 6. Техосмотры
            context.addServlet(new ServletHolder(new com.taxi.controller.TechnicalInspectionServlet()), "/inspections");
            context.addServlet(new ServletHolder(new com.taxi.controller.TechnicalInspectionServlet()), "/inspections/*");
            System.out.println("   TechnicalInspectionServlet -> /inspections, /inspections/*");

            // 7. Путевые листы
            context.addServlet(new ServletHolder(new com.taxi.controller.WaybillServlet()), "/waybills");
            context.addServlet(new ServletHolder(new com.taxi.controller.WaybillServlet()), "/waybills/*");
            System.out.println("   WaybillServlet -> /waybills, /waybills/*");

            // 8. Заказы
            context.addServlet(new ServletHolder(new com.taxi.controller.OrderServlet()), "/orders");
            context.addServlet(new ServletHolder(new com.taxi.controller.OrderServlet()), "/orders/*");
            System.out.println("   OrderServlet -> /orders, /orders/*");

            // 9. Панель управления (по ролям)
            context.addServlet(new ServletHolder(new com.taxi.controller.DoctorPanelServlet()), "/doctor");
            context.addServlet(new ServletHolder(new com.taxi.controller.DoctorPanelServlet()), "/doctor/*");
            System.out.println("   DoctorPanelServlet -> /doctor, /doctor/*");

            context.addServlet(new ServletHolder(new com.taxi.controller.MechanicPanelServlet()), "/mechanic");
            context.addServlet(new ServletHolder(new com.taxi.controller.MechanicPanelServlet()), "/mechanic/*");
            System.out.println("   MechanicPanelServlet -> /mechanic, /mechanic/*");

            context.addServlet(new ServletHolder(new com.taxi.controller.DispatcherPanelServlet()), "/dispatcher");
            context.addServlet(new ServletHolder(new com.taxi.controller.DispatcherPanelServlet()), "/dispatcher/*");
            System.out.println("   DispatcherPanelServlet -> /dispatcher, /dispatcher/*");

            context.addServlet(new ServletHolder(new com.taxi.controller.DriverPanelServlet()), "/driver-panel");
            context.addServlet(new ServletHolder(new com.taxi.controller.DriverPanelServlet()), "/driver-panel/*");
            System.out.println("   DriverPanelServlet -> /driver-panel, /driver-panel/*");

            context.addServlet(new ServletHolder(new com.taxi.controller.AdminPanelServlet()), "/admin");

            // UserManagementServlet - для управления пользователями
            context.addServlet(new ServletHolder(new com.taxi.controller.UserManagementServlet()), "/admin/users");
            context.addServlet(new ServletHolder(new com.taxi.controller.UserManagementServlet()), "/admin/users/*");
            System.out.println("   UserManagementServlet -> /admin/users, /admin/users/*");

            System.out.println("   AdminPanelServlet -> /admin (только корень)");


            // 11. Проверка системы
            context.addServlet(new ServletHolder(new HealthCheckServlet()), "/health");
            System.out.println("   HealthCheckServlet -> /health");

            //Отчет
            context.addServlet(new ServletHolder(new com.taxi.controller.IncomeReportServlet()), "/admin/income-report/csv");
            System.out.println("   IncomeReportServlet -> /admin/income-report/csv");

            //об авторе

            context.addServlet(new ServletHolder(new com.taxi.controller.AboutServlet()), "/about");
            System.out.println("   AboutServlet -> /about");

            System.out.println("Все сервлеты зарегистрированы!");

        } catch (Exception e) {
            System.err.println("Ошибка при регистрации сервлетов: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //  ФИЛЬТР БЕЗОПАСНОСТИ
    public static class AuthFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {}

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpSession session = httpRequest.getSession(false);

            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

            // Разрешенные пути без авторизации
            if (path.equals("/login") || path.equals("/register") || path.equals("/health") ||
                    path.startsWith("/css/") || path.startsWith("/js/")) {
                chain.doFilter(request, response);
                return;
            }

            // Проверка авторизации
//            if (session == null || session.getAttribute("user") == null) {
//                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
//                return;
//            }

            // После проверки авторизации, перенаправляем на правильную панель
            if (session != null && session.getAttribute("user") != null) {
                com.taxi.entity.User user = (com.taxi.entity.User) session.getAttribute("user");
                String userRole = user.getUserType();

                // Если пользователь пытается зайти на "/" или "/index", перенаправляем по роли
                if (path.equals("/") || path.equals("/index")) {
                    String redirectPath = getDashboardByRole(userRole);
                    if (!redirectPath.equals(path)) {
                        httpResponse.sendRedirect(httpRequest.getContextPath() + redirectPath);
                        return;
                    }
                }
            }

            // Проверка ролей для защищенных путей
            com.taxi.entity.User user = (com.taxi.entity.User) session.getAttribute("user");
            String userRole = user.getUserType();

            // Проверка доступа к панелям по ролям
            if (path.startsWith("/doctor") && !"DOCTOR".equals(userRole)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
                return;
            }
            if (path.startsWith("/mechanic") && !"MECHANIC".equals(userRole)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
                return;
            }
            if (path.startsWith("/dispatcher") && !"OPERATOR".equals(userRole)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
                return;
            }
            if (path.startsWith("/driver-panel") && !"DRIVER".equals(userRole)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
                return;
            }
            if (path.startsWith("/admin") && !"ADMIN".equals(userRole)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
                return;
            }

            chain.doFilter(request, response);
        }

        // Метод для определения панели по роли
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

        @Override
        public void destroy() {}
    }

    // СТРАНИЦА ПРОВЕРКИ
    public static class HealthCheckServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>Такси-сервис | Проверка системы</title>");
            out.println("    <meta charset='UTF-8'>");
            out.println("    <style>");
            out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
            out.println("        body { ");
            out.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
            out.println("            background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); ");
            out.println("            color: #e0e0e0; ");
            out.println("            min-height: 100vh; ");
            out.println("            padding: 40px; ");
            out.println("            line-height: 1.6; ");
            out.println("        }");
            out.println("        .container { ");
            out.println("            max-width: 1200px; ");
            out.println("            margin: 0 auto; ");
            out.println("            background: rgba(30, 30, 30, 0.8); ");
            out.println("            border-radius: 15px; ");
            out.println("            padding: 40px; ");
            out.println("            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); ");
            out.println("            backdrop-filter: blur(10px); ");
            out.println("        }");
            out.println("        h1 { ");
            out.println("            color: #ffffff; ");
            out.println("            margin-bottom: 20px; ");
            out.println("            font-size: 2.5em; ");
            out.println("            font-weight: 300; ");
            out.println("            border-bottom: 1px solid #444; ");
            out.println("            padding-bottom: 15px; ");
            out.println("        }");
            out.println("        .status { ");
            out.println("            padding: 15px; ");
            out.println("            margin: 20px 0; ");
            out.println("            border-radius: 8px; ");
            out.println("            font-weight: 500; ");
            out.println("        }");
            out.println("        .success { ");
            out.println("            background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%); ");
            out.println("            color: #ffffff; ");
            out.println("            border-left: 5px solid #4caf50; ");
            out.println("        }");
            out.println("        .warning { ");
            out.println("            background: linear-gradient(135deg, #f57c00 0%, #e65100 100%); ");
            out.println("            color: #ffffff; ");
            out.println("            border-left: 5px solid #ff9800; ");
            out.println("        }");
            out.println("        .endpoints { ");
            out.println("            display: grid; ");
            out.println("            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); ");
            out.println("            gap: 15px; ");
            out.println("            margin-top: 30px; ");
            out.println("        }");
            out.println("        .endpoint { ");
            out.println("            background: rgba(255, 255, 255, 0.05); ");
            out.println("            padding: 20px; ");
            out.println("            border-radius: 10px; ");
            out.println("            text-align: center; ");
            out.println("            transition: all 0.3s ease; ");
            out.println("            text-decoration: none; ");
            out.println("            color: #e0e0e0; ");
            out.println("            border: 1px solid #444; ");
            out.println("        }");
            out.println("        .endpoint:hover { ");
            out.println("            background: rgba(255, 255, 255, 0.1); ");
            out.println("            transform: translateY(-5px); ");
            out.println("            border-color: #666; ");
            out.println("            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3); ");
            out.println("        }");
            out.println("        .endpoint h3 { ");
            out.println("            color: #ffffff; ");
            out.println("            margin-bottom: 10px; ");
            out.println("            font-weight: 400; ");
            out.println("        }");
            out.println("        .info { ");
            out.println("            margin-top: 30px; ");
            out.println("            padding: 20px; ");
            out.println("            background: rgba(255, 255, 255, 0.03); ");
            out.println("            border-radius: 10px; ");
            out.println("        }");
            out.println("        .info p { margin: 10px 0; }");
            out.println("        .login-link { ");
            out.println("            display: inline-block; ");
            out.println("            margin-top: 20px; ");
            out.println("            padding: 12px 30px; ");
            out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
            out.println("            color: white; ");
            out.println("            text-decoration: none; ");
            out.println("            border-radius: 25px; ");
            out.println("            transition: all 0.3s ease; ");
            out.println("            font-weight: 500; ");
            out.println("        }");
            out.println("        .login-link:hover { ");
            out.println("            background: linear-gradient(135deg, #1976d2 0%, #0d47a1 100%); ");
            out.println("            transform: scale(1.05); ");
            out.println("        }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='container'>");
            out.println("        <h1>Такси-сервис | Проверка системы</h1>");

            // Проверяем подключение к БД
            try {
                var sessionFactory = HibernateUtil.getSessionFactory();
                var session = sessionFactory.openSession();
                session.close();
                out.println("        <div class='status success'>База данных подключена</div>");
            } catch (Exception e) {
                out.println("        <div class='status warning'>База данных не подключена: " + e.getMessage() + "</div>");
            }

            out.println("        <div class='info'>");
            out.println("            <p><strong>Время:</strong> " + new java.util.Date() + "</p>");
            out.println("            <p><strong>Сервер:</strong> Jetty 11</p>");
            out.println("            <p><strong>Порт:</strong> 8080</p>");
            out.println("            <a href='/login' class='login-link'>Войти в систему</a>");
            out.println("        </div>");

            out.println("        <div class='endpoints'>");
            out.println("            <a class='endpoint' href='/cars'><h3>Автомобили</h3>Управление автопарком</a>");
            out.println("            <a class='endpoint' href='/drivers'><h3>Водители</h3>Учет водителей</a>");
            out.println("            <a class='endpoint' href='/medical-checks'><h3>Медосмотры</h3>Медицинские проверки</a>");
            out.println("            <a class='endpoint' href='/inspections'><h3>Техосмотры</h3>Технические осмотры</a>");
            out.println("            <a class='endpoint' href='/waybills'><h3>Путевые листы</h3>Учет смен</a>");
            out.println("            <a class='endpoint' href='/orders'><h3>Заказы</h3>Управление заказами</a>");
            out.println("            <a class='endpoint' href='/scenarios'><h3>Сценарии</h3>Бизнес-процессы</a>");
            out.println("        </div>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}