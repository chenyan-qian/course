package com.course.service;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
public class OcrService {

    @Value("${aliyun.ocr.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.ocr.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.ocr.endpoint:ocr-api.cn-hangzhou.aliyuncs.com}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }

    public List<Map<String, String>> recognizeCourses(MultipartFile file) throws Exception {
        File tempFile = File.createTempFile("ocr_upload_", ".jpg");
        try {
            file.transferTo(tempFile);
            System.out.println("[OCR] === 开始阿里云OCR识别 === 图片大小: " + file.getSize() + " bytes");

            Client client = createClient();
            
            try (FileInputStream fis = new FileInputStream(tempFile)) {
                RecognizeAdvancedRequest request = new RecognizeAdvancedRequest()
                        .setBody(fis);

                RecognizeAdvancedResponse response = client.recognizeAdvanced(request);
                String result = response.getBody().getData();
                
                System.out.println("[OCR] 阿里云返回的原始结果: " + result);

                if (result == null || result.isEmpty()) {
                    System.out.println("[OCR] 阿里云OCR返回空结果");
                    return buildFallbackResult("阿里云OCR未识别到任何文字");
                }

                List<TextBlock> blocks = parseBlocksFromJson(result);
                System.out.println("[OCR] 解析出 " + blocks.size() + " 个文字块");
                
                if (!blocks.isEmpty()) {
                    List<Map<String, String>> courses = parseOcrToCourses(blocks);
                    if (!courses.isEmpty()) {
                        System.out.println("[OCR] === 识别成功 === 解析出 " + courses.size() + " 条课程");
                        return courses;
                    }
                }

                System.out.println("[OCR] 无法解析出结构化课程，返回原始文本");
                return buildFallbackResult(result);
            }

        } catch (Exception e) {
            System.err.println("[OCR] 识别异常: " + e.getMessage());
            e.printStackTrace();
            return buildFallbackResult("识别异常: " + e.getMessage());
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private List<Map<String, String>> buildFallbackResult(String text) {
        List<Map<String, String>> fallback = new ArrayList<>();
        Map<String, String> raw = new LinkedHashMap<>();
        
        // 尝试从 JSON 中提取 content 字段
        if (text != null && text.startsWith("{")) {
            try {
                JsonNode rootNode = objectMapper.readTree(text);
                JsonNode contentNode = rootNode.get("content");
                if (contentNode != null && !contentNode.asText().isEmpty()) {
                    raw.put("_rawText", contentNode.asText());
                    fallback.add(raw);
                    return fallback;
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        raw.put("_rawText", text != null && !text.isEmpty() ? text : "阿里云OCR未识别到课程信息");
        fallback.add(raw);
        return fallback;
    }

    private List<TextBlock> parseBlocksFromJson(String jsonResult) {
        List<TextBlock> blocks = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResult);

            // 使用 prism_wordsInfo
            JsonNode wordsInfoNode = rootNode.get("prism_wordsInfo");
            if (wordsInfoNode == null || !wordsInfoNode.isArray()) {
                System.out.println("[OCR] 未找到prism_wordsInfo字段");
                return blocks;
            }

            for (JsonNode item : wordsInfoNode) {
                String word = item.get("word").asText("");
                if (word == null || word.trim().isEmpty()) continue;

                int x = item.get("x").asInt(0);
                int y = item.get("y").asInt(0);
                
                // 不过滤 y 坐标，保留所有文字块让 AI 判断
                int width = item.get("width").asInt(0);
                int height = item.get("height").asInt(0);

                blocks.add(new TextBlock(word.trim(), x, y, width, height));
            }

            System.out.println("[OCR] 解析出 " + blocks.size() + " 个文字块");
        } catch (Exception e) {
            System.err.println("[OCR] JSON解析失败: " + e.getMessage());
            e.printStackTrace();
        }

        return blocks;
    }

    private List<Map<String, String>> parseOcrToCourses(List<TextBlock> blocks) {
        if (blocks.isEmpty()) return new ArrayList<>();

        Map<String, String> course = new LinkedHashMap<>();
        String name = null, teacher = null, classroom = null, weekday = null, timeSlot = null;

        // 表头关键词列表
        Set<String> headers = Set.of("授课教师", "授课周数", "授课地点", "授课时间", 
                                      "查看回放", "修改课程", "添加课程", "课程", "功能");

        for (TextBlock b : blocks) {
            String w = b.text;

            // 跳过表头和按钮文字
            if (headers.contains(w)) {
                continue;
            }

            // 识别星期
            String wd = detectWeekday(w);
            if (wd != null) {
                weekday = wd;
                continue;
            }

            // 识别时间段
            if (w.matches("\\d{1,2}[：:]\\d{2}.*\\d{1,2}[：:]\\d{2}")) {
                timeSlot = w.replaceAll("：", ":").trim();
                continue;
            }

            // 识别教室（包含"楼"或"室"）
            if ((w.contains("楼") || w.contains("室")) && classroom == null) {
                classroom = w;
                continue;
            }

            // 识别教师（2-4个汉字，且前面有"授课教师"标签）
            if (w.matches("^[\\u4e00-\\u9fa5]{2,4}$") && teacher == null) {
                teacher = w;
                continue;
            }

            // 识别课程名（较长文本，且不包含数字）
            if (w.length() > 4 && !w.matches(".*\\d.*") && name == null) {
                name = w;
                continue;
            }
        }

        if (name == null && weekday == null && timeSlot == null) {
            return new ArrayList<>();
        }

        course.put("name", nvl(name));
        course.put("teacher", nvl(teacher));
        course.put("classroom", nvl(classroom));
        course.put("weekday", nvl(weekday));
        course.put("timeSlot", nvl(timeSlot));

        System.out.println("[OCR] 解析结果: " + course);
        return Collections.singletonList(course);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean isHeaderWord(String w) {
        return Set.of("课程名称", "授课教师", "教师", "教室", "星期", "时间",
                "课程表", "节次", "序号", "备注", "上课时间", "地点").contains(w);
    }

    private boolean isClassroom(String w) {
        return w.contains("教室") || w.contains("实验楼") || w.contains("教学楼")
                || (w.contains("楼") && w.length() <= 10)
                || w.matches(".*[A-Za-z]\\d{3,5}.*");
    }

    private boolean isTeacher(String w) {
        return w.contains("老师") || w.contains("教师") || w.contains("教授")
                || w.contains("讲师");
    }

    private String cleanTeacher(String w) {
        return w.replaceAll("(老师|教师|教授|讲师)", "").trim();
    }

    private String detectWeekday(String text) {
        if (text.contains("周一") || text.contains("星期一")) return "周一";
        if (text.contains("周二") || text.contains("星期二")) return "周二";
        if (text.contains("周三") || text.contains("星期三")) return "周三";
        if (text.contains("周四") || text.contains("星期四")) return "周四";
        if (text.contains("周五") || text.contains("星期五")) return "周五";
        if (text.contains("周六") || text.contains("星期六")) return "周六";
        if (text.contains("周日") || text.contains("星期日") || text.contains("星期天")) return "周日";
        return null;
    }

    private static class TextBlock {
        String text;
        int x, y, width, height;

        TextBlock(String t, int x, int y, int w, int h) {
            this.text = t;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }
}
