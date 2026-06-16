package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.contest.entity.Contest;
import com.oj.contest.vo.ContestVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface ContestMapper extends BaseMapper<Contest> {
    Page<ContestVO> selectPage(Page<ContestVO> page, @Param(Constants.WRAPPER) Wrapper<Contest> ew);

    Integer CountContest(Map map);

    void batchUpdateStatus(@Param("upcomingStatus") String upcomingStatus,
                           @Param("now1") LocalDateTime now1,
                           @Param("runningStatus") String runningStatus,
                           @Param("now2") LocalDateTime now2,
                           @Param("endedStatus") String endedStatus,
                           @Param("now3") LocalDateTime now3);
}
