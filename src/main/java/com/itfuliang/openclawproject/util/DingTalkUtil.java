package com.itfuliang.openclawproject.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class DingTalkUtil {

    // ⚠️ 请在这里填入你刚才复制的钉钉机器人 Webhook 地址
    private static final String WEBHOOK_URL = "https://oapi.dingtalk.com/robot/send?access_token=95dbe5ee3c7ae60cc3a7736644e9e1e184ae4c5b63e3cb7a4f9e30fa5a3d2f77";

    private static void sendRequest(String json) throws Exception {
        java.net.URL url = new java.net.URL(WEBHOOK_URL);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP 错误码: " + responseCode);
        }
    }

    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送文本消息到钉钉群
     *
     * @param content 消息内容 (必须包含你在钉钉后台设置的关键词，如 "提醒")
     */
    public static void sendTextMessage(String content) {
        System.out.println("📢 准备发送钉钉消息: " + content);
//        if (WEBHOOK_URL.contains("access_token=95dbe5ee3c7ae60cc3a7736644e9e1e184ae4c5b63e3cb7a4f9e30fa5a3d2f77")) {
//            System.err.println("❌ 错误：请先在 DingTalkUtil.java 中配置真实的 Webhook URL！");
//            return;
//        }

        try {
            // 构建钉钉要求的 JSON 结构
            Map<String, Object> params = new HashMap<>();
            params.put("msgtype", "text");

            Map<String, String> textContent = new HashMap<>();
            // 确保内容里包含关键词 "提醒"，否则钉钉会拦截
            String finalContent = content;
            if (!content.contains("提醒")) {
                finalContent = "[提醒] " + content;
            }
            textContent.put("content", finalContent);
            params.put("text", textContent);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

            // 发送请求
            String response = restTemplate.postForObject(WEBHOOK_URL, entity, String.class);

            if (response != null && response.contains("\"errcode\":0")) {
                System.out.println("📢 钉钉消息发送成功!");
            } else {
                System.err.println("⚠️ 钉钉返回异常: " + response);
            }

        } catch (Exception e) {
            System.err.println("❌ 发送钉钉消息失败: " + e.getMessage());
            e.printStackTrace();
        }


    }
    // --- 新增开始 ---

    /**
     * 发送 Markdown 格式的卡片消息到钉钉群
     * @param title 卡片标题
     * @param text 卡片内容 (支持 Markdown 语法)
     */
    public static void sendMarkdownCard(String title, String text) {
        try {
            // 构造 Markdown 类型的消息体
            String markdownJson = """
            {
                "msgtype": "markdown",
                "markdown": {
                    "title": "%s",
                    "text": "%s"
                }
            }
            """.formatted(title, text.replace("\"", "\\\"")); // 转义双引号防止 JSON 错误

            // 调用通用的发送方法
            sendRequest(markdownJson);

            System.out.println("✅ [钉钉] Markdown 卡片发送成功");
        } catch (Exception e) {
            System.err.println("❌ [钉钉] Markdown 卡片发送失败: " + e.getMessage());
        }
    }
    // --- 新增结束 ---
}