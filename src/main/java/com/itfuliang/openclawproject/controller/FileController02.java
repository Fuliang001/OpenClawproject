package com.itfuliang.openclawproject.controller;

import com.itfuliang.openclawproject.dto.AiAnalysisResult;
import com.itfuliang.openclawproject.entity.CalendarEvent;
import com.itfuliang.openclawproject.entity.FileInfo;
import com.itfuliang.openclawproject.entity.ProcessResult;
import com.itfuliang.openclawproject.entity.TodoTask;
import com.itfuliang.openclawproject.mapper.CalendarMapper;
import com.itfuliang.openclawproject.mapper.TodoMapper;
import com.itfuliang.openclawproject.common.Result;
import com.itfuliang.openclawproject.service.FileService;
import com.itfuliang.openclawproject.util.DingTalkUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("api2/files")
public class FileController02 {

    @Autowired
    private FileService fileService;

    @Autowired
    private CalendarMapper calendarMapper;

    @Autowired
    private TodoMapper todoMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PYTHON_AI_URL = "http://localhost:8000/analyze";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================== 接口 1: 文件上传 (支持多格式) ==================
    @PostMapping("/upload")
    public Result<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return Result.error("无法获取文件名");
        }

        String textContent;
        try {
            textContent = extractTextFromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件解析失败: " + e.getMessage());
        }

        if (textContent == null || textContent.trim().isEmpty()) {
            return Result.error("文件中未提取到有效文本内容");
        }

        System.out.println("📄 提取到的文本内容:\n" + textContent);

        // 调用核心处理逻辑
        return processAiResult(textContent);
    }

    // ================== 接口 2: 直接文本输入 ==================
    @PostMapping("/analyze/text")
    public Result<?> analyzeText(@RequestParam("text") String text) {
        if (text == null || text.trim().isEmpty()) {
            return Result.error("输入的文本不能为空");
        }

        System.out.println("⌨️ 接收到的直接输入文本:\n" + text);

        // 调用核心处理逻辑
        return processAiResult(text);
    }

    // ================== 核心逻辑：调用 AI 并保存数据 ==================
    private Result<?> processAiResult(String textContent) {
        // 1. 准备请求数据
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("text", textContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestParams, headers);

        try {
            System.out.println("🤖 正在请求 Python AI 服务...");

            AiAnalysisResult response = restTemplate.postForObject(
                    PYTHON_AI_URL,
                    entity,
                    AiAnalysisResult.class
            );

            if (response != null && response.isSuccess() && response.getData() != null) {
                System.out.println("✅ AI 解析成功，共提取 " + response.getData().size() + " 条数据");

                int scheduleCount = 0;
                int todoCount = 0;

                for (AiAnalysisResult.Item item : response.getData()) {
                    String type = item.getType();

                    if ("SCHEDULE".equals(type)) {
                        CalendarEvent event = new CalendarEvent();
                        event.setTitle(item.getTitle());
                        event.setLocation(item.getLocation());

                        if (item.getTime() != null && !item.getTime().isEmpty()) {
                            try {
                                LocalDateTime eventTime = LocalDateTime.parse(item.getTime(), formatter);
                                event.setEventTime(eventTime);
                            } catch (Exception e) {
                                System.err.println("⚠️ 时间格式解析失败: " + item.getTime() + ", 将设为当前时间");
                                event.setEventTime(LocalDateTime.now());
                            }
                        } else {
                            event.setEventTime(LocalDateTime.now());
                        }

                        calendarMapper.insert(event);
                        scheduleCount++;

                        // --- 修改这里：启用钉钉发送 ---
                        String scheduleMsg = "📅 新日程提醒:\n" +
                                "标题: " + item.getTitle() + "\n" +
                                "时间: " + item.getTime() +
                                (item.getLocation() != null ? "\n地点: " + item.getLocation() : "");
                        DingTalkUtil.sendTextMessage(scheduleMsg); // 取消注释

                        System.out.println("📅 [已保存并通知] 日程: " + item.getTitle());

                    } else if ("TODO".equals(type)) {
                        TodoTask task = new TodoTask();
                        task.setTaskName(item.getTitle());
                        task.setPriority(item.getPriority() != null ? item.getPriority() : "MEDIUM");
                        task.setStatus("PENDING");

                        todoMapper.insert(task);
                        todoCount++;

                        // --- 修改这里：启用钉钉发送 ---
                        String todoMsg = "✅ 新待办提醒:\n" +
                                "任务: " + item.getTitle() + "\n" +
                                "优先级: " + item.getPriority();
                        DingTalkUtil.sendTextMessage(todoMsg); // 取消注释

                        System.out.println("✅ [已保存并通知] 待办: " + item.getTitle());
                    }
                }

                return Result.success("处理完成！新增日程: " + scheduleCount + ", 新增待办: " + todoCount);

            } else {
                return Result.error("AI 未能从内容中提取到有效信息，请尝试更清晰的描述。");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 服务暂时不可用: " + e.getMessage());
        }
    }

    // ================== 工具方法：根据后缀名解析文件 ==================
    private String extractTextFromFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename().toLowerCase();

        try (InputStream inputStream = file.getInputStream()) {
            if (fileName.endsWith(".xlsx")) {
                return extractFromExcel(inputStream);
            } else if (fileName.endsWith(".docx")) {
                return extractFromWord(inputStream);
            } else if (fileName.endsWith(".pdf")) {
                return extractFromPdf(inputStream);
            } else if (fileName.endsWith(".txt")) {
                return extractFromTxt(inputStream);
            } else {
                throw new IOException("不支持的文件格式: " + fileName + " (支持 .xlsx, .docx, .pdf, .txt)");
            }
        }
    }

    private String extractFromExcel(InputStream is) throws IOException {
        StringBuilder text = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        text.append(cell.getStringCellValue()).append(" ");
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        text.append(cell.getNumericCellValue()).append(" ");
                    }
                }
                text.append("\n");
            }
        }
        return text.toString();
    }

    private String extractFromWord(InputStream is) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    private String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromTxt(InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        return new String(bytes, "UTF-8");
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