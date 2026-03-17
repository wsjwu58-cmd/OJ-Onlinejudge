package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.GroupProblems;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupProblemMapper extends BaseMapper<GroupProblems> {
   default Long selectProblemCount(Long id){
       LambdaQueryWrapper<GroupProblems> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(id!=null,GroupProblems::getProblemId,id);
       return selectCount(lambdaQueryWrapper);
   }

  default   List<GroupProblems> selectGroup(Long id){
       LambdaQueryWrapper<GroupProblems> groupProblemsLambdaQueryWrapper=new LambdaQueryWrapper<>();
       groupProblemsLambdaQueryWrapper.eq(id!=null,GroupProblems::getProblemId,id);
       return  selectList(groupProblemsLambdaQueryWrapper);
  }


  default List<GroupProblems> selectByGroupId(Long groupId){
       LambdaQueryWrapper<GroupProblems> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(groupId!=null,GroupProblems::getGroupId,groupId);
       return selectList(lambdaQueryWrapper);
  }

//  void insert(List<GroupProblems> groupProblemsList);

   default void deleteByGroupId(Integer id){
       LambdaUpdateWrapper<GroupProblems> groupProblemsLambdaUpdateWrapper=new LambdaUpdateWrapper<>();
       groupProblemsLambdaUpdateWrapper.eq(id!=null,GroupProblems::getGroupId,id);
       delete(groupProblemsLambdaUpdateWrapper);
   }

   @Select("select problem_id from group_problems where group_id=#{id} ")
    List<Long> selectListAll(Long id);

  default void deleteProblem(Integer intExact){
      LambdaUpdateWrapper<GroupProblems> lambdaUpdateWrapper=new LambdaUpdateWrapper<>();
      lambdaUpdateWrapper.eq(intExact!=null,GroupProblems::getGroupId,intExact);
      delete(lambdaUpdateWrapper);
  }
}
