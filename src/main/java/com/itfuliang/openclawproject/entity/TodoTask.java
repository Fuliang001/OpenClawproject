package com.itfuliang.openclawproject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_todo")
public class TodoTask {
    private Long id;
    private Long userId;
    private String taskName;    // 任务名：提交报告
    private LocalDateTime deadline; // 截止：2026-03-27 18:00:00
    private String priority;    // 优先级：HIGH
    private String status;      // 状态：PENDING
    private String description; // 详情
    private Long fileId;        // 来源文件ID
}