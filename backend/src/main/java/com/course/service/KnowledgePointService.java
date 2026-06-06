package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.KnowledgePoint;
import com.course.mapper.KnowledgePointMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgePointService extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> {

    /**
     * 获取当前用户的树形知识点结构（按课程 → 章节 → 知识点）
     */
    public List<KnowledgePoint> buildTree(Long userId) {
        // 查全部数据
        List<KnowledgePoint> all = lambdaQuery()
                .eq(KnowledgePoint::getUserId, userId)
                .orderByAsc(KnowledgePoint::getSortOrder)
                .orderByAsc(KnowledgePoint::getCreateTime)
                .list();

        // 按 courseId 分组
        Map<Long, List<KnowledgePoint>> byCourse = all.stream()
                .collect(Collectors.groupingBy(kp -> kp.getCourseId() != null ? kp.getCourseId() : 0L));

        List<KnowledgePoint> tree = new ArrayList<>();

        for (Map.Entry<Long, List<KnowledgePoint>> entry : byCourse.entrySet()) {
            Long courseId = entry.getKey();
            List<KnowledgePoint> items = entry.getValue();

            // 章节（parentId == 0）
            List<KnowledgePoint> chapters = items.stream()
                    .filter(kp -> kp.getParentId() == null || kp.getParentId() == 0)
                    .collect(Collectors.toList());

            // 知识点（parentId > 0）
            Map<Long, List<KnowledgePoint>> childMap = items.stream()
                    .filter(kp -> kp.getParentId() != null && kp.getParentId() > 0)
                    .collect(Collectors.groupingBy(KnowledgePoint::getParentId));

            for (KnowledgePoint chapter : chapters) {
                List<KnowledgePoint> children = childMap.getOrDefault(chapter.getId(), new ArrayList<>());
                chapter.setChildren(children);
                chapter.setTotal(children.size());
                chapter.setMastered((int) children.stream().filter(c -> c.getStatus() != null && c.getStatus() == 2).count());
            }

            // 汇总课程级统计
            int total = 0, mastered = 0;
            for (KnowledgePoint ch : chapters) {
                total += ch.getTotal();
                mastered += ch.getMastered();
            }

            // 用第一个 item 携带课程汇总信息，作为课程节点
            if (!chapters.isEmpty() || !items.isEmpty()) {
                KnowledgePoint courseNode = new KnowledgePoint();
                courseNode.setCourseId(courseId);
                courseNode.setChildren(chapters);
                courseNode.setTotal(total);
                courseNode.setMastered(mastered);
                tree.add(courseNode);
            }
        }
        return tree;
    }
}
