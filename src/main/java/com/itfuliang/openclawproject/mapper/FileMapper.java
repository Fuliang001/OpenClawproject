package com.itfuliang.openclawproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itfuliang.openclawproject.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {
    // 继承 BaseMapper 后，无需编写任何方法即可使用 selectById, insert, updateById 等
}