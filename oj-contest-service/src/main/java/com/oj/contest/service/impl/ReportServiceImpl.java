package com.oj.contest.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.api.JudgeClient;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemAcceptanceFeignDTO;
import com.oj.common.result.Result;
import com.oj.contest.service.ReportService;
import com.oj.contest.vo.ProblemAcceptanceVO;
import com.oj.contest.vo.ProblemTrendVO;
import com.oj.contest.vo.RecordTrendVO;
import com.oj.contest.vo.UserTrendVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private JudgeClient judgeClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PROBLEM_ACCEPTANCE_TOP10_KEY = "problem:acceptance:top10";
    private static final long CACHE_TTL = 10;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public UserTrendVO getUserTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> integerList = new ArrayList<>();
        for (LocalDate localDate : localDateList) {
            String beginStr = LocalDateTime.of(localDate, LocalTime.MIN).format(fmt);
            String endStr = LocalDateTime.of(localDate, LocalTime.MAX).format(fmt);
            Result<Integer> result = userClient.countCreateUser(beginStr, endStr);
            integerList.add(result != null && result.getData() != null ? result.getData() : 0);
        }
        return UserTrendVO.builder()
                .dateList(StringUtils.join(localDateList, ","))
                .turnoverList(StringUtils.join(integerList, ","))
                .build();
    }

    @Override
    public ProblemTrendVO problemTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> integerList = new ArrayList<>();
        List<Integer> newProblemList = new ArrayList<>();
        for (LocalDate localDate : localDateList) {
            String endStr = LocalDateTime.of(localDate, LocalTime.MAX).format(fmt);
            String beginStr = LocalDateTime.of(localDate, LocalTime.MIN).format(fmt);

            // 题目总数（截止到该日）
            Result<Integer> countResult = problemClient.countProblemsByDate(null, endStr);
            integerList.add(countResult != null && countResult.getData() != null ? countResult.getData() : 0);

            // 新增题目数
            Result<Integer> newResult = problemClient.countProblemsByDate(beginStr, endStr);
            newProblemList.add(newResult != null && newResult.getData() != null ? newResult.getData() : 0);
        }
        return ProblemTrendVO.builder()
                .newProblemList(StringUtils.join(newProblemList, ","))
                .dateList(StringUtils.join(localDateList, ","))
                .turnoverList(StringUtils.join(integerList, ","))
                .build();
    }

    @Override
    public RecordTrendVO recordTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> acProblem = new ArrayList<>();
        List<Double> percent = new ArrayList<>();
        List<Integer> recordList = new ArrayList<>();
        for (LocalDate localDate : localDateList) {
            String beginStr = LocalDateTime.of(localDate, LocalTime.MIN).format(fmt);
            String endStr = LocalDateTime.of(localDate, LocalTime.MAX).format(fmt);

            // 提交记录
            Result<Integer> recordResult = judgeClient.countSubmissionsByDate(null, endStr);
            int record = recordResult != null && recordResult.getData() != null ? recordResult.getData() : 0;
            recordList.add(record);

            // AC数
            Result<Integer> acResult = judgeClient.countSubmissionsByDateAndStatus(beginStr, endStr, "Accepted");
            int submission = acResult != null && acResult.getData() != null ? acResult.getData() : 0;
            acProblem.add(submission);

            // 通过率
            percent.add(record != 0 ? submission * 1.0 / record : 0.0);
        }
        return RecordTrendVO.builder()
                .dateList(StringUtils.join(localDateList, ","))
                .AcProblemList(StringUtils.join(acProblem, ","))
                .PercentList(StringUtils.join(percent, ","))
                .turnoverList(StringUtils.join(recordList, ","))
                .build();
    }

    @Override
    public List<ProblemAcceptanceVO> problemAccept() {
        // 尝试从Redis获取
        Long size = redisTemplate.opsForZSet().size(PROBLEM_ACCEPTANCE_TOP10_KEY);
        if (size != null && size > 0) {
            return redisTemplate.opsForZSet().reverseRange(PROBLEM_ACCEPTANCE_TOP10_KEY, 0, 9)
                    .stream().map(json -> {
                        try {
                            return objectMapper.readValue(json, ProblemAcceptanceVO.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }).filter(Objects::nonNull)
                    .toList();
        }

        // 通过Feign调用题目服务获取
        Result<List<ProblemAcceptanceFeignDTO>> result = problemClient.selectAcceptanceTop10();
        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return Collections.emptyList();
        }

        List<ProblemAcceptanceVO> top10 = result.getData().stream().map(dto -> {
            ProblemAcceptanceVO vo = new ProblemAcceptanceVO();
            vo.setId(dto.getId());
            vo.setTitle(dto.getTitle());
            vo.setAcceptance(dto.getAcceptance());
            return vo;
        }).toList();

        // 写入Redis缓存
        redisTemplate.opsForZSet().removeRange(PROBLEM_ACCEPTANCE_TOP10_KEY, 0, -1);
        for (ProblemAcceptanceVO dto : top10) {
            try {
                String json = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForZSet().add(PROBLEM_ACCEPTANCE_TOP10_KEY, json,
                        dto.getAcceptance() != null ? dto.getAcceptance().doubleValue() : 0);
            } catch (Exception ignored) {
            }
        }
        redisTemplate.expire(PROBLEM_ACCEPTANCE_TOP10_KEY, CACHE_TTL, TimeUnit.MINUTES);

        return top10;
    }
}
