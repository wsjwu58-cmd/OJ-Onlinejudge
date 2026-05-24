package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.ContestParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContestParticipantMapper extends BaseMapper<ContestParticipant> {

    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId} AND user_id = #{userId}")
    int countByContestAndUser(Integer contestId, Long userId);

    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId}")
    int countByContest(Integer contestId);
}
