-- ТЕСТОВЫЕ ДАННЫЕ ДЛЯ СИСТЕМЫ ТАКСИ
INSERT INTO users (login, password, full_name, user_type, is_active) VALUES
('admin',      'admin123',   'Администратор Системы',    'ADMIN',       true),
('dispatcher', 'disp123',    'Диспетчер Петрова',        'OPERATOR',  true),
('driver1',    'driver123',  'Иванов Иван Иванович',     'DRIVER',      true),
('doctor',     'doctor123',  'Врач Смирнова',            'DOCTOR',      true),
('mechanic',   'mechanic123','Механик Козлов',           'MECHANIC',    true);
