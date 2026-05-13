package com.hvtc.gradesystem.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {
    private final JdbcTemplate jdbcTemplate;

    public LoginController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        String sql = "SELECT id, username, real_name, role, status, related_no FROM users WHERE username = ? AND password = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username, password);

        Map<String, Object> result = new HashMap<>();
        if (users.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }

        Map<String, Object> user = users.get(0);
        if ("禁用".equals(user.get("status"))) {
            result.put("success", false);
            result.put("message", "该账号已被禁用，请联系管理员");
            return result;
        }

        result.put("success", true);
        result.put("user", user);
        return result;
    }
}
