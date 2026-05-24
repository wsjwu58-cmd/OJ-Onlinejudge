package com.oj.contest.service.impl;

import com.alibaba.fastjson.JSON;
import com.oj.api.JudgeClient;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.common.constant.RedisConstant;
import com.oj.common.constant.StatusConstant;
import com.oj.common.result.Result;
import com.oj.contest.mapper.ContestMapper;
import com.oj.contest.service.WorkSpaceService;
import com.oj.contest.vo.ContestDataVO;
import com.oj.contest.vo.ProblemDataVO;
import com.oj.contest.vo.WorkDataVO;
import com.oj.contest.vo.WorkSpaceVO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkSpaceServiceImpl implements WorkSpaceService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private JudgeClient judgeClient;

    @Autowired
    private ContestMapper contestMapper;

    private static final int MAX_ACTIVITIES = 50;
    private static final long STATS_EXPIRE_TIME = 3600;

    @Override
    public List<WorkSpaceVO> getWorkspace(Integer limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisConstant.RECENT_ACTIVITIES_KEY, 0, limit - 1);
            if (typedTuples != null && !typedTuples.isEmpty()) {
                return typedTuples.stream()
                        .map(tuple -> JSON.parseObject(tuple.getValue(), WorkSpaceVO.class))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("获取活动失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void recordWorkSpace(Long userId, String activityType, String title,
                                String description, Long targetId, String targetType) {
        try {
            WorkSpaceVO workSpaceVO = new WorkSpaceVO();
            workSpaceVO.setId(generateActivityId());
            workSpaceVO.setTitle(title);
            workSpaceVO.setDescription(description);
            workSpaceVO.setCreateTime(LocalDateTime.now());
            workSpaceVO.setActivityType(activityType);
            String activityJson = JSON.toJSONString(workSpaceVO);
            stringRedisTemplate.opsForZSet().add(RedisConstant.RECENT_ACTIVITIES_KEY, activityJson,
                    System.currentTimeMillis());
            stringRedisTemplate.opsForZSet().removeRange(RedisConstant.RECENT_ACTIVITIES_KEY,
                    0, -MAX_ACTIVITIES - 1);
        } catch (Exception e) {
            log.error("记录活动到redis失败", e);
        }
    }

    @Override
    public WorkDataVO getWorkData(LocalDateTime begin, LocalDateTime end) {
        try {
            String statsJson = stringRedisTemplate.opsForValue().get(RedisConstant.DASHBOARD_STATS_KEY);
            if (statsJson != null) {
                log.info("从Redis获取运营数据");
                return JSON.parseObject(statsJson, WorkDataVO.class);
            }

            WorkDataVO workDataVO = calculateWorkData(begin, end);

            stringRedisTemplate.opsForValue().set(
                    RedisConstant.DASHBOARD_STATS_KEY,
                    JSON.toJSONString(workDataVO),
                    STATS_EXPIRE_TIME
            );

            return workDataVO;
        } catch (Exception e) {
            log.error("获取运营数据失败", e);
            return calculateWorkData(begin, end);
        }
    }

    @Override
    public ProblemDataVO getProblem() {
        // 通过Feign调用题目服务统计
        Result<Integer> disResult = problemClient.countProblemsByStatus(StatusConstant.DISABLE);
        Result<Integer> sendResult = problemClient.countProblemsByStatus(StatusConstant.ENABLE);
        return ProblemDataVO.builder()
                .disSend(disResult != null && disResult.getData() != null ? disResult.getData() : 0)
                .send(sendResult != null && sendResult.getData() != null ? sendResult.getData() : 0)
                .build();
    }

    @Override
    public ContestDataVO getContest() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "Upcoming");
        Integer total = contestMapper.CountContest(map);
        map.put("status", "Running");
        Integer newTotal = contestMapper.CountContest(map);
        map.put("status", "Ended");
        Integer finalTotal = contestMapper.CountContest(map);
        return ContestDataVO.builder()
                .disSend(total)
                .send(newTotal)
                .finalSend(finalTotal)
                .build();
    }

    @Override
    public void export(HttpServletResponse httpServletResponse) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        WorkDataVO workDataVO = this.getWorkData(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/businessData.xlsx");
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet1 = xssfWorkbook.getSheet("Sheet1");
            XSSFRow xssfRow = sheet1.getRow(3);
            XSSFRow xssfRow1 = sheet1.getRow(4);
            xssfRow.getCell(2).setCellValue(workDataVO.getTotalUsers());
            xssfRow.getCell(4).setCellValue(workDataVO.getActiveUsersToday());
            xssfRow.getCell(6).setCellValue(workDataVO.getTotalProblems());
            xssfRow1.getCell(2).setCellValue(workDataVO.getActiveChange());
            xssfRow1.getCell(4).setCellValue(workDataVO.getProblemChange());

            for (int i = 0; i < 30; i++) {
                LocalDate data = begin.plusDays(i);
                WorkDataVO workData = this.getWorkData(
                        LocalDateTime.of(data, LocalTime.MIN),
                        LocalDateTime.of(data, LocalTime.MAX));
                xssfRow = sheet1.getRow(7 + i);
                xssfRow.getCell(1).setCellValue(data.toString());
                xssfRow.getCell(2).setCellValue(workDataVO.getTotalUsers());
                xssfRow.getCell(3).setCellValue(workDataVO.getActiveChange());
                xssfRow.getCell(4).setCellValue(workDataVO.getActiveUsersToday());
                xssfRow.getCell(5).setCellValue(workDataVO.getProblemChange());
                xssfRow.getCell(6).setCellValue(workDataVO.getTotalProblems());
            }

            ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();
            xssfWorkbook.write(servletOutputStream);
            xssfWorkbook.close();
            servletOutputStream.flush();
            servletOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WorkDataVO calculateWorkData(LocalDateTime begin, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String beginStr = begin.format(fmt);
        String endStr = end.format(fmt);

        // 提交记录数
        Result<Integer> totalResult = judgeClient.countSubmissionsByDate(null, endStr);
        int total = (totalResult != null && totalResult.getData() != null) ? totalResult.getData() : 0;

        // 通过记录数
        Result<Integer> acceptedResult = judgeClient.countSubmissionsByDateAndStatus(beginStr, endStr, "Accepted");
        int currentTotal = (acceptedResult != null && acceptedResult.getData() != null) ? acceptedResult.getData() : 0;

        // 总用户数
        Result<Long> userCountResult = userClient.countUsers();
        int totalUsers = (userCountResult != null && userCountResult.getData() != null)
                ? Math.toIntExact(userCountResult.getData()) : 0;

        // 题目总数
        Result<Long> problemCountResult = problemClient.countProblems();
        int totalProblems = (problemCountResult != null && problemCountResult.getData() != null)
                ? Math.toIntExact(problemCountResult.getData()) : 0;

        // 今日活跃用户
        Result<Integer> todayUserResult = userClient.countUsersToday(beginStr, endStr);
        int todayUser = (todayUserResult != null && todayUserResult.getData() != null)
                ? todayUserResult.getData() : 0;

        // 增长率计算
        LocalDate yesterDay = LocalDate.now().minusDays(1);
        String yBegin = LocalDateTime.of(yesterDay, LocalTime.MIN).format(fmt);
        String yEnd = LocalDateTime.of(yesterDay, LocalTime.MAX).format(fmt);

        Result<Integer> userYesterdayResult = userClient.countUserBeforeToday(beginStr);
        int userYesterday = (userYesterdayResult != null && userYesterdayResult.getData() != null)
                ? userYesterdayResult.getData() : 0;

        Result<Integer> userHotYesterdayResult = userClient.countUsersToday(yBegin, yEnd);
        int userHotYesterday = (userHotYesterdayResult != null && userHotYesterdayResult.getData() != null)
                ? userHotYesterdayResult.getData() : 0;

        Result<Integer> submissionYesterdayResult = judgeClient.countSubmissionsByDate(yBegin, yEnd);
        int submissionYesterday = (submissionYesterdayResult != null && submissionYesterdayResult.getData() != null)
                ? submissionYesterdayResult.getData() : 0;

        Result<Integer> problemYesterdayResult = problemClient.countProblemsByDate(yBegin, yEnd);
        int problemYesterday = (problemYesterdayResult != null && problemYesterdayResult.getData() != null)
                ? problemYesterdayResult.getData() : 0;

        return WorkDataVO.builder()
                .totalUsers(totalUsers)
                .submissionsToday(currentTotal)
                .activeUsersToday(todayUser)
                .totalProblems(totalProblems)
                .userChange(calculateGrowthRate(totalUsers, userYesterday))
                .activeChange(calculateGrowthRate(todayUser, userHotYesterday))
                .submissionChange(calculateGrowthRate(totalProblems, submissionYesterday))
                .problemChange(calculateGrowthRate(totalProblems, problemYesterday))
                .build();
    }

    private Double calculateGrowthRate(Integer current, Integer previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }

    private String generateActivityId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
