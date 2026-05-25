package com.oj.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.contest.entity.HackRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HackRecordMapper extends BaseMapper<HackRecord> {
}
