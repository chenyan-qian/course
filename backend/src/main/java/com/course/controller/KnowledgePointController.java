package com.course.controller;

import com.course.entity.KnowledgePoint;
import com.course.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-point")
public class KnowledgePointController {

    @Autowired
    private KnowledgePointService kpService;

    // 获取当前登录用户 ID
    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // 查询当前用户的知识点（可指定课程筛选）
    @GetMapping
    public List<KnowledgePoint> list(HttpServletRequest request,
                                      @RequestParam(required = false) Long courseId) {
        var query = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, currentUserId(request));
        if (courseId != null) {
            query.eq(KnowledgePoint::getCourseId, courseId);
        }
        query.orderByDesc(KnowledgePoint::getUpdateTime);
        return query.list();
    }

    // 新增
    @PostMapping
    public Map<String, Object> add(@RequestBody KnowledgePoint kp, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        kp.setUserId(currentUserId(request));
        boolean ok = kpService.save(kp);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "添加成功" : "添加失败");
        if (ok) map.put("id", kp.getId());
        return map;
    }

    // 修改
    @PutMapping
    public Map<String, Object> update(@RequestBody KnowledgePoint kp, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        // 只能修改自己的知识点
        KnowledgePoint exist = kpService.getById(kp.getId());
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权修改");
            return map;
        }
        kp.setUserId(currentUserId(request));
        boolean ok = kpService.updateById(kp);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "修改成功" : "修改失败");
        return map;
    }

    // 删除
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        KnowledgePoint exist = kpService.getById(id);
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权删除");
            return map;
        }
        boolean ok = kpService.removeById(id);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "删除成功" : "删除失败");
        return map;
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody List<Long> ids, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Long userId = currentUserId(request);

        // 校验所有权：所有选中的知识点都必须属于当前用户
        long count = kpService.lambdaQuery()
                .in(KnowledgePoint::getId, ids)
                .ne(KnowledgePoint::getUserId, userId)
                .count();
        if (count > 0) {
            map.put("code", 403);
            map.put("msg", "包含无权删除的数据");
            return map;
        }

        boolean ok = kpService.removeBatchByIds(ids);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "批量删除成功" : "批量删除失败");
        return map;
    }
}
