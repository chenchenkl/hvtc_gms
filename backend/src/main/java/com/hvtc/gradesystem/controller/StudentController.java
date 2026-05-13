package com.hvtc.gradesystem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final JdbcTemplate jdbcTemplate;

    public StudentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("SELECT * FROM students ORDER BY id DESC");
    }

    @PostMapping
    public String add(@RequestBody Map<String, Object> body) {
        validateStudent(body);
        String sql = "INSERT INTO students(student_no, name, gender, class_name) VALUES(?, ?, ?, ?)";
        jdbcTemplate.update(sql, body.get("studentNo"), body.get("name"), body.get("gender"), body.get("className"));
        addLog(body, "添加学生", "添加学生：" + body.get("name"));
        return "添加学生成功";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        validateStudent(body);
        String sql = "UPDATE students SET student_no=?, name=?, gender=?, class_name=? WHERE id=?";
        jdbcTemplate.update(sql, body.get("studentNo"), body.get("name"), body.get("gender"), body.get("className"), id);
        addLog(body, "修改学生", "修改学生：" + body.get("name"));
        return "修改学生成功";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam(required = false) String operatorName,
                         @RequestParam(required = false) String operatorRole) {
        String name = jdbcTemplate.queryForObject("SELECT name FROM students WHERE id=?", String.class, id);
        jdbcTemplate.update("DELETE FROM scores WHERE student_id=?", id);
        jdbcTemplate.update("DELETE FROM students WHERE id=?", id);
        addLog(operatorName, operatorRole, "删除学生", "删除学生：" + name);
        return "删除学生成功";
    }

    private void validateStudent(Map<String, Object> body) {
        if (isBlank(body.get("studentNo"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号不能为空");
        }
        if (isBlank(body.get("name"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学生姓名不能为空");
        }
        if (isBlank(body.get("className"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "班级不能为空");
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
