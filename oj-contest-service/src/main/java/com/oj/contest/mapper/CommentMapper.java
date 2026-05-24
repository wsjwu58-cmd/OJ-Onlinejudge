package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.SolutionComment;
import com.oj.contest.vo.SolutionVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<SolutionComment> {
    List<SolutionVO> selectByIdsOrdered(List<Long> ids);
}
