package com.itfuliang.openclawproject;

import java.io.IOException;
import java.net.URLEncoder;

public class MessageSender {

    /**
     * 功能：调用电脑自带的软件 (邮件、短信、微信)
     */
    public static void sendViaSystemApp(String type, String target, String content) {
        String url = "";
        String os = System.getProperty("os.name").toLowerCase();
        // Windows 用 cmd start, Mac 用 open
        String cmdPrefix = os.contains("win") ? "cmd.exe /c start " : "open ";

        try {
            if ("email".equalsIgnoreCase(type)) {
                // 构造发邮件的链接
                url = "mailto:" + target + "?subject=智能助手消息&body=" + URLEncoder.encode(content, "UTF-8");
            } else if ("sms".equalsIgnoreCase(type)) {
                // 构造发短信的链接
                url = "sms:" + target + "?body=" + URLEncoder.encode(content, "UTF-8");
            } else if ("wechat".equalsIgnoreCase(type)) {
                // 微信只能打开，不能直接指定人
                System.out.println("⚠️ 微信无法直接指定联系人，将打开微信...");
                url = "weixin://";
            }

            if (!url.isEmpty()) {
                System.out.println("🚀 [系统] 正在唤起应用: " + url);
                // 执行命令
                Runtime.getRuntime().exec(cmdPrefix + "\"" + url + "\"");
                System.out.println("✅ 已唤起应用，请在弹出的窗口中确认发送。");
            }
        } catch (IOException e) {
            System.err.println("❌ 唤起应用失败: " + e.getMessage());
        }
    }
}