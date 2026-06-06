package com.course.controller;

import com.course.entity.Course;
import com.course.entity.KnowledgePoint;
import com.course.service.CourseService;
import com.course.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private KnowledgePointService kpService;

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping
    public Map<String, Object> statistics(HttpServletRequest request) {
        Long userId = currentUserId(request);
        Map<String, Object> result = new HashMap<>();

        // ========== 汇总统计 ==========
        long totalCourses = courseService.count();
        long totalKnowledgePoints = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, userId)
                .gt(KnowledgePoint::getParentId, 0)
                .count();
        long mastered = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, userId)
                .eq(KnowledgePoint::getStatus, 2)
                .gt(KnowledgePoint::getParentId, 0)
                .count();
        long learning = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, userId)
                .eq(KnowledgePoint::getStatus, 1)
                .gt(KnowledgePoint::getParentId, 0)
                .count();
        long notStarted = kpService.lambdaQuery()
                .eq(KnowledgePoint::getUserId, userId)
                .eq(KnowledgePoint::getStatus, 0)
                .gt(KnowledgePoint::getParentId, 0)
                .count();

        result.put("totalCourses", totalCourses);
        result.put("totalKnowledgePoints", totalKnowledgePoints);
        result.put("mastered", mastered);
        result.put("learning", learning);
        result.put("notStarted", notStarted);

        // ========== 课程分布（按星期） ==========
        List<Course> courses = courseService.list();
        Map<String, Long> weekdayCount = new LinkedHashMap<>();
        // 按固定顺序
        String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (String wd : weekdays) {
            weekdayCount.put(wd, 0L);
        }
        for (Course c : courses) {
            if (c.getWeekday() != null && weekdayCount.containsKey(c.getWeekday())) {
                weekdayCount.put(c.getWeekday(), weekdayCount.get(c.getWeekday()) + 1);
            }
        }
        result.put("courseDistribution", weekdayCount);

        // ========== 学习进度（饼图数据） ==========
        List<Map<String, Object>> progressList = new ArrayList<>();
        Map<String, Object> masteredItem = new LinkedHashMap<>();
        masteredItem.put("name", "已掌握");
        masteredItem.put("value", mastered);
        progressList.add(masteredItem);

        Map<String, Object> learningItem = new LinkedHashMap<>();
        learningItem.put("name", "学习中");
        learningItem.put("value", learning);
        progressList.add(learningItem);

        Map<String, Object> notStartedItem = new LinkedHashMap<>();
        notStartedItem.put("name", "未学习");
        notStartedItem.put("value", notStarted);
        progressList.add(notStartedItem);

        result.put("learningProgress", progressList);

        return result;
    }
}
