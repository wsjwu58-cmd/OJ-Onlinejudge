package com.oj.problem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.problem.entity.GroupTypesRel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupTypeRelMapper extends BaseMapper<GroupTypesRel> {
    default List<Long> selectTypeList(Long id) {
        return selectList(new LambdaQueryWrapper<GroupTypesRel>().eq(id != null, GroupTypesRel::getTypeId, id))
                .stream().map(rol -> rol.getGroupId().longValue()).toList();
    }
}
