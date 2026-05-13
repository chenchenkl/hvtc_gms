package com.hvtc.gradesystem.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {
    private final JdbcTemplate jdbcTemplate;

    public OperationLogController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            return jdbcTemplate.queryForList("SELECT * FROM operation_logs WHERE operator_name LIKE ? OR action_type LIKE ? OR action_content LIKE ? ORDER BY id DESC LIMIT 100", like, like, like);
        }
        return jdbcTemplate.queryForList("SELECT * FROM operation_logs ORDER BY id DESC LIMIT 100");
    }
}
