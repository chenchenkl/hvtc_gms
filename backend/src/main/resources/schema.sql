CREATE DATABASE IF NOT EXISTS hvtc_grade DEFAULT CHARACTER SET utf8mb4;
USE hvtc_grade;

DROP TABLE IF EXISTS operation_logs;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'admin/teacher/student',
    status VARCHAR(20) NOT NULL DEFAULT '启用' COMMENT '启用/禁用',
    related_no VARCHAR(50) COMMENT '学生账号对应学号，教师账号可填写教师姓名'
);

CREATE TABLE students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    class_name VARCHAR(50) NOT NULL
);

CREATE TABLE courses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_no VARCHAR(30) NOT NULL UNIQUE,
    course_name VARCHAR(80) NOT NULL,
    teacher_name VARCHAR(50) NOT NULL DEFAULT '李老师',
    credit DECIMAL(3,1) DEFAULT 2.0
);

CREATE TABLE scores (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    term VARCHAR(30) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    operator_name VARCHAR(50) NOT NULL,
    operator_role VARCHAR(20) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_content VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users(username, password, real_name, role, status, related_no) VALUES
('admin', '123456', '管理员', 'admin', '启用', NULL),
('teacher', '123456', '李老师', 'teacher', '启用', '李老师'),
('teacher2', '123456', '王老师', 'teacher', '启用', '王老师'),
('student', '123456', '张三', 'student', '启用', '20240101'),
('student2', '123456', '李四', 'student', '启用', '20240102');

INSERT INTO students(student_no, name, gender, class_name) VALUES
('20240101', '张三', '男', '软件技术一班'),
('20240102', '李四', '女', '软件技术一班'),
('20240103', '王五', '男', '软件技术一班'),
('20240201', '赵六', '女', '大数据技术一班'),
('20240202', '孙七', '男', '大数据技术一班');

INSERT INTO courses(course_no, course_name, teacher_name, credit) VALUES
('C001', 'Java程序设计', '李老师', 4.0),
('C002', '数据库基础', '李老师', 3.0),
('C003', 'Web前端开发', '王老师', 3.0),
('C004', '软件测试基础', '王老师', 2.0);

INSERT INTO scores(student_id, course_id, score, term) VALUES
(1, 1, 86, '2025-2026-1'),
(1, 2, 90, '2025-2026-1'),
(2, 1, 78, '2025-2026-1'),
(2, 2, 82, '2025-2026-1'),
(3, 1, 59, '2025-2026-1'),
(3, 3, 73, '2025-2026-1'),
(4, 2, 88, '2025-2026-1'),
(4, 4, 76, '2025-2026-1'),
(5, 3, 91, '2025-2026-1'),
(5, 4, 67, '2025-2026-1');

INSERT INTO operation_logs(operator_name, operator_role, action_type, action_content) VALUES
('管理员', 'admin', '系统初始化', '创建第二次迭代演示数据');
