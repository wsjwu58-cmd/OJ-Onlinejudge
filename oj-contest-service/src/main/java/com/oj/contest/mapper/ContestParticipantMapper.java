package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.ContestParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContestParticipantMapper extends BaseMapper<ContestParticipant> {

    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId} AND user_id = #{userId}")
    int countByContestAndUser(Integer contestId, Long userId);

    @Select("SELECT COUNT(*) FROM contest_participants WHERE contest_id = #{contestId}")
    int countByContest(Integer contestId);

    @Select("<script>" +
            "SELECT contest_id, COUNT(*) AS cnt FROM contest_participants " +
            "WHERE contest_id IN " +
            "<foreach collection='contestIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY contest_id" +
            "</script>")
    List<Map<String, Object>> countByContestBatch(List<Integer> contestIds);
}
