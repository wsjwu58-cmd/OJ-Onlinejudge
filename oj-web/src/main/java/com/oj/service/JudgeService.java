package com.oj.service;

import com.oj.dto.JudgeRunDTO;
import com.oj.dto.JudgeSubmitDTO;
import com.oj.vo.JudgeResultVO;

public interface JudgeService {
    /**
     * 提交代码判题（跑所有测试用例，结果写入 submissions 表）
     */
    JudgeResultVO submit(JudgeSubmitDTO dto, Long userId);

    /**
     * 运行代码（只跑用户输入或第一组示例，不入库）
     */
    JudgeResultVO run(JudgeRunDTO dto);
}
