package com.oj.contest.service;

import com.oj.contest.vo.ProblemAcceptanceVO;
import com.oj.contest.vo.ProblemTrendVO;
import com.oj.contest.vo.RecordTrendVO;
import com.oj.contest.vo.UserTrendVO;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    UserTrendVO getUserTrend(LocalDate begin, LocalDate end);
    ProblemTrendVO problemTrend(LocalDate begin, LocalDate end);
    RecordTrendVO recordTrend(LocalDate begin, LocalDate end);
    List<ProblemAcceptanceVO> problemAccept();
}
