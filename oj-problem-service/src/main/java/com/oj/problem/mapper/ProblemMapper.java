package com.oj.problem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.problem.entity.Problem;
import com.oj.problem.entity.ProblemType;
import com.oj.problem.entity.ProblemTypesRel;
import com.oj.problem.vo.ProblemAcceptanceVO;
import com.oj.problem.vo.ProblemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
    Page<ProblemVO> selectPage(Page<ProblemVO> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<Problem> ew);

    List<ProblemType> selectPageOf(Long id);

    List<ProblemTypesRel> selectTypeListBatch(@Param("ids") List<Long> ids);

    @Select("select * from problems")
    List<Problem> selectAll();

    Integer countProblem(Map map);

    Integer countProblemDis(Map map);

    List<ProblemAcceptanceVO> selectCount10();

    List<ProblemTypesRel> selectProblemTypesRel(@Param(Constants.WRAPPER) LambdaQueryWrapper<ProblemTypesRel> ew);
}
