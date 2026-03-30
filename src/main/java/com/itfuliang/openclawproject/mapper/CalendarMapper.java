package com.itfuliang.openclawproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itfuliang.openclawproject.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CalendarMapper extends BaseMapper<CalendarEvent> {
}