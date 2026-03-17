package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {

   default List<TestCase> selectByProblemId(Integer problemId){
       LambdaQueryWrapper<TestCase> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(problemId!=null,TestCase::getProblemId,problemId);
       return selectList(lambdaQueryWrapper);
   }

    default void deleteProblem(List<Long> ids){
        LambdaUpdateWrapper<TestCase> lambdaUpdateWrapper=new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.in(TestCase::getProblemId,ids);
        delete(lambdaUpdateWrapper);
    }
}
