package com.oj.service;

import com.oj.vo.ContestDataVO;
import com.oj.vo.ProblemDataVO;
import com.oj.vo.WorkDataVO;
import com.oj.vo.WorkSpaceVO;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkSpaceService {
    List<WorkSpaceVO> getWorkspace(Integer limit);
     void recordWorkSpace(Long userId,String activityType,String title,
                                String description,Long targetId,String targetType);

    WorkDataVO getWorkData(LocalDateTime begin,LocalDateTime end);

    ProblemDataVO getProblem();

    ContestDataVO getContest();

    void export(HttpServletResponse httpServletResponse);
}
