package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.entity.Contest;
import com.oj.entity.Problem;
import com.oj.vo.ContestVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContestMapper extends BaseMapper<Contest> {
    
    Page<ContestVO> selectPage(Page<ContestVO> page, @Param(Constants.WRAPPER) Wrapper<Contest> ew);

    List<Problem> SelectById(Long id);

    Integer CountContest(Map map);
}
