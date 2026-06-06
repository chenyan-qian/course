package com.course.controller;

import com.course.entity.Exam;
import com.course.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // 查询（可选按课程筛选，默认按时间升序）
    @GetMapping
    public List<Exam> list(HttpServletRequest request,
                           @RequestParam(required = false) Long courseId) {
        var query = examService.lambdaQuery()
                .eq(Exam::getUserId, currentUserId(request));
        if (courseId != null) {
            query.eq(Exam::getCourseId, courseId);
        }
        query.orderByAsc(Exam::getExamDate);
        return query.list();
    }

    // 新增
    @PostMapping
    public Map<String, Object> add(@RequestBody Exam exam, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        exam.setUserId(currentUserId(request));
        boolean ok = examService.save(exam);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "添加成功" : "添加失败");
        if (ok) map.put("id", exam.getId());
        return map;
    }

    // 修改
    @PutMapping
    public Map<String, Object> update(@RequestBody Exam exam, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Exam exist = examService.getById(exam.getId());
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权修改");
            return map;
        }
        exam.setUserId(currentUserId(request));
        boolean ok = examService.updateById(exam);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "修改成功" : "修改失败");
        return map;
    }

    // 删除
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Exam exist = examService.getById(id);
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权删除");
            return map;
        }
        boolean ok = examService.removeById(id);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "删除成功" : "删除失败");
        return map;
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody List<Long> ids, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Long userId = currentUserId(request);
        long count = examService.lambdaQuery()
                .in(Exam::getId, ids)
                .ne(Exam::getUserId, userId)
                .count();
        if (count > 0) {
            map.put("code", 403);
            map.put("msg", "包含无权删除的数据");
            return map;
        }
        boolean ok = examService.removeBatchByIds(ids);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "批量删除成功" : "批量删除失败");
        return map;
    }
}
