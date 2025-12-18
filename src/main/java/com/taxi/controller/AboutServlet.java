package com.taxi.controller;

import com.taxi.entity.User;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/about")
public class AboutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        String content = buildAboutContent();
        HtmlUtil.renderFullPage(out, request, "Об авторе", "about", content);
    }

    private String buildAboutContent() {
        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>")
                .append("<h1 class='page-title'> Об авторе проекта</h1>")
                .append("<p class='page-subtitle'>Информация о разработчике системы</p>")
                .append("</div>");

        content.append("<div class='card'>")
                .append("<div style='text-align: center; margin-bottom: 30px;'>")
                .append("<div style='font-size: 80px; color: #2196f3; margin-bottom: 20px;'></div>")
                .append("<h2 style='color: #fff; margin-bottom: 10px;'>Зубова Алёна Викторовна</h2>")
                .append("<p style='color: #aaa;'>Разработчик информационно-справочной системы службы такси</p>")
                .append("</div>")

                .append("<div style='max-width: 800px; margin: 0 auto;'>")
                .append("<div class='info-grid'>")

                // Блок с учебным заведением
                .append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #2196f3;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Учебное заведение</h4>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; line-height: 1.6;'>")
                .append("Федеральное государственное образовательное бюджетное<br>")
                .append("учреждение высшего образования<br>")
                .append("«Финансовый университет при Правительстве Российской Федерации»<br>")
                .append("<strong>(Финансовый университет)</strong>")
                .append("</p>")
                .append("</div>")

                // Блок с учебной информацией
                .append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #4caf50;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Учебная информация</h4>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; line-height: 1.6;'>")
                .append("<strong>Группа:</strong> ДПИ23-1<br>")
                .append("<strong>Почта:</strong> <a href='mailto:233306@edu.fa.ru' style='color: #64b5f6; text-decoration: none;'>233306@edu.fa.ru</a><br>")
                .append("</p>")
                .append("</div>")

                // Блок с датами разработки
                .append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #ff9800;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>Сроки разработки</h4>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; line-height: 1.6;'>")
                .append("<strong>Начало:</strong> 1 ноября 2025 г.<br>")
                .append("<strong>Завершение:</strong> 17 декабря 2025 г.<br>")
                .append("</p>")
                .append("</div>")

                // Блок с описанием проекта
                .append("<div class='info-section'>")
                .append("<div style='display: flex; align-items: center; margin-bottom: 15px;'>")
                .append("<div style='font-size: 2em; margin-right: 15px; color: #9c27b0;'></div>")
                .append("<div>")
                .append("<h4 style='color: #fff; margin: 0;'>О проекте</h4>")
                .append("</div>")
                .append("</div>")
                .append("<p style='color: #aaa; line-height: 1.6;'>")
                .append("Информационно-справочная система службы такси<br>")
                .append("<strong>Технологии:</strong> Java, Hibernate, PostgreSQL, Jetty<br>")
                .append("<strong>Цель:</strong> Автоматизация процессов диспетчерской службы такси")
                .append("</p>")
                .append("</div>")

                .append("</div>") // Закрываем info-grid

                // Футер с благодарностью
                .append("<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #333; text-align: center;'>")
                .append("<p style='color: #888; font-style: italic;'>")
                .append("Благодарю за внимание к моему проекту! Система разработана в рамках учебной практики.")
                .append("</p>")
                .append("<div style='margin-top: 20px;'>")
                .append("<a href='/' class='btn btn-primary'>")
                .append("<span style='font-size: 1.2em;'></span> Вернуться на главную")
                .append("</a>")
                .append("</div>")
                .append("</div>")

                .append("</div>") // Закрываем max-width контейнер
                .append("</div>"); // Закрываем card

        return content.toString();
    }
}