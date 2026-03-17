package com.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.ContestParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContestParticipantMapper extends BaseMapper<ContestParticipant> {

    /**
     * 查询用户是否已报名某比赛
     */
    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId} AND user_id = #{userId}")
    int countByContestAndUser(Integer contestId, Long userId);

    /**
     * 查询某比赛的报名人数
     */
    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId}")
    int countByContest(Integer contestId);
}
