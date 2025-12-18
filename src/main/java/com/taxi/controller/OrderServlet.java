package com.taxi.controller;

import com.taxi.entity.*;
import com.taxi.service.*;
import com.taxi.util.HtmlUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OrderServlet extends HttpServlet {
    private OrderService orderService;
    private DriverService driverService;
    private CarService carService;
    private UserService userService;
    private HttpServletRequest currentRequest; // Для доступа в методах

    @Override
    public void init() throws ServletException {
        this.orderService = new OrderService();
        this.driverService = new DriverService();
        this.carService = new CarService();
        this.userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        this.currentRequest = request;

        // Проверяем авторизацию
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect("/login");
            return;
        }

        String userRole = currentUser.getUserType();
        String path = request.getPathInfo();

        // Проверка доступа к разделу заказов
        if (!hasAccessToOrders(userRole)) {
            HtmlUtil.renderAccessDeniedPage(out, request, userRole);
            return;
        }

        try {
            if (path == null || path.equals("/")) {
                // Список заказов
                showOrdersList(request, out, currentUser);
            } else if (path.equals("/new")) {
                // Форма создания заказа
                if (!canCreateOrder(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                    return;
                }
                showCreateOrderForm(out, currentUser);
            } else if (path.equals("/view")) {
                // Просмотр деталей заказа
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long orderId = Long.parseLong(idParam);
                    showOrderDetails(orderId, out, currentUser);
                } else {
                    renderError(out, "Не указан ID заказа", request);
                }
            } else if (path.equals("/assign-driver")) {
                // Форма назначения водителя
                if (!canAssignDriver(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                    return;
                }
                String orderIdParam = request.getParameter("orderId");
                if (orderIdParam != null) {
                    Long orderId = Long.parseLong(orderIdParam);
                    showAssignDriverForm(orderId, out);
                } else {
                    renderError(out, "Не указан ID заказа", request);
                }
            } else if (path.equals("/complete")) {
                // Форма завершения заказа
                if (!canCompleteOrder(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                    return;
                }
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long orderId = Long.parseLong(idParam);
                    showCompleteOrderForm(out, orderId);
                } else {
                    renderError(out, "Не указан ID заказа", request);
                }
            } else if (path.equals("/cancel")) {
                // Форма отмены заказа
                if (!canCancelOrder(userRole)) {
                    HtmlUtil.renderAccessDeniedPage(out, request, userRole);
                    return;
                }
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long orderId = Long.parseLong(idParam);
                    showCancelOrderForm(out, orderId);
                } else {
                    renderError(out, "Не указан ID заказа", request);
                }
            } else if (path.equals("/start")) {
                // Начать выполнение заказа
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    Long orderId = Long.parseLong(idParam);
                    startOrder(orderId, response);
                } else {
                    renderError(out, "Не указан ID заказа", request);
                }
            } else {
                HtmlUtil.renderErrorPage(out, request, "Страница не найдена",
                        "Запрашиваемая страница не существует или была перемещена.");
            }
        } catch (NumberFormatException e) {
            renderError(out, "Неверный формат ID заказа", request);
        } catch (Exception e) {
            e.printStackTrace();
            renderError(out, "Ошибка сервера: " + e.getMessage(), request);
        }
    }

    /**
     * Показывает список заказов с учетом роли
     */
    private void showOrdersList(HttpServletRequest request, PrintWriter out, User currentUser) {
        String userRole = currentUser.getUserType();

        // Формируем контент страницы
        StringBuilder content = new StringBuilder();

        // Заголовок и фильтры
        content.append("<div class='mb-30'>");
        content.append("<h1 class='page-title'>Заказы</h1>");
        content.append("<p class='page-subtitle'>Управление заказами такси</p>");
        content.append("</div>");

        // Кнопки действий
        content.append("<div class='action-buttons mb-30'>");
        if (canCreateOrder(userRole)) {
            content.append("<a href='/orders/new' class='btn btn-success'> Создать заказ</a>");
        }

        // Получаем текущий фильтр из параметра
        String filter = request.getParameter("filter");
        if (filter == null) {
            filter = "active"; // По умолчанию показываем активные заказы
        }

        // Фильтры для оператора и админа
        if ("OPERATOR".equals(userRole) || "ADMIN".equals(userRole)) {
            content.append("<a href='?filter=active' class='btn btn-secondary");
            if ("active".equals(filter)) content.append(" active");
            content.append("'> Текущие заказы</a>");

            content.append("<a href='?filter=all' class='btn btn-secondary");
            if ("all".equals(filter)) content.append(" active");
            content.append("'> Все заказы</a>");

            content.append("<a href='?filter=completed' class='btn btn-secondary");
            if ("completed".equals(filter)) content.append(" active");
            content.append("'> Завершенные</a>");

            content.append("<a href='?filter=cancelled' class='btn btn-secondary");
            if ("cancelled".equals(filter)) content.append(" active");
            content.append("'> Отмененные</a>");
        } else if ("DRIVER".equals(userRole)) {
            // Фильтры для водителя
            content.append("<a href='?filter=assigned' class='btn btn-secondary'> Ожидающие</a>");
            content.append("<a href='?filter=in_progress' class='btn btn-secondary'> В работе</a>");
            content.append("<a href='?filter=completed' class='btn btn-secondary'> Завершенные</a>");
            content.append("<a href='?' class='btn btn-secondary'> Все мои заказы</a>");
        }
        content.append("</div>");


        // Получаем заказы в зависимости от роли и фильтра
        List<Order> orders = getOrdersForUser(currentUser, request, filter);

        // Таблица заказов
        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h3 class='card-title'>Список заказов</h3>");

        // Информация о текущем фильтре
        String filterText = getFilterDisplayText(filter);
        content.append("<span style='color: #888; font-size: 0.9em; margin-left: 15px;'>");
        content.append(filterText);
        content.append("</span>");

        content.append("</div>");
        content.append("<div class='card-body'>");

        if (orders.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<div class='empty-icon'>-</div>");
            content.append("<h3>Заказы не найдены</h3>");
            content.append("<p>По выбранным фильтрам заказов нет</p>");
            if (canCreateOrder(userRole) && ("active".equals(filter) || "new".equals(filter))) {
                content.append("<a href='/orders/new' class='btn btn-success mt-20'>Создать новый заказ</a>");
            }
            content.append("</div>");
        } else {
            content.append("<div class='table-container'>");
            content.append("<table>");
            content.append("<thead>");
            content.append("<tr>");
            // УБИРАЕМ СТОЛБЕЦ ID
            content.append("<th>Дата</th>");
            content.append("<th>Клиент</th>");
            content.append("<th>Маршрут</th>");
            content.append("<th>Водитель/Авто</th>");
            content.append("<th>Статус</th>");
            content.append("<th>Стоимость</th>");
            content.append("<th>Действия</th>");
            content.append("</tr>");
            content.append("</thead>");
            content.append("<tbody>");

            for (Order order : orders) {
                content.append("<tr>");
                // УБИРАЕМ ЯЧЕЙКУ С ID
                content.append("<td>").append(formatDateTime(order.getOrderTime())).append("</td>");
                content.append("<td>");
                content.append("<div><strong>").append(order.getCustomerName() != null ? order.getCustomerName() : "—").append("</strong></div>");
                content.append("<div><small>").append(order.getCustomerPhone() != null ? order.getCustomerPhone() : "—").append("</small></div>");
                content.append("</td>");
                content.append("<td>");
                content.append("<small>").append(order.getPickupAddress()).append(" → ");
                content.append(order.getDestinationAddress() != null ? order.getDestinationAddress() : "—").append("</small>");
                content.append("</td>");

                // Информация о водителе и авто
                content.append("<td>");
                if (order.getDriver() != null) {
                    content.append("<strong>").append(order.getDriver().getFullName()).append("</strong><br>");
                    if (order.getCar() != null) {
                        content.append("<small> ").append(order.getCar().getLicensePlate()).append("</small>");
                    } else if (order.getDriver().getCurrentCar() != null) {
                        content.append("<small> ").append(order.getDriver().getCurrentCar().getLicensePlate()).append("</small>");
                    }
                } else {
                    content.append("—");
                }
                content.append("</td>");

                // Статус
                content.append("<td>").append(getStatusBadge(order.getStatus())).append("</td>");

                // Стоимость
                content.append("<td>");
                if (order.getPrice() != null) {
                    content.append(String.format("%.2f ₽", order.getPrice()));
                } else {
                    content.append("—");
                }
                content.append("</td>");

                // Кнопки действий
                content.append("<td>");
                content.append("<div class='action-buttons-small'>");
                content.append("<a href='/orders/view?id=").append(order.getId()).append("' class='btn btn-sm' title='Просмотр'>Просмотр</a>");

                // Проверяем права на действия для текущего пользователя
                if (canPerformActionOnOrder(currentUser, order)) {
                    if (order.canBeAssigned() && canAssignDriver(userRole)) {
                        content.append("<a href='/orders/assign-driver?orderId=").append(order.getId())
                                .append("' class='btn btn-sm btn-info' title='Назначить водителя'>Водитель</a>");
                    }

                    if ("ASSIGNED".equals(order.getStatus()) && canStartOrder(currentUser, order)) {
                        content.append("<a href='/orders/start?id=").append(order.getId())
                                .append("' class='btn btn-sm btn-success' title='Начать выполнение'>Начать</a>");
                    }

                    if (order.isInProgress() && canCompleteOrder(userRole, order, currentUser)) {
                        content.append("<a href='/orders/complete?id=").append(order.getId())
                                .append("' class='btn btn-sm btn-warning' title='Завершить заказ'>Завершить</a>");
                    }

                    if ((order.canBeAssigned() || "ASSIGNED".equals(order.getStatus()) || order.isInProgress())
                            && canCancelOrder(userRole)) {
                        content.append("<a href='/orders/cancel?id=").append(order.getId())
                                .append("' class='btn btn-sm btn-danger' title='Отменить заказ'>Отмена</a>");
                    }
                }

                content.append("</div>");
                content.append("</td>");
                content.append("</tr>");
            }

            content.append("</tbody>");
            content.append("</table>");
            content.append("</div>");

            // Сводная информация
            content.append("<div class='mt-20' style='padding-top: 15px; border-top: 1px solid #333;'>");
            content.append("<div style='color: #888; font-size: 0.9em;'>");
            content.append("Показано: ").append(orders.size()).append(" заказов");
            if (!"all".equals(filter) && !"active".equals(filter)) {
                long activeCount = orders.stream().filter(o ->
                        "NEW".equals(o.getStatus()) ||
                                "ASSIGNED".equals(o.getStatus()) ||
                                "IN_PROGRESS".equals(o.getStatus())
                ).count();
                content.append(" | Из них активных: ").append(activeCount);
            }
            content.append("</div>");
            content.append("</div>");
        }


        content.append("</div>");
        content.append("</div>");

        // Статистика для оператора и админа
        if ("OPERATOR".equals(userRole) || "ADMIN".equals(userRole)) {
            content.append("<div class='card mb-30'>");
            content.append("<div class='card-header'>");
            content.append("<h3 class='card-title'> Статистика заказов</h3>");
            content.append("</div>");
            content.append("<div class='card-body'>");
            content.append("<div class='stats-grid'>");

            long totalOrders = orderService.getTotalOrders();
            long activeOrders = orderService.getActiveOrdersCount();
            long todayOrders = orderService.getTodayOrdersCount();
            double totalRevenue = orderService.getTotalRevenue();

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(totalOrders).append("</div>");
            content.append("<div class='stat-label'>Всего заказов</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(activeOrders).append("</div>");
            content.append("<div class='stat-label'>Активных</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(todayOrders).append("</div>");
            content.append("<div class='stat-label'>Сегодня</div>");
            content.append("</div>");

            content.append("<div class='stat-card'>");
            content.append("<div class='stat-icon'></div>");
            content.append("<div class='stat-value'>").append(String.format("%.2f", totalRevenue)).append(" ₽</div>");
            content.append("<div class='stat-label'>Общая выручка</div>");
            content.append("</div>");

            content.append("</div></div></div>");
        }

        // Рендерим полную страницу
        HtmlUtil.renderFullPage(out, request, "Заказы", "orders", content.toString());
    }

    /**
     * Получает список заказов для пользователя с учетом фильтров
     */
    private List<Order> getOrdersForUser(User user, HttpServletRequest request, String filter) {
        String userRole = user.getUserType();

        List<Order> orders;

        if ("DRIVER".equals(userRole)) {
            // Водитель видит только свои заказы
            Driver driver = driverService.findDriverByUserId(user.getId());
            if (driver != null) {
                orders = orderService.getDriverOrders(driver.getId());
            } else {
                orders = List.of();
            }
        } else if ("OPERATOR".equals(userRole) || "ADMIN".equals(userRole)) {
            // Оператор и админ видят все заказы
            orders = orderService.getAllOrders();
        } else {
            // Другие роли не должны иметь доступ
            orders = List.of();
        }

        // Применяем фильтр
        if (filter != null && !filter.isEmpty()) {
            orders = applyFilter(orders, filter);
        }

        return orders;
    }

    /**
     * Применяет фильтр к списку заказов
     */
    private List<Order> applyFilter(List<Order> orders, String filter) {
        return orders.stream()
                .filter(order -> {
                    switch (filter) {
                        case "new":
                            return "NEW".equals(order.getStatus());
                        case "assigned":
                            return "ASSIGNED".equals(order.getStatus());
                        case "in_progress":
                            return "IN_PROGRESS".equals(order.getStatus());
                        case "completed":
                            return "COMPLETED".equals(order.getStatus());
                        case "cancelled":
                            return "CANCELLED".equals(order.getStatus());
                        case "active": // ПО УМОЛЧАНИЮ - все кроме завершенных и отмененных
                            return !"COMPLETED".equals(order.getStatus()) &&
                                    !"CANCELLED".equals(order.getStatus());
                        case "all":
                            return true;
                        default:
                            return true;
                    }
                })
                .sorted((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает текст для отображения текущего фильтра
     */
    private String getFilterDisplayText(String filter) {
        if (filter == null) return "Текущие заказы";

        switch (filter) {
            case "new": return "Новые заказы";
            case "assigned": return "Назначенные заказы";
            case "in_progress": return "Заказы в работе";
            case "completed": return "Завершенные заказы";
            case "cancelled": return "Отмененные заказы";
            case "active": return "Текущие заказы (без завершенных и отмененных)";
            case "all": return "Все заказы";
            default: return "Текущие заказы";
        }
    }

    /**
     * Форма создания нового заказа
     */
    private void showCreateOrderForm(PrintWriter out, User currentUser) {
        StringBuilder content = new StringBuilder();

        content.append("<div class='card'>");
        content.append("<div class='card-header'>");
        content.append("<h2 class='card-title'> Создать новый заказ</h2>");
        content.append("</div>");
        content.append("<div class='card-body'>");

        content.append("<form method='POST' action='/orders/save' class='form'>");
        content.append("<div class='form-group'>");
        content.append("<label for='customerName' class='form-label'>Имя клиента:</label>");
        content.append("<input type='text' class='form-control' id='customerName' name='customerName' placeholder='Например: Иван Иванов'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='customerPhone' class='form-label'>Телефон клиента: <span class='required'>*</span></label>");
        content.append("<input type='tel' class='form-control' id='customerPhone' name='customerPhone' required placeholder='+79991234567'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='pickupAddress' class='form-label'>Адрес подачи: <span class='required'>*</span></label>");
        content.append("<input type='text' class='form-control' id='pickupAddress' name='pickupAddress' required placeholder='Например: ул. Ленина, 10'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='destinationAddress' class='form-label'>Адрес назначения:</label>");
        content.append("<input type='text' class='form-control' id='destinationAddress' name='destinationAddress' placeholder='Например: ул. Пушкина, 20'>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='notes' class='form-label'>Примечания:</label>");
        content.append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Дополнительная информация'></textarea>");
        content.append("</div>");

        content.append("<div class='form-group'>");
        content.append("<label for='plannedPickupTime' class='form-label'>Планируемое время подачи:</label>");
        content.append("<input type='datetime-local' class='form-control' id='plannedPickupTime' name='plannedPickupTime'>");
        content.append("</div>");

        content.append("<input type='hidden' name='operatorId' value='").append(currentUser.getId()).append("'>");

        content.append("<div class='form-actions'>");
        content.append("<button type='submit' class='btn btn-success'> Создать заказ</button>");
        content.append("<a href='/orders' class='btn btn-danger'> Отмена</a>");
        content.append("</div>");
        content.append("</form>");

        content.append("</div>");
        content.append("</div>");

        HtmlUtil.renderFullPage(out, currentRequest, "Создать заказ", "orders", content.toString());
    }

    /**
     * Просмотр деталей заказа
     */
    private void showOrderDetails(Long orderId, PrintWriter out, User currentUser) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                renderError(out, "Заказ не найден", currentRequest);
                return;
            }

            // Проверяем, имеет ли пользователь доступ к этому заказу
            if (!hasAccessToOrder(currentUser, order)) {
                HtmlUtil.renderAccessDeniedPage(out, currentRequest, currentUser.getUserType());
                return;
            }

            String userRole = currentUser.getUserType(); // ДОБАВЛЯЕМ ЭТУ СТРОКУ

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'> Детали заказа #").append(order.getId()).append("</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            // Информация о заказе в виде сетки
            content.append("<div class='info-grid'>");

            // Клиент
            content.append("<div class='info-section'>");
            content.append("<h3> Клиент</h3>");
            content.append("<p><strong>Имя:</strong> ").append(order.getCustomerName() != null ? order.getCustomerName() : "—").append("</p>");
            content.append("<p><strong>Телефон:</strong> ").append(order.getCustomerPhone() != null ? order.getCustomerPhone() : "—").append("</p>");
            content.append("<p><strong>Оператор:</strong> ").append(order.getOperator().getFullName()).append("</p>");
            content.append("</div>");

            // Маршрут
            content.append("<div class='info-section'>");
            content.append("<h3> Маршрут</h3>");
            content.append("<p><strong>Откуда:</strong> ").append(order.getPickupAddress()).append("</p>");
            content.append("<p><strong>Куда:</strong> ").append(order.getDestinationAddress() != null ? order.getDestinationAddress() : "—").append("</p>");
            if (order.getDistanceKm() != null) {
                content.append("<p><strong>Дистанция:</strong> ").append(String.format("%.1f", order.getDistanceKm())).append(" км</p>");
            }
            content.append("</div>");

            // Исполнитель
            content.append("<div class='info-section'>");
            content.append("<h3> Исполнитель</h3>");
            if (order.getDriver() != null) {
                content.append("<p><strong>Водитель:</strong> ").append(order.getDriver().getFullName()).append("</p>");
                content.append("<p><strong>Телефон:</strong> ").append(order.getDriver().getPhone() != null ? order.getDriver().getPhone() : "—").append("</p>");
                content.append("<p><strong>В/у:</strong> ").append(order.getDriver().getLicenseNumber()).append("</p>");
                if (order.getCar() != null) {
                    Car car = order.getCar();
                    content.append("<p><strong>Автомобиль:</strong> ").append(car.getLicensePlate())
                            .append(" (").append(car.getBrand()).append(" ").append(car.getModel()).append(")</p>");
                }
            } else {
                content.append("<p><strong>Водитель:</strong> Не назначен</p>");
            }
            content.append("</div>");

            // Время
            content.append("<div class='info-section'>");
            content.append("<h3> Время</h3>");
            content.append("<p><strong>Создан:</strong> ").append(formatDateTime(order.getOrderTime())).append("</p>");
            if (order.getPlannedPickupTime() != null) {
                content.append("<p><strong>Плановое время подачи:</strong> ").append(formatDateTime(order.getPlannedPickupTime())).append("</p>");
            }
            if (order.getActualPickupTime() != null) {
                content.append("<p><strong>Фактическое время подачи:</strong> ").append(formatDateTime(order.getActualPickupTime())).append("</p>");
            }
            if (order.getCompletionTime() != null) {
                content.append("<p><strong>Завершен:</strong> ").append(formatDateTime(order.getCompletionTime())).append("</p>");
            }
            content.append("</div>");

            // Финансы и статус
            content.append("<div class='info-section'>");
            content.append("<h3> Финансы</h3>");
            content.append("<p><strong>Статус:</strong> ").append(getStatusBadge(order.getStatus())).append("</p>");
            content.append("<p><strong>Стоимость:</strong> ").append(order.getPrice() != null ? String.format("%.2f ₽", order.getPrice()) : "—").append("</p>");
            if (order.getNotes() != null && !order.getNotes().isEmpty()) {
                content.append("<p><strong>Примечания:</strong> ").append(order.getNotes()).append("</p>");
            }
            content.append("</div>");

            content.append("</div>"); // закрываем info-grid

            // Кнопки действий
            content.append("<div class='action-buttons mt-30'>");
            content.append("<a href='/orders' class='btn btn-secondary'>← Назад к списку</a>");

            // Общая логика для всех ролей
            if (canPerformActionOnOrder(currentUser, order)) {
                if (order.canBeAssigned() && canAssignDriver(userRole)) {
                    content.append("<a href='/orders/assign-driver?orderId=").append(order.getId())
                            .append("' class='btn btn-info'> Назначить водителя</a>");
                }

                if ("ASSIGNED".equals(order.getStatus()) && canStartOrder(currentUser, order)) {
                    content.append("<a href='/orders/start?id=").append(order.getId())
                            .append("' class='btn btn-success'>️ Начать выполнение</a>");
                }

                if (order.isInProgress() && canCompleteOrder(userRole, order, currentUser)) {
                    content.append("<a href='/orders/complete?id=").append(order.getId())
                            .append("' class='btn btn-warning'> Завершить заказ</a>");
                }

                if ((order.canBeAssigned() || "ASSIGNED".equals(order.getStatus()) || order.isInProgress())
                        && canCancelOrder(userRole)) {
                    content.append("<a href='/orders/cancel?id=").append(order.getId())
                            .append("' class='btn btn-danger'> Отменить заказ</a>");
                }
            }

            content.append("</div>");
            content.append("</div>"); // закрываем card-body
            content.append("</div>"); // закрываем card

            HtmlUtil.renderFullPage(out, currentRequest, "Заказ #" + order.getId(), "orders", content.toString());

        } catch (Exception e) {
            renderError(out, "Ошибка при загрузке данных: " + e.getMessage(), currentRequest);
        }
    }

    /**
     * Форма назначения водителя
     */
    private void showAssignDriverForm(Long orderId, PrintWriter out) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                renderError(out, "Заказ не найден", currentRequest);
                return;
            }

            List<Driver> availableDrivers = driverService.getAvailableDrivers();
            List<Car> allCars = carService.getAllCars();

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'> Назначить водителя и автомобиль</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            // Информация о заказе
            content.append("<div class='alert alert-info mb-30'>");
            content.append("<p><strong>Заказ #").append(order.getId()).append("</strong></p>");
            content.append("<p><small>").append(order.getCustomerName() != null ? order.getCustomerName() : "Клиент")
                    .append(" | ").append(order.getCustomerPhone() != null ? order.getCustomerPhone() : "").append("</small></p>");
            content.append("<p><small>🗺 ").append(order.getPickupAddress()).append(" → ")
                    .append(order.getDestinationAddress() != null ? order.getDestinationAddress() : "...").append("</small></p>");
            content.append("</div>");

            // Форма
            content.append("<form method='post' action='/orders/assign-driver' class='form'>");
            content.append("<input type='hidden' name='orderId' value='").append(order.getId()).append("'>");

            content.append("<div class='form-group'>");
            content.append("<label for='driverId' class='form-label'>Выберите водителя <span class='required'>*</span></label>");
            content.append("<select class='form-control' id='driverId' name='driverId' required>");
            content.append("<option value=''>-- Выберите водителя --</option>");

            for (Driver driver : availableDrivers) {
                Car driverCar = driver.getCurrentCar();
                String carInfo = driverCar != null ?
                        " " + driverCar.getLicensePlate() + " (" + driverCar.getModel() + ")" :
                        " Нет автомобиля";

                String activeOrders = orderService.getActiveOrdersForDriver(driver.getId()).size() + " активных заказов";

                content.append("<option value='").append(driver.getId()).append("'>")
                        .append(driver.getFullName()).append(" (").append(driver.getLicenseNumber()).append(") - ")
                        .append(carInfo).append(" - ").append(activeOrders).append("</option>");
            }
            content.append("</select>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='carId' class='form-label'>Выберите автомобиль:</label>");
            content.append("<select class='form-control' id='carId' name='carId'>");
            content.append("<option value=''>-- Автомобиль по умолчанию (из профиля водителя) --</option>");

            for (Car car : allCars) {
                boolean isAvailable = car.getIsActive() &&
                        !car.getInRepair() &&
                        car.getCurrentDriver() == null;

                String status = "";
                if (!isAvailable) {
                    if (car.getCurrentDriver() != null) {
                        status = " ( Занят: " + car.getCurrentDriver().getFullName() + ")";
                    } else if (Boolean.TRUE.equals(car.getInRepair())) {
                        status = " ( В ремонте)";
                    } else if (!Boolean.TRUE.equals(car.getIsActive())) {
                        status = " ( Не активен)";
                    }
                }

                String disabled = isAvailable ? "" : "disabled";

                content.append("<option value='").append(car.getId()).append("' ").append(disabled).append(">")
                        .append(car.getLicensePlate()).append(" - ").append(car.getBrand()).append(" ").append(car.getModel())
                        .append(" (").append(car.getMileageKm() != null ? car.getMileageKm() : "0").append(" км)")
                        .append(status).append("</option>");
            }
            content.append("</select>");
            content.append("<small class='form-hint'>Если не выбрать автомобиль, будет использован автомобиль из профиля водителя</small>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='estimatedPrice' class='form-label'>Примерная стоимость (руб):</label>");
            content.append("<input type='number' class='form-control' id='estimatedPrice' name='estimatedPrice' step='0.01' min='0' placeholder='500.00'>");
            content.append("<small class='form-hint'>Ориентировочная стоимость поездки</small>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-success'> Назначить</button>");
            content.append("<a href='/orders' class='btn btn-danger'> Отмена</a>");
            content.append("</div>");
            content.append("</form>");

            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Назначить водителя", "orders", content.toString());

        } catch (Exception e) {
            renderError(out, "Ошибка при загрузке данных: " + e.getMessage(), currentRequest);
        }
    }

    /**
     * Форма завершения заказа
     */
    private void showCompleteOrderForm(PrintWriter out, Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                renderError(out, "Заказ не найден", currentRequest);
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'> Завершение заказа</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            content.append("<div class='alert alert-warning mb-30'>");
            content.append("<p><strong>Заказ #").append(order.getId()).append("</strong></p>");
            content.append("<p><small>").append(order.getPickupAddress()).append(" → ")
                    .append(order.getDestinationAddress() != null ? order.getDestinationAddress() : "...").append("</small></p>");
            if (order.getCustomerName() != null) {
                content.append("<p><small> ").append(order.getCustomerName()).append("</small></p>");
            }
            content.append("</div>");

            content.append("<form method='post' action='/orders/complete' class='form'>");
            content.append("<input type='hidden' name='orderId' value='").append(order.getId()).append("'>");

            // Изменяем поле дистанции - делаем НЕОБЯЗАТЕЛЬНЫМ
            content.append("<div class='form-group'>");
            content.append("<label for='actualDistance' class='form-label'>Фактическая дистанция (км):</label>");
            content.append("<input type='number' class='form-control' id='actualDistance' name='actualDistance' step='0.1' min='0' placeholder='5.5'>");
            content.append("<p class='form-hint'>Если не указано, будет использована примерная дистанция</p>");
            content.append("</div>");

            // Стоимость остается обязательной
            content.append("<div class='form-group'>");
            content.append("<label for='actualPrice' class='form-label'>Фактическая стоимость (руб): <span class='required'>*</span></label>");
            content.append("<input type='number' class='form-control' id='actualPrice' name='actualPrice' required step='0.01' min='0' placeholder='500.00'>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='notes' class='form-label'>Комментарий к выполнению:</label>");
            content.append("<textarea class='form-control' id='notes' name='notes' rows='3' placeholder='Дополнительная информация о выполнении заказа'></textarea>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-success'>Завершить заказ</button>");
            content.append("<a href='/orders' class='btn btn-danger'> Отмена</a>");
            content.append("</div>");
            content.append("</form>");

            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Завершить заказ", "orders", content.toString());

        } catch (Exception e) {
            renderError(out, "Ошибка при загрузке данных: " + e.getMessage(), currentRequest);
        }
    }

    /**
     * Форма отмены заказа
     */
    private void showCancelOrderForm(PrintWriter out, Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                renderError(out, "Заказ не найден", currentRequest);
                return;
            }

            StringBuilder content = new StringBuilder();

            content.append("<div class='card'>");
            content.append("<div class='card-header'>");
            content.append("<h2 class='card-title'> Отмена заказа</h2>");
            content.append("</div>");
            content.append("<div class='card-body'>");

            content.append("<div class='alert alert-danger mb-30'>");
            content.append("<p><strong>Заказ #").append(order.getId()).append("</strong></p>");
            content.append("<p><small>").append(order.getPickupAddress()).append(" → ")
                    .append(order.getDestinationAddress() != null ? order.getDestinationAddress() : "...").append("</small></p>");
            if (order.getCustomerName() != null) {
                content.append("<p><small> ").append(order.getCustomerName()).append(" (").append(order.getCustomerPhone()).append(")</small></p>");
            }
            if (order.getDriver() != null) {
                content.append("<p><small> Водитель: ").append(order.getDriver().getFullName()).append("</small></p>");
            }
            content.append("</div>");

            content.append("<form method='post' action='/orders/cancel' class='form'>");
            content.append("<input type='hidden' name='orderId' value='").append(order.getId()).append("'>");

            content.append("<div class='form-group'>");
            content.append("<label for='reason' class='form-label'>Причина отмены: <span class='required'>*</span></label>");
            content.append("<select class='form-control' id='reason' name='reason' required>");
            content.append("<option value=''>-- Выберите причину --</option>");
            content.append("<option value='Клиент отменил'>Клиент отменил</option>");
            content.append("<option value='Нет свободных водителей'>Нет свободных водителей</option>");
            content.append("<option value='Проблемы с автомобилем'>Проблемы с автомобилем</option>");
            content.append("<option value='Другое'>Другое</option>");
            content.append("</select>");
            content.append("</div>");

            content.append("<div class='form-group'>");
            content.append("<label for='details' class='form-label'>Подробности (если выбрано 'Другое'):</label>");
            content.append("<textarea class='form-control' id='details' name='details' rows='3' placeholder='Подробное описание причины отмены'></textarea>");
            content.append("</div>");

            content.append("<div class='form-actions'>");
            content.append("<button type='submit' class='btn btn-danger'> Отменить заказ</button>");
            content.append("<a href='/orders' class='btn btn-secondary'> Назад</a>");
            content.append("</div>");
            content.append("</form>");

            content.append("</div>");
            content.append("</div>");

            HtmlUtil.renderFullPage(out, currentRequest, "Отменить заказ", "orders", content.toString());

        } catch (Exception e) {
            renderError(out, "Ошибка при загрузке данных: " + e.getMessage(), currentRequest);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        this.currentRequest = request; // Сохраняем для использования в методах

        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
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
                    if (!canCreateOrder(currentUser.getUserType())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для создания заказов");
                        return;
                    }
                    createOrder(request, response, currentUser);
                    break;

                case "/assign-driver":
                    if (!canAssignDriver(currentUser.getUserType())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для назначения водителей");
                        return;
                    }
                    assignDriver(request, response, currentUser);
                    break;

                case "/complete":
                    if (!canCompleteOrder(currentUser.getUserType())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для завершения заказов");
                        return;
                    }
                    completeOrder(request, response, currentUser);
                    break;

                case "/cancel":
                    if (!canCancelOrder(currentUser.getUserType())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для отмены заказов");
                        return;
                    }
                    cancelOrder(request, response, currentUser);
                    break;

                case "/start":
                    startOrderFromPost(request, response, currentUser);
                    break;

                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Неизвестное действие: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/orders?error=" + e.getMessage());
        }
    }

    // ==================== POST-МЕТОДЫ ====================

    private void createOrder(HttpServletRequest request, HttpServletResponse response, User operator)
            throws IOException {

        String customerName = request.getParameter("customerName");
        String customerPhone = request.getParameter("customerPhone");
        String pickupAddress = request.getParameter("pickupAddress");
        String destinationAddress = request.getParameter("destinationAddress");
        String notes = request.getParameter("notes");
        String plannedPickupTimeStr = request.getParameter("plannedPickupTime");

        try {
            Order order = new Order();
            order.setOperator(operator);
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);
            order.setPickupAddress(pickupAddress);
            order.setDestinationAddress(destinationAddress);
            order.setStatus("NEW");
            order.setOrderTime(LocalDateTime.now());

            if (notes != null && !notes.isEmpty()) {
                order.setNotes(notes);
            }

            if (plannedPickupTimeStr != null && !plannedPickupTimeStr.isEmpty()) {
                try {
                    LocalDateTime plannedTime = LocalDateTime.parse(plannedPickupTimeStr.replace("T", " "));
                    order.setPlannedPickupTime(plannedTime);
                } catch (Exception e) {
                    // Игнорируем ошибку парсинга времени
                }
            }

            // Используем правильный метод из OrderService
            orderService.createOrder(order);
            response.sendRedirect("/orders?success=order_created&id=" + order.getId());

        } catch (Exception e) {
            response.sendRedirect("/orders/new?error=" + e.getMessage());
        }
    }

    private void assignDriver(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        Long orderId = Long.parseLong(request.getParameter("orderId"));
        Long driverId = Long.parseLong(request.getParameter("driverId"));
        String carIdParam = request.getParameter("carId");
        Double estimatedPrice = getDoubleParameter(request, "estimatedPrice");

        try {
            Order order = orderService.getOrderById(orderId);
            Driver driver = driverService.getDriverById(driverId);

            if (order == null || driver == null) {
                response.sendRedirect("/orders?error=Не найдены заказ или водитель");
                return;
            }

            Car car = null;
            if (carIdParam != null && !carIdParam.isEmpty()) {
                Long carId = Long.parseLong(carIdParam);
                car = carService.getCarById(carId);
            } else {
                car = driver.getCurrentCar();
            }

            if (car == null) {
                response.sendRedirect("/orders/assign-driver?orderId=" + orderId + "&error=У водителя нет автомобиля");
                return;
            }

            // Используем бизнес-метод из сущности
            order.assignToDriver(driver, car);

            if (estimatedPrice != null) {
                String additionalNotes = "Ориентировочная стоимость: " + estimatedPrice + " руб.";
                order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + additionalNotes);
            }

            // Используем правильный метод из OrderService
            orderService.updateOrder(order);
            response.sendRedirect("/orders?success=Водитель назначен");

        } catch (Exception e) {
            response.sendRedirect("/orders/assign-driver?orderId=" + orderId + "&error=" + e.getMessage());
        }
    }

    private void completeOrder(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        Long orderId = Long.parseLong(request.getParameter("orderId"));

        // Получаем дистанцию как необязательный параметр
        String actualDistanceStr = request.getParameter("actualDistance");
        Double actualDistance = null;
        if (actualDistanceStr != null && !actualDistanceStr.isEmpty()) {
            actualDistance = Double.parseDouble(actualDistanceStr);
        }

        // Стоимость остается обязательной
        Double actualPrice = Double.parseDouble(request.getParameter("actualPrice"));
        String notes = request.getParameter("notes");

        try {
            Order order = orderService.getOrderById(orderId);

            // Проверяем, может ли пользователь завершить этот заказ
            if (!canCompleteOrder(currentUser.getUserType(), order, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для завершения этого заказа");
                return;
            }

            // Используем бизнес-метод из сущности
            // Добавляем проверку на null для дистанции
            if (actualDistance != null) {
                order.completeOrder(actualDistance, actualPrice);
            } else {
                // Если дистанция не указана, используем примерную или 0
                order.setStatus("COMPLETED");
                order.setPrice(actualPrice);
                order.setCompletionTime(LocalDateTime.now());
                // Дистанция остается null
            }

            if (notes != null && !notes.isEmpty()) {
                order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + "При завершении: " + notes);
            }

            // Используем правильный метод из OrderService
            orderService.updateOrder(order);
            response.sendRedirect("/orders?success=Заказ завершен");

        } catch (Exception e) {
            response.sendRedirect("/orders?error=Ошибка при завершении заказа");
        }
    }

    private void cancelOrder(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        Long orderId = Long.parseLong(request.getParameter("orderId"));
        String reason = request.getParameter("reason");
        String details = request.getParameter("details");

        try {
            Order order = orderService.getOrderById(orderId);

            String fullReason = reason;
            if (details != null && !details.isEmpty()) {
                fullReason += ": " + details;
            }

            // Используем бизнес-метод из сущности
            order.cancelOrder(fullReason);

            // Используем правильный метод из OrderService
            orderService.updateOrder(order);
            response.sendRedirect("/orders?success=Заказ отменен");

        } catch (Exception e) {
            response.sendRedirect("/orders?error=Ошибка при отмене заказа");
        }
    }

    /**
     * Начало выполнения заказа (из POST)
     */
    private void startOrderFromPost(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        Long orderId = Long.parseLong(request.getParameter("id"));

        try {
            Order order = orderService.getOrderById(orderId);

            // Проверяем, может ли пользователь начать этот заказ
            if (!canStartOrder(currentUser, order)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "У вас нет прав для начала выполнения этого заказа");
                return;
            }

            // Используем бизнес-метод из сущности
            order.startTrip();

            // Используем правильный метод из OrderService
            orderService.updateOrder(order);
            response.sendRedirect("/orders?success=Заказ начат");

        } catch (Exception e) {
            response.sendRedirect("/orders?error=Ошибка при начале выполнения заказа");
        }
    }

    /**
     * Начало выполнения заказа (из GET для кнопок)
     */
    private void startOrder(Long orderId, HttpServletResponse response) throws IOException {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null && "ASSIGNED".equals(order.getStatus())) {
                order.startTrip();
                // Используем правильный метод из OrderService
                orderService.updateOrder(order);
            }
            response.sendRedirect("/orders?success=Заказ начат");
        } catch (Exception e) {
            response.sendRedirect("/orders?error=Ошибка при начале заказа");
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Проверяет, имеет ли пользователь доступ к разделу заказов
     */
    private boolean hasAccessToOrders(String userRole) {
        return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole) || "DRIVER".equals(userRole);
    }

    /**
     * Проверяет, имеет ли пользователь доступ к конкретному заказу
     */
    private boolean hasAccessToOrder(User user, Order order) {
        String userRole = user.getUserType();

        if ("ADMIN".equals(userRole)) {
            return true;
        } else if ("OPERATOR".equals(userRole)) {
            // Оператор видит все заказы
            return true;
        } else if ("DRIVER".equals(userRole)) {
            // Водитель видит только свои заказы
            Driver driver = driverService.findDriverByUserId(user.getId());
            return driver != null && order.getDriver() != null &&
                    driver.getId().equals(order.getDriver().getId());
        }
        return false;
    }

    /**
     * Может ли пользователь создать заказ
     */
    private boolean canCreateOrder(String userRole) {
        return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole);
    }

    /**
     * Может ли пользователь назначить водителя
     */
    private boolean canAssignDriver(String userRole) {
        return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole);
    }

    /**
     * Может ли пользователь завершить заказ (общая проверка)
     */
    private boolean canCompleteOrder(String userRole) {
        return "ADMIN".equals(userRole) || "DRIVER".equals(userRole);
    }

    /**
     * Может ли конкретный пользователь завершить конкретный заказ
     */
    private boolean canCompleteOrder(String userRole, Order order, User user) {
        if ("ADMIN".equals(userRole)) {
            return true;
        } else if ("DRIVER".equals(userRole)) {
            // Водитель может завершить только свой заказ
            Driver driver = driverService.findDriverByUserId(user.getId());
            return driver != null && order.getDriver() != null &&
                    driver.getId().equals(order.getDriver().getId());
        }
        return false;
    }

    /**
     * Может ли пользователь отменить заказ
     */
    private boolean canCancelOrder(String userRole) {
        return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole);
    }

    /**
     * Может ли пользователь начать выполнение заказа
     */
    private boolean canStartOrder(User user, Order order) {
        String userRole = user.getUserType();

        if ("ADMIN".equals(userRole)) {
            return true;
        } else if ("DRIVER".equals(userRole)) {
            // Водитель может начать только свой заказ
            Driver driver = driverService.findDriverByUserId(user.getId());
            return driver != null && order.getDriver() != null &&
                    driver.getId().equals(order.getDriver().getId()) &&
                    "ASSIGNED".equals(order.getStatus());
        }
        return false;
    }

    /**
     * Может ли пользователь выполнить действие с заказом
     */
    private boolean canPerformActionOnOrder(User user, Order order) {
        String userRole = user.getUserType();

        if ("ADMIN".equals(userRole)) {
            return true;
        } else if ("OPERATOR".equals(userRole)) {
            // Оператор может только назначать и отменять
            return order.canBeAssigned() || "ASSIGNED".equals(order.getStatus()) || order.isInProgress();
        } else if ("DRIVER".equals(userRole)) {
            // Водитель может работать только со своими заказами
            Driver driver = driverService.findDriverByUserId(user.getId());
            if (driver == null || order.getDriver() == null) {
                return false;
            }
            boolean isDriverOrder = driver.getId().equals(order.getDriver().getId());
            return isDriverOrder && ("ASSIGNED".equals(order.getStatus()) || order.isInProgress());
        }
        return false;
    }

    /**
     * Формирует бейдж статуса
     */
    private String getStatusBadge(String status) {
        if (status == null) return "<span class='badge'>Неизвестно</span>";

        String badgeClass;
        String statusText;

        switch (status) {
            case "NEW":
                badgeClass = "badge-info";
                statusText = "Новый";
                break;
            case "ASSIGNED":
                badgeClass = "badge-info";
                statusText = "Назначен";
                break;
            case "IN_PROGRESS":
                badgeClass = "badge-warning";
                statusText = "В работе";
                break;
            case "COMPLETED":
                badgeClass = "badge-success";
                statusText = "Завершен";
                break;
            case "CANCELLED":
                badgeClass = "badge-danger";
                statusText = "Отменен";
                break;
            default:
                badgeClass = "badge-secondary";
                statusText = status;
        }

        return "<span class='badge " + badgeClass + "'>" + statusText + "</span>";
    }

    /**
     * Форматирует дату и время
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Получает Double параметр из запроса
     */
    private Double getDoubleParameter(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);
        if (value != null && !value.isEmpty()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Отображает ошибку
     */
    private void renderError(PrintWriter out, String message, HttpServletRequest request) {
        HtmlUtil.renderErrorPage(out, request, "Ошибка", message);
    }
}