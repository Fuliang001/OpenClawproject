package com.itfuliang.openclawproject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_calendar")
public class CalendarEvent {
    private Long id;
    private Long userId;
    private String title;       // 标题：项目评审会
    private LocalDateTime eventTime; // 时间：2026-03-25 15:00:00
    private String location;    // 地点：会议室A
    private String description; // 详情
    private Long fileId;        // 来源文件ID
    //time
    private String time;
}