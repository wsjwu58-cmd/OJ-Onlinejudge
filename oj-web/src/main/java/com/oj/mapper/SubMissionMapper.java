package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.entity.Submission;
import com.oj.vo.JudgeResultVO;
import com.oj.vo.SubmissionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;

@Mapper
public interface SubMissionMapper extends BaseMapper<Submission> {

    Integer countSubmission(Map map);

   default List<JudgeResultVO> selectSubmission(Long currentId,Long problemId){
       LambdaQueryWrapper<Submission> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(Submission::getUserId,currentId)
               .eq(problemId!=null,Submission::getProblemId,problemId);
       List<Submission> submissions = selectList(lambdaQueryWrapper);
       List<JudgeResultVO> list = submissions.stream().map(submission -> {
           JudgeResultVO judgeResultVO = new JudgeResultVO();
           BeanUtils.copyProperties(submission, judgeResultVO);
           judgeResultVO.setSubmissionId(submission.getId());

           return judgeResultVO;
       }).toList();

       return list;
   }

   default Integer selectCountProblem(Integer problemId,String status){
       LambdaQueryWrapper<Submission> lambdaQueryWrapper=new LambdaQueryWrapper<>();
       lambdaQueryWrapper.eq(Submission::getProblemId,problemId)
               .eq(status!=null&&!status.isEmpty(),Submission::getStatus,status);
      return Math.toIntExact(selectCount(lambdaQueryWrapper));
   }
}
