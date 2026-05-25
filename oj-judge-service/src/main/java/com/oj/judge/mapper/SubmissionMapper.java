package com.oj.judge.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.judge.entity.Submission;
import com.oj.judge.vo.JudgeResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
    Integer countSubmission(Map map);

    default List<JudgeResultVO> selectSubmission(Long currentId, Long problemId) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, currentId)
                .eq(problemId != null, Submission::getProblemId, problemId)
                .orderByDesc(Submission::getSubmitTime);
        List<Submission> submissions = selectList(wrapper);
        return submissions.stream().map(submission -> {
            JudgeResultVO vo = new JudgeResultVO();
            org.springframework.beans.BeanUtils.copyProperties(submission, vo);
            vo.setSubmissionId(submission.getId());
            return vo;
        }).toList();
    }

    default Integer selectCountProblem(Integer problemId, String status) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getProblemId, problemId)
                .eq(status != null && !status.isEmpty(), Submission::getStatus, status);
        return Math.toIntExact(selectCount(wrapper));
    }
}
