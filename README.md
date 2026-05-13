# 成绩管理系统

## 一、项目定位

本项目适合学生课程设计、小组敏捷开发作业和课堂答辩使用。

系统保持“简单、清晰、能运行”的原则，没有使用复杂的企业级权限框架，而是用基础代码实现。

## 二、技术选型

- 前端：HTML + CSS + JavaScript + Vue 3 
- 后端：Java + Spring Boot + JdbcTemplate
- 数据库：MySQL
- 数据交互：JSON
- 导出方式：CSV 文件，可用 Excel 打开

## 三、数据库名称

```sql
hvtc_grade
```

数据库脚本位置：

```text
backend/src/main/resources/schema.sql
```

## 四、测试账号

| 用户名 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | 123456 | 管理员 | 可管理用户、学生、课程、所有成绩和日志 |
| teacher | 123456 | 教师 | 李老师，只能管理自己课程的成绩 |
| teacher2 | 123456 | 教师 | 王老师，只能管理自己课程的成绩 |
| student | 123456 | 学生 | 张三，只能查看自己的成绩 |
| student2 | 123456 | 学生 | 李四，只能查看自己的成绩 |

## 五、第二次迭代原因

第一次迭代已经完成成绩校验、条件查询、成绩修改和简单统计，系统基本可用。但在实际学校成绩管理场景中，还存在以下不足：

1. 不同角色的权限不够清晰，管理员、教师、学生应该拥有不同操作范围。
2. 成绩只能在页面中查看，不方便导出、保存或上交。
3. 系统缺少操作记录，无法追踪是谁录入、修改或删除了成绩。
4. 成绩统计只有数字，不够直观。
5. 管理员缺少系统用户维护能力，不方便管理账号。

因此，第二次迭代围绕“权限更清晰、数据更安全、管理更方便、统计更直观”进行优化。

## 六、第二次迭代新增功能点

### 1. 角色权限管理

系统按管理员、教师、学生三类角色区分权限：

- 管理员：可以管理用户、学生、课程、全部成绩和操作日志。
- 教师：只能查看、录入、修改和删除自己所负责课程的成绩。
- 学生：只能查看自己的成绩，不能录入、修改或删除成绩。

该功能优化了系统安全性，减少了越权操作和误操作。

### 2. 用户管理功能

管理员可以在系统中进行基础用户维护：

- 添加用户
- 修改用户姓名、角色、状态
- 禁用账号
- 重置密码为 123456
- 删除用户

该功能方便管理员维护学生、教师和管理员账号。

### 3. 成绩导出功能

教师和管理员可以根据当前查询条件导出成绩数据。

导出内容包括：

- 学号
- 姓名
- 班级
- 课程编号
- 课程名称
- 任课教师
- 学分
- 成绩
- 学期

导出的 CSV 文件可以直接用 Excel 打开，方便保存和上交成绩材料。

### 4. 操作日志功能

系统新增操作日志表 `operation_logs`，用于记录重要操作。

记录内容包括：

- 操作人
- 操作人角色
- 操作类型
- 操作内容
- 操作时间

日志可以帮助管理员追踪成绩和基础数据的变动情况。

### 5. 成绩可视化统计

在第一次迭代的统计功能基础上，第二次迭代增加了简单柱状图展示。

统计维度包括：

- 优秀人数：90 分及以上
- 良好人数：80 到 89 分
- 及格人数：60 到 79 分
- 不及格人数：60 分以下

该功能让教师和管理员可以更直观地了解成绩分布情况。

### 6. 课程绑定任课教师

课程表新增 `teacher_name` 字段，用于绑定任课教师。

教师登录后，只能看到自己负责的课程，只能操作自己课程下的成绩。

## 七、运行步骤

### 1. 导入数据库

用 Navicat 或 MySQL 命令行执行：

```text
backend/src/main/resources/schema.sql
```

执行成功后会创建数据库：

```text
hvtc_grade
```

注意：执行 V1.2 的 `schema.sql` 会重建演示表和演示数据。如果你之前导入过基础版或 V1.1，建议重新执行一次 V1.2 的脚本。

### 2. 修改数据库密码

打开：

```text
backend/src/main/resources/application.yml
```

根据自己电脑的 MySQL 密码修改：

```yaml
spring:
  datasource:
    username: root
    password: 123456
```

### 3. 启动后端

进入后端目录：

```bash
cd backend
mvn spring-boot:run
```

启动成功后，后端地址为：

```text
http://localhost:8080
```

### 4. 打开前端

直接用浏览器打开：

```text
frontend/index.html
```

## 八、接口说明

### 登录

```text
POST /api/login
```

### 用户管理

```text
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
PUT    /api/users/{id}/password
DELETE /api/users/{id}
```

### 学生管理

```text
GET    /api/students
POST   /api/students
PUT    /api/students/{id}
DELETE /api/students/{id}
```

### 课程管理

```text
GET    /api/courses
POST   /api/courses
PUT    /api/courses/{id}
DELETE /api/courses/{id}
```

### 成绩管理

```text
GET    /api/scores
POST   /api/scores
PUT    /api/scores/{id}
DELETE /api/scores/{id}
```

### 成绩统计

```text
GET /api/scores/stats
```

### 成绩导出

```text
GET /api/scores/export
```

### 操作日志

```text
GET /api/logs
```

