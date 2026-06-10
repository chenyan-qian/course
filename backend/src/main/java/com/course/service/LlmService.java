package com.course.service;




import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class LlmService {

    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;

    public String chat(String userMessage) {
        return chat(userMessage, "qwen-turbo");
    }

    public String chat(String userMessage, String model) {
        try {
            Generation gen = new Generation();
            
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个教育助手，帮助用户管理课程、知识点和考试。")
                    .build();
            
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI服务调用失败：" + e.getMessage();
        }
    }

    public String summarizeText(String text) {
        String prompt = "请对以下内容进行简洁总结，提取关键点：\n\n" + text;
        return chat(prompt);
    }

    public String generateQuestions(String topic, int count) {
        String prompt = String.format("请围绕'%s'主题生成%d道选择题，每题包含题目、4个选项和正确答案。\n格式：\n1. 题目\nA. 选项1\nB. 选项2\nC. 选项3\nD. 选项4\n答案：X", topic, count);
        return chat(prompt);
    }

    public String improveHomeworkFeedback(String studentAnswer, String correctAnswer) {
        String prompt = String.format("学生答案：%s\n正确答案：%s\n请给出评价和改进建议。", studentAnswer, correctAnswer);
        return chat(prompt);
    }

    /**
     * 从自然语言描述中解析课程信息，返回结构化 JSON
     */
    public String parseCourseFromText(String userText) {
        String prompt = String.format(
            "你是一个课程管理助手。请从以下用户描述中提取课程信息，严格返回纯JSON对象（不要代码块标记）。\n" +
            "JSON格式：{\"name\":\"课程名\",\"teacher\":\"授课教师\",\"classroom\":\"教室地点\",\"weekday\":\"周X\",\"timeSlot\":\"HH:MM-HH:MM\"}\n" +
            "规则：\n" +
            "1. weekday 必须是 周一/周二/周三/周四/周五/周六/周日 之一\n" +
            "2. timeSlot 格式必须是 HH:MM-HH:MM\n" +
            "3. 无法提取的字段填 null\n" +
            "4. 只返回JSON，不要其他文字\n\n" +
            "用户描述：%s", userText);
        return chat(prompt);
    }

    /**
     * 从文件提取的文字中解析课程（可能包含多门课程），返回 JSON 数组
     */
    public String parseCoursesFromFileText(String fileText) {
        String prompt = String.format(
            "你是课程表识别助手。以下是从课程表截图/文件中提取的文字内容。\n\n" +
            "**重要规则：**\n" +
            "1. 课程名称通常是较长的中文文本，如'计算机组成与系统结构'、'数据库系统原理B'\n" +
            "2. 教师姓名通常是2-4个汉字，如'杨益'\n" +
            "3. 教室通常包含'楼'、'室'等字，如'南湖南-博学主楼-403'\n" +
            "4. 星期几格式为'周一'到'周日'\n" +
            "5. 时间段格式为'HH:MM-HH:MM'，如'09:55-12:20'\n\n" +
            "**任务：**\n" +
            "请仔细分析文字，提取所有课程信息，严格返回纯JSON数组（不要代码标记）。\n" +
            "每门课必须包含字段：\n" +
            "- name: 课程名称（必填，不能为null或空字符串）\n" +
            "- teacher: 教师（可选）\n" +
            "- classroom: 教室（可选）\n" +
            "- weekday: 星期几（可选，格式：周一/周二/.../周日）\n" +
            "- timeSlot: 时间段（可选，格式：HH:MM-HH:MM）\n\n" +
            "**示例：**\n" +
            "输入文字：'数据库系统原理B 授课教师 杨益 授课地点 南湖南-博学主楼-403 授课时间 09:55-12:20 周五'\n" +
            "输出：[{\"name\":\"数据库系统原理B\",\"teacher\":\"杨益\",\"classroom\":\"南湖南-博学主楼-403\",\"weekday\":\"周五\",\"timeSlot\":\"09:55-12:20\"}]\n\n" +
            "文件文字内容：\n%s", 
            fileText);
        return chat(prompt);
    }
}
