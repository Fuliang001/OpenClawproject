package com.itfuliang.openclawproject.controller;

import com.itfuliang.openclawproject.common.Result;
import com.itfuliang.openclawproject.dto.AiAnalysisResult;
import com.itfuliang.openclawproject.entity.CalendarEvent;
import com.itfuliang.openclawproject.entity.FileInfo;
import com.itfuliang.openclawproject.entity.ProcessResult;
import com.itfuliang.openclawproject.entity.TodoTask;
import com.itfuliang.openclawproject.mapper.CalendarMapper;
import com.itfuliang.openclawproject.mapper.FileMapper;
import com.itfuliang.openclawproject.mapper.TodoMapper;
import com.itfuliang.openclawproject.service.AiService;
import com.itfuliang.openclawproject.service.FileService;
import com.itfuliang.openclawproject.util.DingTalkUtil;
// 导入我们新建的两个核心助手类
import com.itfuliang.openclawproject.SystemHandler;
import com.itfuliang.openclawproject.MessageSender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private AiService aiService;

    @Autowired
    private CalendarMapper calendarMapper;

    @Autowired
    private TodoMapper todoMapper;

    // 模拟当前用户 ID
    private static final Long DEFAULT_USER_ID = 1L;

    /**
     * 接口 1: 普通上传 (默认行为：既发钉钉，又加系统日历)
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return handleFileUpload(file, ""); // 调用统一处理逻辑，指令为空
    }

    /**
     * 接口 2: 带指令上传 (推荐用于测试不同场景)
     * 使用方法：在 Postman form-data 中添加 key: instruction
     * 例子：
     * - "只发钉钉"
     * - "只加系统日历"
     * - "发给 zhangsan@test.com 说会议开始了"
     * - "发给 13800000000 提醒开会"
     */
    @PostMapping("/uploadWithInstruction")
    public Result<String> uploadFileWithInstruction(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "instruction", required = false, defaultValue = "") String instruction) {
        return handleFileUpload(file, instruction);
    }

    /**
     * 统一处理逻辑
     */
    private Result<String> handleFileUpload(MultipartFile file, String instruction) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 1. 保存文件
            String uploadDir = "uploads/";
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Files.write(path.resolve(fileName), file.getBytes());
            String filePath = path.resolve(fileName).toString();

            // 2. 调用 AI 解析 (直接获取对象)
            AiAnalysisResult resultData = aiService.analyzeFileToObject(filePath);

            if (resultData == null || resultData.getData() == null || resultData.getData().isEmpty()) {
                return Result.error("AI 未提取到有效信息，请检查文件内容。");
            }

            List<AiAnalysisResult.Item> items = resultData.getData();

            // 3. 执行智能分发
            processItems(items, instruction);

            String msg = "处理成功！";
            if (instruction != null && !instruction.isEmpty()) {
                msg += " 执行指令：" + instruction;
            } else {
                msg += " (默认模式：钉钉 + 系统日历)";
            }
            return Result.success(msg);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    /**
     * 【核心大脑】智能分发器
     */
    private void processItems(List<AiAnalysisResult.Item> items, String instruction) {
        // --- 1. 意图识别逻辑 ---

        // 如果指令为空，默认两个平台都开
        boolean isEmptyInstruction = (instruction == null || instruction.trim().isEmpty());

        boolean wantDingTalk = isEmptyInstruction || instruction.contains("钉钉");
        // 如果说了"只加系统"，那 wantDingTalk 应该为 false (简单逻辑优化)
        if (instruction.contains("只加系统") || instruction.contains("只要系统")) {
            wantDingTalk = false;
        }

        boolean wantSystem = isEmptyInstruction || instruction.contains("系统") || instruction.contains("日历");
        if (instruction.contains("只发钉钉") || instruction.contains("只要钉钉")) {
            wantSystem = false;
        }

// 修改后：如果指令为空（默认全开），或者指令中包含关键词，则执行
        boolean wantSendMessage = isEmptyInstruction || instruction.contains("发给") || instruction.contains("通知");

        System.out.println("\n🤖 === 智能分发开始 ===");
        System.out.println("🗣️ 用户指令: " + (isEmptyInstruction ? "(无，默认全开)" : instruction));
        System.out.println("✅ 执行钉钉: " + wantDingTalk);
        System.out.println("✅ 执行系统: " + wantSystem);
        System.out.println("✅ 执行发消息: " + wantSendMessage);

        // --- 2. 遍历处理每一项 ---
        for (AiAnalysisResult.Item item : items) {
            String type = item.getType();

            if ("SCHEDULE".equals(type)) {
                // A. 保存数据库 (永远执行)
                CalendarEvent event = new CalendarEvent();
                event.setTitle(item.getTitle());
                event.setTime(item.getTime());

                // 解析时间并设置到 eventTime 字段
                if (item.getTime() != null && !item.getTime().isEmpty()) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(
                                item.getTime(),
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        );
                        event.setEventTime(localDateTime);
                    } catch (Exception e) {
                        System.err.println("⚠️ 时间格式解析失败：" + item.getTime());
                        event.setEventTime(java.time.LocalDateTime.now());
                    }
                } else {
                    event.setEventTime(java.time.LocalDateTime.now());
                }
                event.setLocation(item.getLocation());
                event.setUserId(DEFAULT_USER_ID);
                calendarMapper.insert(event);

                // B. 钉钉通知
                if (wantDingTalk) {
                    String content = "**📅 事项：** " + item.getTitle() + "\n" +
                            "**⏰ 时间：** " + item.getTime() +
                            (item.getLocation() != null ? "\n**📍 地点：** " + item.getLocation() : "");
                    DingTalkUtil.sendMarkdownCard("钉钉日程提醒", content);
                    System.out.println("   -> [钉钉] 已发送日程卡片");
                }

                // C. 系统日历
                if (wantSystem) {
                    SystemHandler.addScheduleToSystem(item.getTitle(), item.getTime(), item.getLocation());
                    System.out.println("   -> [系统] 已生成日历文件并尝试打开");
                }
            }

            else if ("TODO".equals(type)) {
                // A. 保存数据库
                TodoTask task = new TodoTask();
                task.setTaskName(item.getTitle());

                // 解析时间并设置到 deadline 字段
                if (item.getTime() != null && !item.getTime().isEmpty()) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(
                                item.getTime(),
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        );
                        task.setDeadline(localDateTime);
                    } catch (Exception e) {
                        System.err.println("⚠️ 时间格式解析失败：" + item.getTime());
                    }
                }

                task.setPriority(item.getPriority() != null && !item.getPriority().isEmpty() ? item.getPriority() : "MEDIUM");
                task.setStatus("PENDING");
                task.setUserId(DEFAULT_USER_ID);
                todoMapper.insert(task);

                // B. 钉钉通知
                if (wantDingTalk) {
                    String content = "**✅ 任务：** " + item.getTitle() + "\n" +
                            "**🚩 优先级：** " + (item.getPriority() != null ? item.getPriority() : "普通");
                    DingTalkUtil.sendMarkdownCard("钉钉待办提醒", content);
                    System.out.println("   -> [钉钉] 已发送待办卡片");
                }

                // C. 系统待办
                if (wantSystem) {
                    SystemHandler.addTodoToSystem(item.getTitle(), item.getPriority());
                    System.out.println("   -> [系统] 已记录待办任务");
                }
            }
        }

        // --- 3. 特殊处理：发送消息给某人 ---
        if (wantSendMessage) {
            // 正则：发给 [目标] [内容]
            Pattern p = Pattern.compile("发给\\s*(\\S+)\\s*(.*)");
            Matcher m = p.matcher(instruction);

            // --- 👇 修改这里 👇 ---
            if (m.find()) {
                // 智能获取非空的那个组
                String target = m.group(1) != null ? m.group(1) : m.group(2);
                String contentPart = m.group(2) != null ? m.group(2) : ""; // 如果有具体消息内容也要处理

                // 拼接默认消息内容（如果你的指令里没有带具体消息内容）
                String finalMsg = "您有新的日程安排：" + items.get(0).getTitle();

                System.out.println("-> 【消息】解析成功，目标：" + target);

                // 判断类型并发送
                if (target != null && target.contains("@")) {
                    MessageSender.sendViaSystemApp("email", target, finalMsg);
                } else if (target != null) {
                    MessageSender.sendViaSystemApp("sms", target, finalMsg);
                }
            }
            }

        System.out.println("🤖 === 智能分发结束 ===\n");
    }

    /**
     * 保存处理结果接口
     * POST /api/files/process/{fileId}
     * Params: tags, structuredNotes, originalContent
     */
    @PostMapping("/process/{fileId}")
    public Result<ProcessResult> processFile(
            @PathVariable Long fileId,
            @RequestParam String tags,
            @RequestParam String structuredNotes,
            @RequestParam String originalContent) {
        try {
            ProcessResult result = fileService.saveProcessResult(fileId, tags, structuredNotes, originalContent);
            return Result.success(result);
        } catch (Exception e) {
            log.info("处理文件失败，fileId:{} ",fileId, e);
//            e.printStackTrace();
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 查询文件详情
     * GET /api/files/{id}
     */
    @GetMapping("/{id}")
    public Result<FileInfo> getById(@PathVariable Long id) {
        FileInfo info = fileService.getFileById(id);
        if (info == null) {
            return Result.error(404, "文件未找到");
        }
        return Result.success(info);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("System is running normally.");
    }
}