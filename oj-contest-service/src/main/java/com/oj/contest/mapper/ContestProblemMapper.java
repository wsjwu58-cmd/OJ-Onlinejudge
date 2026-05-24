package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.ContestProblem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ContestProblemMapper extends BaseMapper<ContestProblem> {

    default Long selectContentCount(Long problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(problemId != null, ContestProblem::getProblemId, problemId);
        return selectCount(wrapper);
    }

    default void deleteProblem(Integer contestId) {
        LambdaUpdateWrapper<ContestProblem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(contestId != null, ContestProblem::getContestId, contestId);
        delete(wrapper);
    }

    @Select("select problem_id from contest_problems where contest_id=#{id}")
    List<Long> selectListAll(Long id);
}
