package com.course.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.course.entity.KnowledgePoint;
import com.course.mapper.KnowledgePointMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointService extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> {
}
