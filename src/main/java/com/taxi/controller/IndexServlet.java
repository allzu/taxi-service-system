package com.taxi.controller;

import com.taxi.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class IndexServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = getCurrentUser(req);

        if (user == null) {
            // Показываем публичную главную страницу
            showPublicHomePage(req, resp);
        } else {
            // Перенаправляем на панель по роли
            String redirectPath = getDashboardByRole(user.getUserType());
            resp.sendRedirect(req.getContextPath() + redirectPath);
        }
    }

    private void showPublicHomePage(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Такси-сервис | Главная</title>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <style>");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { ");
        out.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
        out.println("            background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); ");
        out.println("            color: #e0e0e0; ");
        out.println("            min-height: 100vh; ");
        out.println("        }");
        out.println("        .hero { ");
        out.println("            text-align: center; ");
        out.println("            padding: 100px 20px; ");
        out.println("            background: rgba(30, 30, 30, 0.8); ");
        out.println("            border-bottom: 1px solid #333; ");
        out.println("        }");
        out.println("        .hero h1 { ");
        out.println("            font-size: 48px; ");
        out.println("            color: #fff; ");
        out.println("            margin-bottom: 20px; ");
        out.println("            font-weight: 300; ");
        out.println("        }");
        out.println("        .hero p { ");
        out.println("            font-size: 18px; ");
        out.println("            color: #aaa; ");
        out.println("            max-width: 600px; ");
        out.println("            margin: 0 auto 40px; ");
        out.println("            line-height: 1.6; ");
        out.println("        }");
        out.println("        .cta-button { ");
        out.println("            display: inline-block; ");
        out.println("            padding: 15px 40px; ");
        out.println("            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); ");
        out.println("            color: white; ");
        out.println("            text-decoration: none; ");
        out.println("            border-radius: 25px; ");
        out.println("            font-size: 18px; ");
        out.println("            font-weight: 500; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .cta-button:hover { ");
        out.println("            background: linear-gradient(135deg, #1976d2 0%, #0d47a1 100%); ");
        out.println("            transform: translateY(-3px); ");
        out.println("            box-shadow: 0 10px 20px rgba(33, 150, 243, 0.3); ");
        out.println("        }");
        out.println("        .features { ");
        out.println("            max-width: 1200px; ");
        out.println("            margin: 80px auto; ");
        out.println("            padding: 0 20px; ");
        out.println("            display: grid; ");
        out.println("            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); ");
        out.println("            gap: 30px; ");
        out.println("        }");
        out.println("        .feature-card { ");
        out.println("            background: rgba(30, 30, 30, 0.8); ");
        out.println("            padding: 30px; ");
        out.println("            border-radius: 15px; ");
        out.println("            border: 1px solid #333; ");
        out.println("            transition: all 0.3s ease; ");
        out.println("        }");
        out.println("        .feature-card:hover { ");
        out.println("            transform: translateY(-5px); ");
        out.println("            border-color: #444; ");
        out.println("            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); ");
        out.println("        }");
        out.println("        .feature-card h3 { ");
        out.println("            color: #fff; ");
        out.println("            margin-bottom: 15px; ");
        out.println("            font-size: 22px; ");
        out.println("            font-weight: 400; ");
        out.println("        }");
        out.println("        .feature-card p { ");
        out.println("            color: #888; ");
        out.println("            line-height: 1.6; ");
        out.println("        }");
        out.println("        .footer { ");
        out.println("            text-align: center; ");
        out.println("            padding: 40px 20px; ");
        out.println("            color: #666; ");
        out.println("            font-size: 14px; ");
        out.println("            border-top: 1px solid #333; ");
        out.println("            margin-top: 80px; ");
        out.println("        }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");

        out.println("    <div class='hero'>");
        out.println("        <h1>Такси-сервис</h1>");
        out.println("        <p>Информационно-справочная система для управления автопарком такси. Полный контроль за водителями, автомобилями и заказами в одном месте.</p>");
        out.println("        <a href='/login' class='cta-button'>Войти в систему</a>");
        out.println("    </div>");

        out.println("    <div class='features'>");
        out.println("        <div class='feature-card'>");
        out.println("            <h3>Управление автопарком</h3>");
        out.println("            <p>Полный учет автомобилей, техосмотров, ремонтов и текущего состояния транспортных средств.</p>");
        out.println("        </div>");

        out.println("        <div class='feature-card'>");
        out.println("            <h3>Контроль водителей</h3>");
        out.println("            <p>Система медосмотров, учет рабочего времени, путевые листы и расчет заработной платы.</p>");
        out.println("        </div>");

        out.println("        <div class='feature-card'>");
        out.println("            <h3>Обработка заказов</h3>");
        out.println("            <p>Прием заказов от клиентов, назначение водителей, отслеживание выполнения и расчет стоимости.</p>");
        out.println("        </div>");

        out.println("        <div class='feature-card'>");
        out.println("            <h3>Бизнес-сценарии</h3>");
        out.println("            <p>Автоматизация типовых процессов: начало смены, рабочий день, завершение смены.</p>");
        out.println("        </div>");

        out.println("        <div class='feature-card'>");
        out.println("            <h3>Безопасность</h3>");
        out.println("            <p>Ролевая модель доступа, авторизация пользователей, защита данных и журналирование действий.</p>");
        out.println("        </div>");

        out.println("        <div class='feature-card'>");
        out.println("            <h3>Отчетность</h3>");
        out.println("            <p>Детальная статистика, финансовые отчеты, аналитика эффективности работы автопарка.</p>");
        out.println("        </div>");
        out.println("    </div>");

        out.println("    <div class='footer'>");
        out.println("        &copy; 2024 Такси-сервис. Информационно-справочная система.");
        out.println("    </div>");

        out.println("</body>");
        out.println("</html>");
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
}