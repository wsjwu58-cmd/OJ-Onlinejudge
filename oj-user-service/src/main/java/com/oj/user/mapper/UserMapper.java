package com.oj.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    Integer countUserToday(Map map);

    Integer countUserBeforeToday(LocalDateTime localDate);

    Integer countCreateUser(Map map);
}
