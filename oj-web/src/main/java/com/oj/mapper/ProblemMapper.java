package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.entity.Problem;
import com.oj.entity.ProblemType;
import com.oj.entity.ProblemTypesRel;
import com.oj.vo.ProblemAcceptanceVO;
import com.oj.vo.ProblemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
    Page<ProblemVO> selectPage(Page<ProblemVO> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<Problem> ew);

    List<ProblemType> selectPageOf(Long id);

    @Select("select * from problems")
    List<Problem> selectAll();

    Integer countProblem(Map mapyesterDay);

    Integer countProblemDis(Map map);

    List<ProblemAcceptanceVO> selectCount10();

    List<ProblemTypesRel> selectProblemTypesRel(@Param(Constants.WRAPPER) LambdaQueryWrapper<ProblemTypesRel> ew);
}
