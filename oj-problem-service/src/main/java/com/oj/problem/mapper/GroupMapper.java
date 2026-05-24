package com.oj.problem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.problem.entity.Problem;
import com.oj.problem.entity.ProblemGroup;
import com.oj.problem.vo.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMapper extends BaseMapper<ProblemGroup> {
    Page<GroupVO> selectPage(Page<GroupVO> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<ProblemGroup> ew);

    List<Problem> SelectByID(Long id);
}
