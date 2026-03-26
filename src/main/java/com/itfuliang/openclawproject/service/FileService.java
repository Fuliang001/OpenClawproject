package com.itfuliang.openclawproject.service;

import com.itfuliang.openclawproject.entity.FileInfo;
import com.itfuliang.openclawproject.entity.ProcessResult;
import com.itfuliang.openclawproject.mapper.FileMapper;
import com.itfuliang.openclawproject.mapper.ProcessResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ProcessResultMapper processResultMapper;

    private final String UPLOAD_DIR = "uploads/";

    /**
     * 上传文件并保存元数据到数据库
     */
    @Transactional
    public FileInfo uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 1. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        // 2. 保存文件到本地磁盘
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath);

        // 3. 读取文件内容 (仅适用于文本类文件，二进制文件建议只存路径)
        // 注意：大文件直接读入内存可能导致 OOM，生产环境建议流式处理或只存路径
        String content = new String(Files.readAllBytes(filePath), "UTF-8");

        // 4. 构建实体
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(originalFilename);
        fileInfo.setFileContent(content);
        fileInfo.setFileType(file.getContentType());
        fileInfo.setFileSize(file.getSize());
        // 时间字段由 MetaObjectHandler 自动填充

        // 5. 存入数据库
        fileMapper.insert(fileInfo);

        return fileInfo;
    }

    /**
     * 处理文件（模拟 AI 处理结果存储）
     */
    @Transactional
    public ProcessResult saveProcessResult(Long fileId, String tags, String notes, String originalContent) {
        // 校验文件是否存在
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在，ID: " + fileId);
        }

        ProcessResult result = new ProcessResult();
        result.setFileId(fileId);
        result.setTags(tags);
        result.setStructuredNotes(notes);
        result.setOriginalContent(originalContent);
        // 时间自动填充

        processResultMapper.insert(result);

        // 可选：将关联的文件信息塞回去方便返回
        result.setFileInfo(fileInfo);
        return result;
    }

    /**
     * 获取文件详情
     */
    public FileInfo getFileById(Long id) {
        return fileMapper.selectById(id);
    }
}