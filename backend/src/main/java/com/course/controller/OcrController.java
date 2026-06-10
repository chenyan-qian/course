package com.course.controller;

import com.course.entity.Course;
import com.course.service.CourseService;
import com.course.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private CourseService courseService;

    /**
     * 上传图片进行 OCR 识别，返回解析出的课程列表
     */
    @PostMapping("/recognize")
    public Map<String, Object> recognize(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> courses = ocrService.recognizeCourses(file);
            result.put("code", 200);
            result.put("data", courses);
            result.put("msg", "识别完成，共 " + courses.size() + " 条记录");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "识别失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 确认导入：批量保存识别的课程
     */
    @PostMapping("/import")
    public Map<String, Object> importCourses(@RequestBody List<Course> courses) {
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        for (Course course : courses) {
            try {
                course.setId(null); // 确保新增
                courseService.checkTimeConflict(course);
                boolean ok = courseService.save(course);
                if (ok) {
                    success++;
                } else {
                    fail++;
                    errors.add("「" + course.getName() + "」保存失败");
                }
            } catch (RuntimeException e) {
                fail++;
                errors.add("「" + course.getName() + "」" + e.getMessage());
            }
        }

        result.put("code", 200);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        result.put("msg", "成功导入 " + success + " 门课程" + (fail > 0 ? "，" + fail + " 门失败" : ""));
        return result;
    }
}
