package com.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.ProblemTypesRel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface ProblemTypeRel extends BaseMapper<ProblemTypesRel> {
    default List<Long> selectTypeList(Long id) {
        return selectList(new LambdaQueryWrapper<ProblemTypesRel>().eq(id != null, ProblemTypesRel::getTypeId, id))
                .stream()
                .map(rel -> rel.getProblemId().longValue())
                .collect(Collectors.toList());
    }

  default   void deleteType(Integer id){
      LambdaUpdateWrapper<ProblemTypesRel> lambdaUpdateWrapper=new LambdaUpdateWrapper<>();
      lambdaUpdateWrapper.eq(id!=null,ProblemTypesRel::getProblemId,id);
      delete(lambdaUpdateWrapper);
  }

  default   void deleteProblem(List<Long> ids){
        LambdaUpdateWrapper<ProblemTypesRel> lambdaUpdateWrapper=new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.in(ProblemTypesRel::getProblemId,ids);
        delete(lambdaUpdateWrapper);
  }
}
