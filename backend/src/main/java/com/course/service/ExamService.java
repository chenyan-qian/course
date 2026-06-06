package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.Exam;
import com.course.mapper.ExamMapper;
import org.springframework.stereotype.Service;

@Service
public class ExamService extends ServiceImpl<ExamMapper, Exam> {
}
