package com.hvtc.gradesystem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final JdbcTemplate jdbcTemplate;

    public UserController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("SELECT id, username, real_name, role, status, related_no FROM users ORDER BY id DESC");
    }

    @PostMapping
    public String add(@RequestBody Map<String, Object> body) {
        validateUser(body, true);
        String sql = "INSERT INTO users(username, password, real_name, role, status, related_no) VALUES(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                body.get("username"),
                body.get("password"),
                body.get("realName"),
                body.get("role"),
                body.getOrDefault("status", "启用"),
                body.get("relatedNo"));
        addLog(body, "添加用户", "添加用户：" + body.get("username"));
        return "添加用户成功";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        validateUser(body, false);
        String sql = "UPDATE users SET real_name=?, role=?, status=?, related_no=? WHERE id=?";
        jdbcTemplate.update(sql,
                body.get("realName"),
                body.get("role"),
                body.getOrDefault("status", "启用"),
                body.get("relatedNo"),
                id);
        addLog(body, "修改用户", "修改用户信息：" + body.get("realName"));
        return "修改用户成功";
    }

    @PutMapping("/{id}/password")
    public String resetPassword(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String newPassword = String.valueOf(body.getOrDefault("password", "123456"));
        if (!hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能为空");
        }
        jdbcTemplate.update("UPDATE users SET password=? WHERE id=?", newPassword, id);
        addLog(body, "重置密码", "重置用户ID为" + id + "的密码");
        return "重置密码成功";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam(required = false) String operatorName,
                         @RequestParam(required = false) String operatorRole) {
        String username = jdbcTemplate.queryForObject("SELECT username FROM users WHERE id=?", String.class, id);
        jdbcTemplate.update("DELETE FROM users WHERE id=?", id);
        addLog(operatorName, operatorRole, "删除用户", "删除用户：" + username);
        return "删除用户成功";
    }

    private void validateUser(Map<String, Object> body, boolean needPassword) {
        if (isBlank(body.get("username")) && needPassword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        if (needPassword && isBlank(body.get("password"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }
        if (isBlank(body.get("realName"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名不能为空");
        }
        if (isBlank(body.get("role"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不能为空");
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
