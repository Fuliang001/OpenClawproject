package com.itfuliang.openclawproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itfuliang.openclawproject.dto.AiAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // 构造函数注入
    public AiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 分析文件内容并提取结构化数据
     * 【修改点】参数改为 String filePath，直接读取本地文件
     */
    public AiAnalysisResult analyzeFileToObject(String filePath) throws IOException {
        // 1. 【修改】根据路径读取文件内容
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);

        // 2. 构造提示词 (Prompt)
        String prompt = """
                请分析以下文本内容，提取其中的日程安排 (SCHEDULE) 和待办事项 (TODO)。
                
                要求：
                1. 必须严格返回标准的 JSON 格式，不要包含任何 Markdown 标记（如 ```json 或 ```）。
                2. JSON 结构必须符合以下定义：
                   {
                     "code": 200,
                     "msg": "success",
                     "data": [
                       {
                         "type": "SCHEDULE" 或 "TODO",
                         "title": "标题",
                         "time": "YYYY-MM-DD HH:mm:ss",
                         "location": "地点 (如果是日程)",
                         "priority": "优先级 (如果是待办，可选)"
                       }
                     ]
                   }
                
                文本内容如下：
                %s
                """.formatted(content);

        // 3. 调用 AI 模型
        String responseContent = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println("🤖 AI 原始返回内容:\n" + responseContent);

        // 4. 清洗 JSON 字符串，去除 Markdown 标记
        String cleanJson = responseContent.trim();

        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }

        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }

        cleanJson = cleanJson.trim();

        // 5. 解析 JSON 为对象
        try {
            AiAnalysisResult result = objectMapper.readValue(cleanJson, AiAnalysisResult.class);
            System.out.println("✅ AI 结果解析成功，共提取 " + (result.getData() != null ? result.getData().size() : 0) + " 条记录");
            return result;
        } catch (Exception e) {
            System.err.println("❌ JSON 解析失败，清洗后的内容为:\n" + cleanJson);
            throw new RuntimeException("AI 结果解析失败：" + e.getMessage(), e);
        }
    }
}










