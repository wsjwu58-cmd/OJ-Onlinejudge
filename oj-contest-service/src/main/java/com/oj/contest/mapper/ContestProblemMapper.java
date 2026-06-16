package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.ContestProblem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface ContestProblemMapper extends BaseMapper<ContestProblem> {

    default Long selectContentCount(Long problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(problemId != null, ContestProblem::getProblemId, problemId);
        return selectCount(wrapper);
    }

    default List<Long> selectContentCountBatch(List<Integer> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ContestProblem::getProblemId, problemIds)
                .select(ContestProblem::getProblemId);
        List<ContestProblem> list = selectList(wrapper);
        return list.stream()
                .map(cp -> cp.getProblemId().longValue())
                .distinct()
                .collect(Collectors.toList());
    }

    default void deleteProblem(Integer contestId) {
        LambdaUpdateWrapper<ContestProblem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(contestId != null, ContestProblem::getContestId, contestId);
        delete(wrapper);
    }

    @Select("select problem_id from contest_problems where contest_id=#{id}")
    List<Long> selectListAll(Long id);
}
