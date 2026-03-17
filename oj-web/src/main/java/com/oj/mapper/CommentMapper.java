package com.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.SolutionComment;
import com.oj.vo.SolutionVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<SolutionComment> {
    List<SolutionVO> selectVOByIdsOrdered(List<Long> ids);
}
