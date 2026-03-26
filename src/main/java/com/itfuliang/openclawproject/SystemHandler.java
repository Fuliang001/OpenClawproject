package com.itfuliang.openclawproject; // 注意：这里包名要和你的一致

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SystemHandler {

    /**
     * 功能：生成一个日历文件，并尝试自动打开它
     */
    public static void addScheduleToSystem(String title, String timeStr, String location) {
        try {
            // 1. 准备日历文件的内容 (.ics 格式是通用的)
            String uid = UUID.randomUUID().toString();
            // 解析时间，假设传入的是 "yyyy-MM-dd HH:mm:ss" 格式
            LocalDateTime dt = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String dtStart = dt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            String dtEnd = dt.plusHours(1).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

            String icsContent = "BEGIN:VCALENDAR\n" +
                    "VERSION:2.0\n" +
                    "PRODID:-//MySmartAssistant//CN\n" +
                    "BEGIN:VEVENT\n" +
                    "UID:" + uid + "\n" +
                    "DTSTAMP:" + dtStart + "\n" +
                    "DTSTART:" + dtStart + "\n" +
                    "DTEND:" + dtEnd + "\n" +
                    "SUMMARY:" + title + "\n" +
                    (location != null ? "LOCATION:" + location + "\n" : "") +
                    "DESCRIPTION:由 AI 智能助手自动添加\n" +
                    "END:VEVENT\n" +
                    "END:CALENDAR";

            // 2. 把这个内容保存成一个临时文件
            Path tempFile = Files.createTempFile("calendar_event_", ".ics");
            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(icsContent);
            }

            System.out.println("📅 [系统] 已生成日历文件，准备打开...");

            // 3. 命令电脑打开这个文件 (Windows 用 start, Mac 用 open)
            openFile(tempFile.toString());

            System.out.println("✅ 系统日历添加指令已发送！");

        } catch (Exception e) {
            System.err.println("❌ 添加系统日历失败：" + e.getMessage());
        }
    }

    /**
     * 功能：简单的待办提醒 (打印在控制台)
     */
    public static void addTodoToSystem(String task, String priority) {
        System.out.println("✅ [系统待办] 任务已记录: " + task + " (优先级: " + priority + ")");
        // 进阶：这里也可以写成生成一个桌面快捷方式
    }

    // 下面是辅助方法，不用管它具体怎么写，它能帮我们要打开文件
    private static void openFile(String filePath) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", filePath);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", filePath);
        } else {
            pb = new ProcessBuilder("xdg-open", filePath);
        }
        pb.start();
    }
}