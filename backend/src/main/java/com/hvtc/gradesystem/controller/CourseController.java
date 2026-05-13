package com.hvtc.gradesystem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final JdbcTemplate jdbcTemplate;

    public CourseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String userRole,
                                          @RequestParam(required = false) String realName) {
        if ("teacher".equals(userRole) && hasText(realName)) {
            return jdbcTemplate.queryForList("SELECT * FROM courses WHERE teacher_name=? ORDER BY id DESC", realName);
        }
        return jdbcTemplate.queryForList("SELECT * FROM courses ORDER BY id DESC");
    }

    @PostMapping
    public String add(@RequestBody Map<String, Object> body) {
        validateCourse(body);
        String sql = "INSERT INTO courses(course_no, course_name, teacher_name, credit) VALUES(?, ?, ?, ?)";
        jdbcTemplate.update(sql, body.get("courseNo"), body.get("courseName"), body.get("teacherName"), body.get("credit"));
        addLog(body, "添加课程", "添加课程：" + body.get("courseName"));
        return "添加课程成功";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        validateCourse(body);
        String sql = "UPDATE courses SET course_no=?, course_name=?, teacher_name=?, credit=? WHERE id=?";
        jdbcTemplate.update(sql, body.get("courseNo"), body.get("courseName"), body.get("teacherName"), body.get("credit"), id);
        addLog(body, "修改课程", "修改课程：" + body.get("courseName"));
        return "修改课程成功";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam(required = false) String operatorName,
                         @RequestParam(required = false) String operatorRole) {
        String courseName = jdbcTemplate.queryForObject("SELECT course_name FROM courses WHERE id=?", String.class, id);
        jdbcTemplate.update("DELETE FROM scores WHERE course_id=?", id);
        jdbcTemplate.update("DELETE FROM courses WHERE id=?", id);
        addLog(operatorName, operatorRole, "删除课程", "删除课程：" + courseName);
        return "删除课程成功";
    }

    private void validateCourse(Map<String, Object> body) {
        if (isBlank(body.get("courseNo"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程编号不能为空");
        }
        if (isBlank(body.get("courseName"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程名称不能为空");
        }
        if (isBlank(body.get("teacherName"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任课教师不能为空");
        }
    }

    private void addLog(Map<String, Object> body, String type, String content) {
        addLog(String.valueOf(body.getOrDefault("operatorName", "未知用户")),
                String.valueOf(body.getOrDefault("operatorRole", "unknown")), type, content);
    }

    private void addLog(String operatorName, String operatorRole, String type, String content) {
        jdbcTemplate.update("INSERT INTO operation_logs(operator_name, operator_role, action_type, action_content) VALUES(?, ?, ?, ?)",
                hasText(operatorName) ? operatorName : "未知用户",
                hasText(operatorRole) ? operatorRole : "unknown",
                type,
                content);
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
