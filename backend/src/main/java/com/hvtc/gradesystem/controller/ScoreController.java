package com.hvtc.gradesystem.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {
    private final JdbcTemplate jdbcTemplate;

    public ScoreController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String studentName,
                                          @RequestParam(required = false) String courseName,
                                          @RequestParam(required = false) String className,
                                          @RequestParam(required = false) String term,
                                          @RequestParam(required = false) String userRole,
                                          @RequestParam(required = false) String realName,
                                          @RequestParam(required = false) String relatedNo) {
        QueryData queryData = buildScoreQuery(keyword, studentName, courseName, className, term, userRole, realName, relatedNo, false);
        return jdbcTemplate.queryForList(queryData.sql, queryData.params.toArray());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String studentName,
                                     @RequestParam(required = false) String courseName,
                                     @RequestParam(required = false) String className,
                                     @RequestParam(required = false) String term,
                                     @RequestParam(required = false) String userRole,
                                     @RequestParam(required = false) String realName,
                                     @RequestParam(required = false) String relatedNo) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) AS total_count, ")
                .append("ROUND(IFNULL(AVG(sc.score), 0), 2) AS avg_score, ")
                .append("IFNULL(MAX(sc.score), 0) AS max_score, ")
                .append("IFNULL(MIN(sc.score), 0) AS min_score, ")
                .append("SUM(CASE WHEN sc.score >= 60 THEN 1 ELSE 0 END) AS pass_count, ")
                .append("SUM(CASE WHEN sc.score < 60 THEN 1 ELSE 0 END) AS fail_count, ")
                .append("SUM(CASE WHEN sc.score >= 90 THEN 1 ELSE 0 END) AS excellent_count, ")
                .append("SUM(CASE WHEN sc.score >= 80 AND sc.score < 90 THEN 1 ELSE 0 END) AS good_count, ")
                .append("SUM(CASE WHEN sc.score >= 60 AND sc.score < 80 THEN 1 ELSE 0 END) AS normal_count ")
                .append("FROM scores sc ")
                .append("JOIN students st ON sc.student_id = st.id ")
                .append("JOIN courses co ON sc.course_id = co.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        addQueryConditions(sql, params, keyword, studentName, courseName, className, term, userRole, realName, relatedNo);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
        Number total = number(result.get("total_count"));
        Number pass = number(result.get("pass_count"));
        double passRate = total.intValue() == 0 ? 0 : pass.doubleValue() * 100 / total.doubleValue();
        result.put("pass_rate", Math.round(passRate * 100.0) / 100.0);
        return result;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String studentName,
                                         @RequestParam(required = false) String courseName,
                                         @RequestParam(required = false) String className,
                                         @RequestParam(required = false) String term,
                                         @RequestParam(required = false) String userRole,
                                         @RequestParam(required = false) String realName,
                                         @RequestParam(required = false) String relatedNo) {
        QueryData queryData = buildScoreQuery(keyword, studentName, courseName, className, term, userRole, realName, relatedNo, false);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(queryData.sql, queryData.params.toArray());

        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF");
        csv.append("学号,姓名,班级,课程编号,课程名称,任课教师,学分,成绩,学期\n");
        for (Map<String, Object> row : rows) {
            csv.append(value(row.get("student_no"))).append(',')
                    .append(value(row.get("student_name"))).append(',')
                    .append(value(row.get("class_name"))).append(',')
                    .append(value(row.get("course_no"))).append(',')
                    .append(value(row.get("course_name"))).append(',')
                    .append(value(row.get("teacher_name"))).append(',')
                    .append(value(row.get("credit"))).append(',')
                    .append(value(row.get("score"))).append(',')
                    .append(value(row.get("term"))).append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grades-v1.2.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping
    public String add(@RequestBody Map<String, Object> body) {
        validatePermission(body, "add", null);
        validateScoreBody(body, true);
        String sql = "INSERT INTO scores(student_id, course_id, score, term) VALUES(?, ?, ?, ?)";
        jdbcTemplate.update(sql, body.get("studentId"), body.get("courseId"), body.get("score"), body.get("term"));
        addLog(body, "录入成绩", "录入成绩：学生ID " + body.get("studentId") + "，课程ID " + body.get("courseId") + "，分数 " + body.get("score"));
        return "录入成绩成功";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        validatePermission(body, "update", id);
        validateScoreBody(body, false);
        String sql = "UPDATE scores SET score=?, term=? WHERE id=?";
        jdbcTemplate.update(sql, body.get("score"), body.get("term"), id);
        addLog(body, "修改成绩", "修改成绩ID " + id + "，新分数 " + body.get("score"));
        return "修改成绩成功";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam(required = false) String operatorName,
                         @RequestParam(required = false) String operatorRole) {
        if ("student".equals(operatorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学生不能删除成绩");
        }
        if ("teacher".equals(operatorRole) && !canTeacherOperateScore(id, operatorName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "教师只能删除自己课程的成绩");
        }
        jdbcTemplate.update("DELETE FROM scores WHERE id=?", id);
        addLog(operatorName, operatorRole, "删除成绩", "删除成绩ID：" + id);
        return "删除成绩成功";
    }

    private QueryData buildScoreQuery(String keyword,
                                      String studentName,
                                      String courseName,
                                      String className,
                                      String term,
                                      String userRole,
                                      String realName,
                                      String relatedNo,
                                      boolean onlyCount) {
        StringBuilder sql = new StringBuilder();
        if (onlyCount) {
            sql.append("SELECT COUNT(*) ");
        } else {
            sql.append("SELECT sc.id, st.id AS student_id, co.id AS course_id, ")
                    .append("st.student_no, st.name AS student_name, st.class_name, ")
                    .append("co.course_no, co.course_name, co.teacher_name, co.credit, sc.score, sc.term ");
        }
        sql.append("FROM scores sc ")
                .append("JOIN students st ON sc.student_id = st.id ")
                .append("JOIN courses co ON sc.course_id = co.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        addQueryConditions(sql, params, keyword, studentName, courseName, className, term, userRole, realName, relatedNo);
        if (!onlyCount) {
            sql.append("ORDER BY sc.id DESC");
        }
        return new QueryData(sql.toString(), params);
    }

    private void addQueryConditions(StringBuilder sql,
                                    List<Object> params,
                                    String keyword,
                                    String studentName,
                                    String courseName,
                                    String className,
                                    String term,
                                    String userRole,
                                    String realName,
                                    String relatedNo) {
        if (hasText(keyword)) {
            sql.append("AND (st.name LIKE ? OR st.student_no LIKE ? OR co.course_name LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (hasText(studentName)) {
            sql.append("AND st.name LIKE ? ");
            params.add("%" + studentName.trim() + "%");
        }
        if (hasText(courseName)) {
            sql.append("AND co.course_name LIKE ? ");
            params.add("%" + courseName.trim() + "%");
        }
        if (hasText(className)) {
            sql.append("AND st.class_name LIKE ? ");
            params.add("%" + className.trim() + "%");
        }
        if (hasText(term)) {
            sql.append("AND sc.term LIKE ? ");
            params.add("%" + term.trim() + "%");
        }
        if ("student".equals(userRole)) {
            if (hasText(relatedNo)) {
                sql.append("AND st.student_no = ? ");
                params.add(relatedNo);
            } else if (hasText(realName)) {
                sql.append("AND st.name = ? ");
                params.add(realName);
            }
        }
        if ("teacher".equals(userRole) && hasText(realName)) {
            sql.append("AND co.teacher_name = ? ");
            params.add(realName);
        }
    }

    private void validatePermission(Map<String, Object> body, String action, Integer scoreId) {
        String role = String.valueOf(body.getOrDefault("operatorRole", ""));
        String operatorName = String.valueOf(body.getOrDefault("operatorName", ""));
        if ("student".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学生不能录入、修改或删除成绩");
        }
        if ("teacher".equals(role)) {
            boolean allowed;
            if ("add".equals(action)) {
                Integer courseId = parseInt(body.get("courseId"));
                allowed = canTeacherOperateCourse(courseId, operatorName);
            } else {
                allowed = canTeacherOperateScore(scoreId, operatorName);
            }
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "教师只能操作自己课程的成绩");
            }
        }
    }

    private boolean canTeacherOperateCourse(Integer courseId, String teacherName) {
        if (courseId == null || !hasText(teacherName)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courses WHERE id=? AND teacher_name=?", Integer.class, courseId, teacherName);
        return count != null && count > 0;
    }

    private boolean canTeacherOperateScore(Integer scoreId, String teacherName) {
        if (scoreId == null || !hasText(teacherName)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM scores sc JOIN courses co ON sc.course_id=co.id WHERE sc.id=? AND co.teacher_name=?", Integer.class, scoreId, teacherName);
        return count != null && count > 0;
    }

    private void validateScoreBody(Map<String, Object> body, boolean needStudentAndCourse) {
        if (needStudentAndCourse) {
            if (body.get("studentId") == null || body.get("studentId").toString().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学生");
            }
            if (body.get("courseId") == null || body.get("courseId").toString().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择课程");
            }
        }
        if (body.get("term") == null || body.get("term").toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写学期");
        }
        if (body.get("score") == null || body.get("score").toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写成绩");
        }

        double score;
        try {
            score = Double.parseDouble(body.get("score").toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成绩必须是数字");
        }

        if (score < 0 || score > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成绩必须在0到100之间");
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

    private Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return 0;
    }

    private Integer parseInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String value(Object object) {
        if (object == null) {
            return "";
        }
        String text = object.toString();
        if (text.contains(",") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim());
    }

    private static class QueryData {
        private final String sql;
        private final List<Object> params;

        private QueryData(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
