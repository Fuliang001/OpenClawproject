package com.itfuliang.openclawproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itfuliang.openclawproject.entity.TodoTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper extends BaseMapper<TodoTask> {
}