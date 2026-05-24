package com.oj.judge.service;

import com.oj.judge.dto.JudgeRunDTO;
import com.oj.judge.dto.JudgeSubmitDTO;
import com.oj.judge.vo.JudgeResultVO;

public interface JudgeService {
    JudgeResultVO submit(JudgeSubmitDTO dto, Long userId);
    JudgeResultVO run(JudgeRunDTO dto);
}
