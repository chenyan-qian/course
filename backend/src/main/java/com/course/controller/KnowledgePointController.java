package com.course.controller;

import com.course.entity.KnowledgePoint;
import com.course.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge-point")
public class KnowledgePointController {

    @Autowired
    private KnowledgePointService kpService;

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // 树形结构
    @GetMapping("/tree")
    public List<KnowledgePoint> tree(HttpServletRequest request) {
        return kpService.buildTree(currentUserId(request));
    }

    // 查询某章节下的知识点
    @GetMapping
    public List<KnowledgePoint> list(HttpServletRequest request,
                                     @RequestParam(required = false) Long parentId) {
        var query = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, currentUserId(request));
        if (parentId != null) {
            query.eq(KnowledgePoint::getParentId, parentId);
        }
        query.orderByAsc(KnowledgePoint::getSortOrder)
             .orderByAsc(KnowledgePoint::getCreateTime);
        return query.list();
    }

    // 新增
    @PostMapping
    public Map<String, Object> add(@RequestBody KnowledgePoint kp, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        kp.setUserId(currentUserId(request));
        if (kp.getParentId() == null) kp.setParentId(0L);
        if (kp.getStatus() == null) kp.setStatus(0);
        if (kp.getSortOrder() == null) kp.setSortOrder(0);
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

    // 快速切换状态
    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id,
                                            @RequestBody Map<String, Integer> body,
                                            HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        KnowledgePoint exist = kpService.getById(id);
        if (exist == null || !exist.getUserId().equals(currentUserId(request))) {
            map.put("code", 403);
            map.put("msg", "无权操作");
            return map;
        }
        Integer status = body.get("status");
        if (status == null || status < 0 || status > 2) {
            map.put("code", 400);
            map.put("msg", "状态值无效(0/1/2)");
            return map;
        }
        exist.setStatus(status);
        boolean ok = kpService.updateById(exist);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "更新成功" : "更新失败");
        map.put("status", exist.getStatus());
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
        // 一并删除子节点
        kpService.lambdaUpdate()
                .eq(KnowledgePoint::getParentId, id)
                .remove();
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
        long count = kpService.lambdaQuery()
                .in(KnowledgePoint::getId, ids)
                .ne(KnowledgePoint::getUserId, userId)
                .count();
        if (count > 0) {
            map.put("code", 403);
            map.put("msg", "包含无权删除的数据");
            return map;
        }
        // 删除子节点
        for (Long id : ids) {
            kpService.lambdaUpdate()
                    .eq(KnowledgePoint::getParentId, id)
                    .remove();
        }
        boolean ok = kpService.removeBatchByIds(ids);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "批量删除成功" : "批量删除失败");
        return map;
    }
}
