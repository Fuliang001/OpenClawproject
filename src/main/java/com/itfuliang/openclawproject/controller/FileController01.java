//package com.itfuliang.openclawproject.controller;
//
//import com.alibaba.fastjson.JSONObject;
//import com.itfuliang.openclawproject.common.Result;
//import com.itfuliang.openclawproject.entity.*;
//import com.itfuliang.openclawproject.mapper.CalendarMapper;
//import com.itfuliang.openclawproject.mapper.FileMapper;
//import com.itfuliang.openclawproject.mapper.ProcessResultMapper;
//import com.itfuliang.openclawproject.mapper.TodoMapper;
//import com.itfuliang.openclawproject.service.AiService;
//import com.itfuliang.openclawproject.service.FileService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.Map;
//
//
//@Slf4j
//@RestController
//@RequestMapping("/api1/files")
//@CrossOrigin(origins = "*") // 允许跨域
//public class FileController01 {
//
//    @Autowired
//    private FileService fileService;
//
//    @Autowired // 记得注入刚才写的 AiService
//    private AiService aiService;
//
//    @Autowired
//    private FileMapper fileInfoMapper; // 注意：这里变量名可以叫 fileInfoMapper，但类型必须是 FileMapper
//
//    @Autowired
//    private ProcessResultMapper processResultMapper;
//
//    // 在类头部注入新的 Mapper
//    @Autowired
//    private CalendarMapper calendarMapper;
//    @Autowired
//    private TodoMapper todoMapper;
//
//    // Python 服务地址
//    private static final String PYTHON_AI_URL = "http://localhost:8000/analyze";
//    // HTTP 客户端工具 (Spring 自带)
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @PostMapping("/upload")
//    public Result<FileVO> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
//
//        //引入logger
//
//        // 1. 保存基础文件信息 (原有逻辑)
//        FileInfo fileInfo = new FileInfo();
//        fileInfo.setFileName(file.getOriginalFilename());
//        fileInfo.setFileSize(file.getSize());
//        fileInfo.setFileType(file.getContentType());
//        fileInfoMapper.insert(fileInfo); // 存入文件表
//
//        // 2. 读取内容
//        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
//
//        // 3. 呼叫 AI 识别意图
//        Map<String, Object> aiResult = aiService.analyzeContent(content);
//        String type = (String) aiResult.get("type");
//        String tags = (String) aiResult.get("tags");
//        String summary = (String) aiResult.get("summary");
//        JSONObject actionData = (JSONObject) aiResult.get("action_data");
//
//        System.out.println("🚀 [执行器] 收到指令类型: " + type);
//
//        // 4. 【核心】根据类型自动执行动作
//        if ("SCHEDULE".equals(type)) {
//            // --- 动作 A: 添加日程 ---
//            CalendarEvent event = new CalendarEvent();
//            event.setTitle(actionData.getString("title"));
//            // 简单处理时间字符串转为 LocalDateTime (实际生产需更严谨的日期格式化)
//            if (actionData.getString("time") != null) {
//                event.setEventTime(LocalDateTime.parse(actionData.getString("time").replace(" ", "T")));
//            }
//            event.setLocation(actionData.getString("location"));
//            event.setDescription(actionData.getString("description"));
//            event.setFileId(fileInfo.getId());
//
//            calendarMapper.insert(event); // <--- 自动写入日程表！
//            System.out.println("📅 [系统] 已自动为您添加日程：" + event.getTitle());
//
//        } else if ("TODO".equals(type)) {
//            // --- 动作 B: 添加待办 ---
//            TodoTask task = new TodoTask();
//            task.setTaskName(actionData.getString("title"));
//            if (actionData.getString("time") != null) {
//                task.setDeadline(LocalDateTime.parse(actionData.getString("time").replace(" ", "T")));
//            }
//            task.setPriority(actionData.getString("priority"));
//            task.setDescription(actionData.getString("description"));
//            task.setFileId(fileInfo.getId());
//            task.setStatus("PENDING");
//
//            todoMapper.insert(task); // <--- 自动写入待办表！
//            System.out.println("✅ [系统] 已自动为您创建待办：" + task.getTaskName());
//        }
//
//        // 5. 保存分析结果到过程表 (可选，用于历史记录)
//        ProcessResult pr = new ProcessResult();
//        pr.setFileId(fileInfo.getId());
//        pr.setTags(tags);
//        pr.setStructuredNotes(summary);
//        processResultMapper.insert(pr);
//
//        // 6. 构建返回给前端的信息
//        FileVO vo = new FileVO();
//        BeanUtils.copyProperties(fileInfo, vo);
//        vo.setAiTags(tags);
//        vo.setAiSummary(summary);
//        vo.setAiType(type); // 你可以在 VO 里加个字段告诉前端这是什么类型
//
//        return Result.success(vo);
//    }
//
//    /**
//     * 保存处理结果接口
//     * POST /api/files/process/{fileId}
//     * Params: tags, structuredNotes, originalContent
//     */
//    @PostMapping("/process/{fileId}")
//    public Result<ProcessResult> processFile(
//            @PathVariable Long fileId,
//            @RequestParam String tags,
//            @RequestParam String structuredNotes,
//            @RequestParam String originalContent) {
//        try {
//            ProcessResult result = fileService.saveProcessResult(fileId, tags, structuredNotes, originalContent);
//            return Result.success(result);
//        } catch (Exception e) {
//            log.info("处理文件失败，fileId:{} ",fileId, e);
////            e.printStackTrace();
//            return Result.error("处理失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 查询文件详情
//     * GET /api/files/{id}
//     */
//    @GetMapping("/{id}")
//    public Result<FileInfo> getById(@PathVariable Long id) {
//        FileInfo info = fileService.getFileById(id);
//        if (info == null) {
//            return Result.error(404, "文件未找到");
//        }
//        return Result.success(info);
//    }
//
//    /**
//     * 健康检查
//     */
//    @GetMapping("/health")
//    public Result<String> health() {
//        return Result.success("System is running normally.");
//    }
//
//}