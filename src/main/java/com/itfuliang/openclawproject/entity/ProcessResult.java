package com.itfuliang.openclawproject.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("process_result")
public class ProcessResult {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;
    private String tags;
    private String structuredNotes;
    private String originalContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 非数据库字段，用于前端展示关联信息
    @TableField(exist = false)
    private FileInfo fileInfo;
}