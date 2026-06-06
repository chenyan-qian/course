package com.course.controller;

import com.course.entity.Homework;
import com.course.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // 查询（可选筛选：courseId, status, dueSoon）
    @GetMapping
    public List<Homework> list(HttpServletRequest request,
                               @RequestParam(required = false) Long courseId,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) Boolean dueSoon) {
        Long userId = currentUserId(request);
        var query = homeworkService.lambdaQuery()
                .eq(Homework::getUserId, userId);

        if (courseId != null) {
            query.eq(Homework::getCourseId, courseId);
        }
        if (status != null) {
            query.eq(Homework::getStatus, status);
        }
        if (dueSoon != null && dueSoon) {
            // 离截止时间不到1天
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneDayLater = now.plusDays(1);
            query.gt(Homework::getDeadline, now)          // 还未截止
                 .lt(Homework::getDeadline, oneDayLater)   // 截止在1天内
                 .eq(Homework::getStatus, 0);              // 未完成
        }
        query.orderByAsc(Homework::getDeadline);
        query.orderByDesc(Homework::getCreateTime);
        return query.list();
    }

    // 新增
    @PostMapping
    public Map<String, Object> add(@RequestBody Homework hw, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        hw.setUserId(currentUserId(request));
        if (hw.getStatus() == null) hw.setStatus(0);
        boolean ok = homeworkService.save(hw);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "添加成功" : "添加失败");
        if (ok) map.put("id", hw.getId());
        return map;
    }

    // 修改
    @PutMapping
    public Map<String, Object> update(@RequestBody Homework hw, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Homework exist = homeworkService.getById(hw.getId());
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权修改");
            return map;
        }
        hw.setUserId(currentUserId(request));
        boolean ok = homeworkService.updateById(hw);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "修改成功" : "修改失败");
        return map;
    }

    // 切换完成状态
    @PutMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Homework exist = homeworkService.getById(id);
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权操作");
            return map;
        }
        exist.setStatus(exist.getStatus() == 1 ? 0 : 1);
        boolean ok = homeworkService.updateById(exist);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "操作成功" : "操作失败");
        map.put("status", exist.getStatus());
        return map;
    }

    // 删除
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Homework exist = homeworkService.getById(id);
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权删除");
            return map;
        }
        boolean ok = homeworkService.removeById(id);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "删除成功" : "删除失败");
        return map;
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody List<Long> ids, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Long userId = currentUserId(request);
        long count = homeworkService.lambdaQuery()
                .in(Homework::getId, ids)
                .ne(Homework::getUserId, userId)
                .count();
        if (count > 0) {
            map.put("code", 403);
            map.put("msg", "包含无权删除的数据");
            return map;
        }
        boolean ok = homeworkService.removeBatchByIds(ids);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "批量删除成功" : "批量删除失败");
        return map;
    }
}
