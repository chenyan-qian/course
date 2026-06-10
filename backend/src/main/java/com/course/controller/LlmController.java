package com.course.controller;

import com.course.entity.Course;
import com.course.service.CourseService;
import com.course.service.FileParserService;
import com.course.service.LlmService;
import com.course.service.OcrService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    @Autowired
    private LlmService llmService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private FileParserService fileParserService;

    @Autowired
    private CourseService courseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        Map<String, Object> map = new HashMap<>();
        String message = request.get("message");
        String model = request.getOrDefault("model", "qwen-turbo");
        
        if (message == null || message.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "消息不能为空");
            return map;
        }

        String response = llmService.chat(message, model);
        map.put("code", 200);
        map.put("data", response);
        return map;
    }

    @PostMapping("/summarize")
    public Map<String, Object> summarize(@RequestBody Map<String, String> request) {
        Map<String, Object> map = new HashMap<>();
        String text = request.get("text");
        
        if (text == null || text.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "文本不能为空");
            return map;
        }

        String summary = llmService.summarizeText(text);
        map.put("code", 200);
        map.put("data", summary);
        return map;
    }

    @PostMapping("/generate-questions")
    public Map<String, Object> generateQuestions(@RequestBody Map<String, Object> request) {
        Map<String, Object> map = new HashMap<>();
        String topic = (String) request.get("topic");
        Integer count = (Integer) request.getOrDefault("count", 5);
        
        if (topic == null || topic.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "主题不能为空");
            return map;
        }

        String questions = llmService.generateQuestions(topic, count);
        map.put("code", 200);
        map.put("data", questions);
        return map;
    }

    @PostMapping("/homework-feedback")
    public Map<String, Object> homeworkFeedback(@RequestBody Map<String, String> request) {
        Map<String, Object> map = new HashMap<>();
        String studentAnswer = request.get("studentAnswer");
        String correctAnswer = request.get("correctAnswer");
        
        if (studentAnswer == null || correctAnswer == null) {
            map.put("code", 400);
            map.put("msg", "参数不完整");
            return map;
        }

        String feedback = llmService.improveHomeworkFeedback(studentAnswer, correctAnswer);
        map.put("code", 200);
        map.put("data", feedback);
        return map;
    }

    @PostMapping("/recognize-course")
    public Map<String, Object> recognizeCourse(@RequestBody Map<String, String> request) {
        Map<String, Object> map = new HashMap<>();
        String ocrText = request.get("ocrText");
        
        if (ocrText == null || ocrText.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "OCR文本不能为空");
            return map;
        }

        String prompt = String.format(
            "请从以下课程表文字中提取课程信息，返回JSON数组格式。\n" +
            "每门课程包含：name(课程名), teacher(教师), classroom(教室), weekday(星期几如'周一'), timeSlot(时间段如'8:00-9:30')\n" +
            "如果无法提取某字段则设为null。\n\n原始文字：\n%s", 
            ocrText
        );
        
        String result = llmService.chat(prompt);
        map.put("code", 200);
        map.put("data", result);
        return map;
    }

    /**
     * 上传文件（Word/PDF/图片）智能添加课程：提取文字 → LLM解析 → 保存到数据库
     */
    @PostMapping("/add-course-by-file")
    public Map<String, Object> addCourseByFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> map = new HashMap<>();

        if (file.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "文件为空");
            return map;
        }

        try {
            // 1. 从文件中提取文字（使用阿里云OCR或文本提取）
            String fileText = extractTextFromFile(file);
            System.out.println("[LLM-File] 提取文字长度: " + fileText.length());

            if (fileText.isEmpty()) {
                map.put("code", 400);
                map.put("msg", "未能从文件中提取到文字内容");
                return map;
            }

            // 2. 用 LLM 解析文字 → 课程 JSON 数组
            String llmResult = llmService.parseCoursesFromFileText(fileText);
            System.out.println("[LLM-File] LLM 返回原始结果: " + llmResult);
            System.out.println("[LLM-File] LLM 返回长度: " + llmResult.length());

            // 3. 清理 JSON
            String json = llmResult.trim();
            if (json.startsWith("``")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }

            // 4. 解析并保存
            int success = 0;
            List<String> errors = new ArrayList<>();
            List<Course> savedCourses = new ArrayList<>();

            // 判断是数组还是单个对象
            json = json.trim();
            List<Map<String, Object>> courseList;
            if (json.startsWith("[")) {
                // JSON 数组: 多门课程
                courseList = objectMapper.readValue(
                        json, new TypeReference<List<Map<String, Object>>>() {});
            } else if (json.startsWith("{")) {
                // JSON 对象: 单门课程
                Map<String, Object> single = objectMapper.readValue(
                        json, new TypeReference<Map<String, Object>>() {});
                courseList = Collections.singletonList(single);
            } else {
                map.put("code", 500);
                map.put("msg", "AI 返回格式异常，原始回复:\n" + llmResult);
                return map;
            }

            // 逐门保存
            for (Map<String, Object> item : courseList) {
                try {
                    System.out.println("[LLM-File] 解析课程项: " + item);
                    
                    Course course = objectMapper.convertValue(item, Course.class);
                    
                    // 检查必填字段
                    if (course.getName() == null || course.getName().trim().isEmpty()) {
                        errors.add("课程名称为空，跳过: " + item);
                        continue;
                    }
                    
                    // 设置 ID 为 null（让数据库自动生成）
                    course.setId(null);
                    
                    // 检查时间冲突
                    courseService.checkTimeConflict(course);
                    
                    // 保存到数据库
                    if (courseService.save(course)) {
                        success++;
                        savedCourses.add(course);
                        System.out.println("[LLM-File] 成功保存课程: " + course.getName());
                    } else {
                        errors.add("「" + course.getName() + "」保存失败");
                    }
                } catch (RuntimeException e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("name")) {
                        errorMsg = "课程名称不能为空";
                    }
                    errors.add(errorMsg != null ? errorMsg : "未知错误: " + item);
                    System.err.println("[LLM-File] 保存失败: " + e.getMessage());
                } catch (Exception e) {
                    errors.add("解析异常: " + e.getMessage());
                    System.err.println("[LLM-File] 解析异常: " + e.getMessage());
                }
            }

            map.put("code", 200);
            map.put("success", success);
            map.put("fail", errors.size());
            map.put("errors", errors);
            map.put("data", savedCourses);
            map.put("rawText", fileText);
            map.put("msg", "成功导入 " + success + " 门课程"
                    + (errors.size() > 0 ? "，" + errors.size() + " 门失败" : ""));
        } catch (Exception e) {
            System.err.println("[LLM-File] 异常: " + e.getMessage());
            e.printStackTrace();
            map.put("code", 500);
            map.put("msg", "处理失败: " + e.getMessage());
        }

        return map;
    }

    /**
     * 从文件中提取文字（支持图片、PDF、Word）
     */
    private String extractTextFromFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名为空");
        }

        String lower = filename.toLowerCase();
        
        // 图片文件：使用阿里云 OCR
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp") || lower.endsWith(".gif")) {
            // 调用 OcrService 的 recognizeCourses 方法获取原始文本
            List<Map<String, String>> courses = ocrService.recognizeCourses(file);
            if (!courses.isEmpty() && courses.get(0).containsKey("_rawText")) {
                return courses.get(0).get("_rawText");
            }
            // 如果没有 _rawText，拼接所有字段
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> course : courses) {
                for (String value : course.values()) {
                    if (value != null && !value.isEmpty()) {
                        sb.append(value).append(" ");
                    }
                }
            }
            return sb.toString();
        } 
        // PDF/Word：使用 FileParserService
        else if (lower.endsWith(".pdf") || lower.endsWith(".docx")) {
            return fileParserService.extractText(file);
        } else {
            throw new RuntimeException("不支持的文件格式: " + lower);
        }
    }
}
