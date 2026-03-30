package com.itfuliang.openclawproject.entity;

import lombok.Data;

/**
 * FileVO (View Object)
 * 用于向前端返回文件信息的视图对象
 * 它和 FileInfo 很像，但可以根据需要只返回部分字段，或者增加一些计算后的字段
 */
@Data // Lombok 注解，自动生成 getter/setter/toString 等方法
public class FileVO {

    private Long id;              // 文件 ID
    private String fileName;      // 文件名
    private Long fileSize;        // 文件大小
    private String fileType;      // 文件类型
    private String fileContent;   // 文件内容 (注意：大文件通常不返回这个，但为了演示先留着)

    // private String fileContent; // 通常大文件内容不直接返回，这里先注释掉，保持响应轻量

    // 【新增】这两个字段用来展示 AI 的分析结果
    private String aiTags;      // AI 生成的标签，例如："会议, 进度汇报, 待办"
    private String aiSummary;   // AI 生成的摘要，例如："2026年3月24日召开了项目进度会..."
    private String aiType;
}