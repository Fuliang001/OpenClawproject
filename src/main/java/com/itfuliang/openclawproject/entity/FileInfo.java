package com.itfuliang.openclawproject.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private String fileContent;
    private String fileType;
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}