//package com.itfuliang.openclawproject.service;
//
//import com.alibaba.fastjson.JSON; // 注意：fastjson2 包名可能是这个，如果是 fastjson 1.x 则是 com.alibaba.fastjson
//import com.alibaba.fastjson.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class AiService {
//
//    @Value("${ollama.base-url:http://localhost:11434}")
//    private String ollamaBaseUrl;
//
//    @Value("${ollama.model:qwen2.5:7b}")
//    private String modelName;
//
//    // 初始化 WebClient
//    private final WebClient webClient = WebClient.builder().build();
//
//    /**
//     * 分析文件内容
//     * @param content 文件文本内容
//     * @return 包含 tags 和 summary 的 Map
//     */
//    public Map<String, Object> analyzeContent(String content) {
//        System.out.println("🧠 [AI 大脑] 正在分析意图...");
//
//        // 1. 构造超级提示词
//        String prompt = "你是一个智能行动管家。请分析用户的输入内容，判断其意图。" +
//                "你必须严格返回一个标准的 JSON 对象，不要包含任何多余文字。" +
//                "JSON 结构如下：\n" +
//                "{\n" +
//                "  \"type\": \"SCHEDULE\" 或 \"TODO\" 或 \"NOTE\", // SCHEDULE=日程, TODO=待办, NOTE=普通笔记\n" +
//                "  \"tags\": [\"标签1\", \"标签2\"],\n" +
//                "  \"summary\": \"一句话摘要\",\n" +
//                "  \"action_data\": { // 如果是日程或待办，这里必须有数据；如果是笔记，可以为空对象\n" +
//                "     \"title\": \"标题\",\n" +
//                "     \"time\": \"YYYY-MM-DD HH:mm:ss 格式的时间，如果没有具体时间则为 null\",\n" +
//                "     \"location\": \"地点，没有则为 null\",\n" +
//                "     \"priority\": \"HIGH/NORMAL/LOW\", // 仅待办需要\n" +
//                "     \"description\": \"详细描述\"\n" +
//                "  }\n" +
//                "}\n" +
//                "用户输入内容：\n" + content;
//
//        try {
//            // 2. 呼叫 Ollama
//            String responseBody = webClient.post()
//                    .uri(ollamaBaseUrl + "/api/generate")
//                    .bodyValue(Map.of("model", modelName, "prompt", prompt, "stream", false))
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .timeout(Duration.ofSeconds(60))
//                    .block();
//
//            // 3. 解析外层 JSON
//            JSONObject outerJson = JSON.parseObject(responseBody);
//            String aiText = outerJson.getString("response");
//
//            // 清理 markdown 标记
//            if (aiText.contains("```")) {
//                aiText = aiText.replaceAll("```json", "").replaceAll("```", "").trim();
//            }
//
//            // 4. 解析业务 JSON
//            JSONObject resultJson = JSON.parseObject(aiText);
//
//            // 将解析结果放入 Map 返回，方便后续处理
//            Map<String, Object> resultMap = new HashMap<>();
//            resultMap.put("type", resultJson.getString("type"));
//            resultMap.put("tags", resultJson.getJSONArray("tags").toString());
//            resultMap.put("summary", resultJson.getString("summary"));
//            resultMap.put("action_data", resultJson.getJSONObject("action_data"));
//
//            System.out.println("✅ [AI 大脑] 识别结果 -> 类型: " + resultMap.get("type"));
//            return resultMap;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            // 出错时返回默认值，类型为 NOTE，防止程序崩溃
//            Map<String, Object> errorMap = new HashMap<>();
//            errorMap.put("type", "NOTE");
//            errorMap.put("tags", "[\"错误\"]");
//            errorMap.put("summary", "AI 分析失败");
//            errorMap.put("action_data", new JSONObject());
//            return errorMap;
//        }
//    }
//}
//
//
//
////package com.itfuliang.openclawproject.service;
////
////import org.springframework.stereotype.Service;
////import org.springframework.web.reactive.function.client.WebClient;
////import reactor.core.publisher.Mono;
////
////import java.util.HashMap;
////import java.util.Map;
////
////@Service
////public class AiService {
////
////    // 创建一个能发请求的工具
////    private final WebClient webClient = WebClient.create("http://localhost:11434");
////
////    /**
////     * 让 AI 分析文本，返回标签和摘要
////     * @param content 文件的原始内容
////     * @return 一个包含 tags 和 notes 的地图
////     */
////    public Map<String, String> analyzeContent(String content) {
////        // 1. 构造给 AI 的指令 (Prompt)
////        String prompt = "请分析以下文本内容：\n" + content +
////                "\n\n请执行以下任务：\n" +
////                "1. 提取 3-5 个关键词标签，用逗号分隔。\n" +
////                "2. 写一段 100 字以内的结构化摘要。\n" +
////                "3. 请严格只返回 JSON 格式，不要其他废话。格式如下：{\"tags\": \"标签1,标签2\", \"notes\": \"摘要内容\"}";
////
////        // 2. 构造请求体
////        Map<String, Object> requestBody = new HashMap<>();
////        requestBody.put("model", "qwen2.5:7b"); // 这里要和你下载的模型名字一致
////        requestBody.put("prompt", prompt);
////        requestBody.put("stream", false); // 不需要流式输出，一次性拿结果
////
////        // 3. 发送请求并获取结果 (这里是同步写法，方便理解)
////        try {
////            String response = webClient.post()
////                    .uri("/api/generate")
////                    .bodyValue(requestBody)
////                    .retrieve()
////                    .bodyToMono(String.class)
////                    .block(); // block() 表示等待结果回来，实际生产环境通常不用这个，但教学演示最简单
////
////            // 4. 简单解析返回的 JSON (实际项目中会用 Jackson 库，这里为了简单手动处理一下)
////            // 注意：真实的 JSON 解析需要引入 fastjson 或 jackson，为了不让步骤太复杂，
////            // 我们这里先打印出来看看，下一步再教你怎么完美解析。
////            System.out.println("AI 返回的原始数据: " + response);
////
////            // 临时模拟解析结果 (因为手动解析 JSON 很麻烦，我们先硬编码返回示例，稍后优化)
////            // ⬇️⬇️⬇️ 暂时先用假数据演示流程，确保你不怕代码 ⬇️⬇️⬇️
////            Map<String, String> result = new HashMap<>();
////            result.put("tags", "AI自动分析,智能标签,待优化解析");
////            result.put("notes", "AI已经成功读取了文件内容，正在学习如何完美返回JSON格式给你。");
////
////            // 如果你想看真正的 AI 反应，可以把上面两行注释掉，去研究怎么解析 response 字符串
////            // 但为了让你立刻看到效果，我们先让代码跑通流程！
////
////            return result;
////
////        } catch (Exception e) {
////            e.printStackTrace();
////            // 如果 AI 挂了，返回一个默认值，不让程序崩溃
////            Map<String, String> errorResult = new HashMap<>();
////            errorResult.put("tags", "连接失败");
////            errorResult.put("notes", "AI 服务未启动或模型名称错误");
////            return errorResult;
////        }
////    }
////}