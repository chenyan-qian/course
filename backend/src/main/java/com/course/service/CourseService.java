package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.Course;
import com.course.mapper.CourseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    /**
     * 检查课程时间是否与已有课程冲突
     * @param course 待检查的课程
     * @throws RuntimeException 时间冲突时抛出
     */
    public void checkTimeConflict(Course course) {
        int[] newRange = parseTimeSlot(course.getTimeSlot());

        // 查询同一天的所有课程，编辑时排除自身
        List<Course> sameDayCourses = lambdaQuery()
                .eq(Course::getWeekday, course.getWeekday())
                .ne(course.getId() != null, Course::getId, course.getId())
                .list();

        for (Course existing : sameDayCourses) {
            int[] existRange = parseTimeSlot(existing.getTimeSlot());
            // 区间重叠判断: a.start < b.end && b.start < a.end
            if (newRange[0] < existRange[1] && existRange[0] < newRange[1]) {
                throw new RuntimeException("时间冲突：「" + existing.getName()
                        + "」已占用 " + existing.getWeekday() + " " + existing.getTimeSlot());
            }
        }
    }

    /**
     * 解析时间字符串 "8:00-9:30" 为分钟数组 [startMinutes, endMinutes]
     */
    private int[] parseTimeSlot(String timeSlot) {
        try {
            String[] parts = timeSlot.split("-");
            return new int[]{timeToMinutes(parts[0].trim()), timeToMinutes(parts[1].trim())};
        } catch (Exception e) {
            throw new RuntimeException("时间格式错误，请使用如「8:00-9:30」的格式");
        }
    }

    private int timeToMinutes(String time) {
        String[] hm = time.split(":");
        return Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);
    }
}
