package com.oj.service;

import com.oj.vo.ProblemAcceptanceVO;
import com.oj.vo.ProblemTrendVO;
import com.oj.vo.RecordTrendVO;
import com.oj.vo.UserTrendVO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    UserTrendVO getUserTrend(LocalDate begin, LocalDate end);

    ProblemTrendVO problemTrend(LocalDate begin, LocalDate end);

    RecordTrendVO recordTrend(LocalDate begin, LocalDate end);

    List<ProblemAcceptanceVO> problemAccept();

}
