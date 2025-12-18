package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.entity.Car;
import com.taxi.repository.OrderRepository;
import com.taxi.repository.DriverRepository;
import com.taxi.repository.CarRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {

    private final OrderRepository orderRepository = new OrderRepository();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Получить отчет по доходам из ЗАКАЗОВ
     */
    public List<IncomeReportRecord> getIncomeReportRecords(LocalDate startDate, LocalDate endDate,
                                                           Long driverId, Long carId) {
        List<IncomeReportRecord> records = new ArrayList<>();

        // Получаем ВСЕ заказы для отладки
        List<Order> allOrders = orderRepository.findAll();
        System.out.println("=== ReportService Debug ===");
        System.out.println("Всего заказов в системе: " + allOrders.size());

        // Выводим статусы всех заказов
        for (Order order : allOrders) {
            System.out.println("Заказ #" + order.getId() +
                    " | Статус: " + order.getStatus() +
                    " | Водитель: " + (order.getDriver() != null ? order.getDriver().getId() : "null") +
                    " | Цена: " + order.getPrice() +
                    " | Дата завершения: " + order.getCompletionTime());
        }

        // Фильтруем завершенные заказы
        List<Order> orders = allOrders.stream()
                .filter(o -> {
                    boolean isCompleted = "COMPLETED".equals(o.getStatus());
                    boolean hasPrice = o.getPrice() != null && o.getPrice() > 0;
                    boolean hasCompletionTime = o.getCompletionTime() != null;

                    if (isCompleted) {
                        System.out.println("Заказ #" + o.getId() + " - COMPLETED, цена: " + o.getPrice());
                    }

                    return isCompleted && hasPrice && hasCompletionTime;
                })
                .filter(o -> {
                    // Безопасное преобразование LocalDateTime в LocalDate
                    if (o.getCompletionTime() == null) return false;
                    try {
                        LocalDate orderDate = o.getCompletionTime().toLocalDate();
                        return isInPeriod(orderDate, startDate, endDate);
                    } catch (Exception e) {
                        System.out.println("Ошибка при преобразовании даты для заказа #" + o.getId() + ": " + e.getMessage());
                        return false;
                    }
                })
                .filter(o -> driverId == null || (o.getDriver() != null && o.getDriver().getId().equals(driverId)))
                .filter(o -> carId == null || (o.getCar() != null && o.getCar().getId().equals(carId)))
                .collect(Collectors.toList());

        System.out.println("Найдено завершенных заказов: " + orders.size());
        System.out.println("Параметры фильтрации: startDate=" + startDate + ", endDate=" + endDate +
                ", driverId=" + driverId + ", carId=" + carId);

        for (Order order : orders) {
            try {
                IncomeReportRecord record = new IncomeReportRecord();
                record.setOrderId(order.getId());

                // Безопасное преобразование даты
                if (order.getCompletionTime() != null) {
                    record.setDate(order.getCompletionTime().toLocalDate());
                } else {
                    System.out.println("Заказ #" + order.getId() + " не имеет даты завершения");
                    continue; // Пропускаем заказы без даты завершения
                }

                // Информация о водителе
                if (order.getDriver() != null) {
                    record.setDriverName(order.getDriver().getFullName());
                    record.setDriverId(order.getDriver().getId());
                } else {
                    record.setDriverName("Не назначен");
                    record.setDriverId(null);
                }

                // Информация об автомобиле
                if (order.getCar() != null) {
                    record.setCarLicense(order.getCar().getLicensePlate());
                    record.setCarModel(order.getCar().getModel());
                    record.setCarId(order.getCar().getId());
                } else {
                    record.setCarLicense("Не назначен");
                    record.setCarModel("—");
                    record.setCarId(null);
                }

                // Статистика по заказу
                record.setOrdersCount(1);
                record.setDistance(order.getDistanceKm() != null ? order.getDistanceKm() : 0.0);
                record.setRevenue(order.getPrice() != null ? order.getPrice() : 0.0);

                // Рассчитываем заработок водителя (70% от стоимости заказа)
                double price = order.getPrice() != null ? order.getPrice() : 0.0;
                record.setEarnings(price * 0.7);

                // Временные метки
                record.setStartTime(order.getOrderTime());
                record.setEndTime(order.getCompletionTime());

                // Рассчитываем продолжительность
                if (order.getOrderTime() != null && order.getCompletionTime() != null) {
                    try {
                        Duration duration = Duration.between(order.getOrderTime(), order.getCompletionTime());
                        long hours = duration.toHours();
                        long minutes = duration.toMinutes() % 60;
                        record.setDuration(hours + "ч " + minutes + "м");
                    } catch (Exception e) {
                        System.out.println("Ошибка при расчете продолжительности заказа #" + order.getId() + ": " + e.getMessage());
                        record.setDuration("Н/Д");
                    }
                } else {
                    record.setDuration("Н/Д");
                }

                // Дополнительная информация
                record.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Без имени");
                record.setCustomerPhone(order.getCustomerPhone() != null ? order.getCustomerPhone() : "—");
                record.setPickupAddress(order.getPickupAddress() != null ? order.getPickupAddress() : "—");
                record.setDestinationAddress(order.getDestinationAddress() != null ? order.getDestinationAddress() : "—");

                records.add(record);

                System.out.println("Добавлен в отчет: Заказ #" + order.getId() +
                        " | Водитель: " + record.getDriverName() +
                        " | Цена: " + record.getRevenue() +
                        " | Дата: " + record.getDate());

            } catch (Exception e) {
                System.out.println("Ошибка при обработке заказа #" + order.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Итого записей в отчете: " + records.size());

        // Сортируем по дате (новые сверху)
        records.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        return records;
    }

    /**
     * Получить упрощенный отчет (только основные поля)
     */
    public List<SimpleIncomeRecord> getSimpleIncomeReport(LocalDate startDate, LocalDate endDate,
                                                          Long driverId, Long carId) {
        List<IncomeReportRecord> detailedRecords = getIncomeReportRecords(startDate, endDate, driverId, carId);
        List<SimpleIncomeRecord> simpleRecords = new ArrayList<>();

        for (IncomeReportRecord record : detailedRecords) {
            SimpleIncomeRecord simple = new SimpleIncomeRecord();
            simple.setDate(record.getDate());
            simple.setDriverName(record.getDriverName());
            simple.setCarInfo(record.getCarLicense() + " (" + record.getCarModel() + ")");
            simple.setRevenue(record.getRevenue());
            simple.setOrdersCount(record.getOrdersCount());
            simpleRecords.add(simple);
        }

        return simpleRecords;
    }

    /**
     * Получить сводную статистику по отчету
     */
    public ReportSummary getReportSummary(LocalDate startDate, LocalDate endDate,
                                          Long driverId, Long carId) {
        List<IncomeReportRecord> records = getIncomeReportRecords(startDate, endDate, driverId, carId);

        ReportSummary summary = new ReportSummary();
        summary.setRecordCount(records.size());

        double totalRevenue = 0;
        double totalEarnings = 0;
        double totalDistance = 0;
        int totalOrders = records.size();

        for (IncomeReportRecord record : records) {
            totalRevenue += record.getRevenue();
            totalEarnings += record.getEarnings();
            totalDistance += record.getDistance();
        }

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalEarnings(totalEarnings);
        summary.setTotalDistance(totalDistance);
        summary.setTotalOrders(totalOrders);

        if (records.size() > 0) {
            summary.setAverageRevenue(totalRevenue / records.size());
            summary.setAverageOrderPrice(totalOrders > 0 ? totalRevenue / totalOrders : 0);
        }

        return summary;
    }

    private boolean isInPeriod(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null) return false; // Добавлена проверка на null

        if (startDate == null && endDate == null) {
            return true;
        }
        if (startDate == null) {
            return !date.isAfter(endDate);
        }
        if (endDate == null) {
            return !date.isBefore(startDate);
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }


    /**
     * Детальная запись отчета
     */
    public static class IncomeReportRecord {
        private Long orderId;
        private LocalDate date;
        private String driverName;
        private Long driverId;
        private String carLicense;
        private String carModel;
        private Long carId;
        private int ordersCount;
        private double distance;
        private double revenue;
        private double earnings;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String duration;

        // Дополнительные поля для информации о заказе
        private String customerName;
        private String customerPhone;
        private String pickupAddress;
        private String destinationAddress;

        // Геттеры и сеттеры
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public Long getDriverId() {
            return driverId;
        }

        public void setDriverId(Long driverId) {
            this.driverId = driverId;
        }

        public String getCarLicense() {
            return carLicense;
        }

        public void setCarLicense(String carLicense) {
            this.carLicense = carLicense;
        }

        public String getCarModel() {
            return carModel;
        }

        public void setCarModel(String carModel) {
            this.carModel = carModel;
        }

        public Long getCarId() {
            return carId;
        }

        public void setCarId(Long carId) {
            this.carId = carId;
        }

        public int getOrdersCount() {
            return ordersCount;
        }

        public void setOrdersCount(int ordersCount) {
            this.ordersCount = ordersCount;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public double getRevenue() {
            return revenue;
        }

        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }

        public double getEarnings() {
            return earnings;
        }

        public void setEarnings(double earnings) {
            this.earnings = earnings;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getCustomerPhone() {
            return customerPhone;
        }

        public void setCustomerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
        }

        public String getPickupAddress() {
            return pickupAddress;
        }

        public void setPickupAddress(String pickupAddress) {
            this.pickupAddress = pickupAddress;
        }

        public String getDestinationAddress() {
            return destinationAddress;
        }

        public void setDestinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
        }

        // Метод для CSV (используется IncomeReportServlet)
        public String toCsvLine() {
            try {
                String dateStr = (date != null) ? date.format(DATE_FORMATTER) : "";
                String startTimeStr = (startTime != null) ? startTime.format(DATETIME_FORMATTER) : "";
                String endTimeStr = (endTime != null) ? endTime.format(DATETIME_FORMATTER) : "";

                return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\",\"%s\",\"%s\"",
                        dateStr,
                        escapeCsv(driverName),
                        escapeCsv(carLicense),
                        escapeCsv(carModel),
                        escapeCsv(carModel),
                        distance,
                        revenue,
                        earnings,
                        startTimeStr,
                        endTimeStr,
                        duration != null ? duration : ""
                );
            } catch (Exception e) {
                System.out.println("Ошибка при форматировании CSV строки: " + e.getMessage());
                return "\"Ошибка\",\"Ошибка\",\"\",\"\",\"\",0.00,0.00,0.00,\"\",\"\",\"Ошибка\"";
            }
        }

        public static String getCsvHeader() {
            return "Дата,Водитель,Гос. номер,Модель,Дистанция (км),Выручка (руб),Заработок (руб),Начало,Окончание,Продолжительность";
        }

        private String escapeCsv(String value) {
            if (value == null) return "";
            return value.replace("\"", "\"\"");
        }
    }

    /**
     * Упрощенная запись отчета (только основные поля)
     */
    public static class SimpleIncomeRecord {
        private LocalDate date;
        private String driverName;
        private String carInfo;
        private double revenue;
        private int ordersCount;

        // Геттеры и сеттеры
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public String getCarInfo() {
            return carInfo;
        }

        public void setCarInfo(String carInfo) {
            this.carInfo = carInfo;
        }

        public double getRevenue() {
            return revenue;
        }

        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }

        public int getOrdersCount() {
            return ordersCount;
        }

        public void setOrdersCount(int ordersCount) {
            this.ordersCount = ordersCount;
        }

        public String toCsvLine() {
            return String.format("\"%s\",\"%s\",\"%s\",%.2f,%d",
                    date.format(DATE_FORMATTER),
                    driverName,
                    carInfo,
                    revenue,
                    ordersCount
            );
        }

        public static String getCsvHeader() {
            return "Дата,Водитель,Автомобиль,Стоимость (руб),Кол-во заказов";
        }
    }

    /**
     * Сводная статистика отчета
     */
    public static class ReportSummary {
        private int recordCount;
        private double totalRevenue;
        private double totalEarnings;
        private double totalDistance;
        private int totalOrders;
        private double averageRevenue;
        private double averageOrderPrice;

        // Геттеры и сеттеры
        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public double getTotalEarnings() {
            return totalEarnings;
        }

        public void setTotalEarnings(double totalEarnings) {
            this.totalEarnings = totalEarnings;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public void setTotalDistance(double totalDistance) {
            this.totalDistance = totalDistance;
        }

        public int getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(int totalOrders) {
            this.totalOrders = totalOrders;
        }

        public double getAverageRevenue() {
            return averageRevenue;
        }

        public void setAverageRevenue(double averageRevenue) {
            this.averageRevenue = averageRevenue;
        }

        public double getAverageOrderPrice() {
            return averageOrderPrice;
        }

        public void setAverageOrderPrice(double averageOrderPrice) {
            this.averageOrderPrice = averageOrderPrice;
        }
    }

}