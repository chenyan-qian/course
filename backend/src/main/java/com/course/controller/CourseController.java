package com.course.controller;

import com.course.entity.Course;
import com.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    // 查询全部
    @GetMapping
    public List<Course> list() {
        return courseService.list();
    }

    // 新增
    @PostMapping
    public Map<String, Object> add(@RequestBody Course course) {
        Map<String, Object> map = new HashMap<>();
        boolean ok = courseService.save(course);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "添加成功" : "添加失败");
        return map;
    }

    // 修改
    @PutMapping
    public Map<String, Object> update(@RequestBody Course course) {
        Map<String, Object> map = new HashMap<>();
        boolean ok = courseService.updateById(course);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "修改成功" : "修改失败");
        return map;
    }

    // 删除
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> map = new HashMap<>();
        boolean ok = courseService.removeById(id);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "删除成功" : "删除失败");
        return map;
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody List<Long> ids) {
        Map<String, Object> map = new HashMap<>();
        boolean ok = courseService.removeBatchByIds(ids);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "批量删除成功" : "批量删除失败");
        return map;
    }
}
