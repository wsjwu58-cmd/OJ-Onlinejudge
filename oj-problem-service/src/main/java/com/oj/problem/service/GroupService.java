package com.oj.problem.service;

import com.oj.common.result.PageResult;
import com.oj.problem.dto.GroupDTO;
import com.oj.problem.dto.GroupQueryDTO;
import com.oj.problem.entity.Problem;
import com.oj.problem.vo.GroupVO;

import java.util.List;

public interface GroupService {
    void saveGroup(GroupDTO groupDTO);
    PageResult pageGroup(GroupQueryDTO groupQueryDTO);
    GroupVO selectId(Long id);
    void update(GroupDTO groupDTO);
    void deleteId(Long id);
    void status(Integer status, Long id);
    List<Problem> getGroupProblems(Long groupId, GroupQueryDTO groupQueryDTO);
}
