package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.Course;
import com.course.mapper.CourseMapper;
import org.springframework.stereotype.Service;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {
}
