package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.ContestProblem;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ContestProblemMapper extends BaseMapper<ContestProblem> {
   default Long selectContentCount(Long id){
       LambdaQueryWrapper<ContestProblem> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(id!=null,ContestProblem::getProblemId,id);
       return selectCount(lambdaQueryWrapper);
   }

  default   void deleteProblem(Integer id){
      LambdaUpdateWrapper<ContestProblem> contestProblemLambdaUpdateWrapper=new LambdaUpdateWrapper<>();
      contestProblemLambdaUpdateWrapper.eq(id!=null,ContestProblem::getContestId,id);
      delete(contestProblemLambdaUpdateWrapper);
  }


  @Select("select problem_id from contest_problems where contest_id=#{id}")
    List<Long> selectListAll(Long id);
}
