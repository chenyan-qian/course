package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.Homework;
import com.course.mapper.HomeworkMapper;
import org.springframework.stereotype.Service;

@Service
public class HomeworkService extends ServiceImpl<HomeworkMapper, Homework> {
}
