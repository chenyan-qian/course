package com.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("knowledge_point")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long parentId;    // 0=章节, 非0=归属某章节
    private String title;
    private String content;
    private String tags;
    private Integer status;   // 0-未学习 1-学习中 2-已掌握
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ---------- 树形展示用（非数据库字段） ----------
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<KnowledgePoint> children;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer total;      // 子节点总数

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer mastered;   // 已掌握数

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String courseName;  // 关联课程名
}
