package com.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.ProblemType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProblemTypeMapper extends BaseMapper<ProblemType> {
    @Select("select * from problem_types")
    List<ProblemType> selectAll();
}
