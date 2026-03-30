package com.oj.service;

import com.oj.dto.GroupDTO;
import com.oj.dto.GroupQueryDTO;
import com.oj.entity.Problem;
import com.oj.result.PageResult;
import com.oj.vo.GroupVO;

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
