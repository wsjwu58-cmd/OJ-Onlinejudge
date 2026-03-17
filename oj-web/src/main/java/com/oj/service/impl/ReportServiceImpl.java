package com.oj.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.ReportService;
import com.oj.vo.ProblemAcceptanceVO;
import com.oj.vo.ProblemTrendVO;
import com.oj.vo.RecordTrendVO;
import com.oj.vo.UserTrendVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private SubMissionMapper subMissionMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String PROBLEM_ACCEPTANCE_TOP10_KEY = "problem:acceptance:top10";
    private static final long CACHE_TTL = 10; // 缓存10分钟
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserTrendVO getUserTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList=new ArrayList<>();
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> integerList=new ArrayList<>();
        for (LocalDate localDate:localDateList){
            LocalDateTime beginTime=LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            Integer userTotal=userMapper.countCreateUser(map);
            integerList.add(userTotal==null?0:userTotal);
        }
        return UserTrendVO.builder().
                dateList(StringUtils.join(localDateList,",")).
                turnoverList(StringUtils.join(integerList,","))
                .build();
    }

    @Override
    public ProblemTrendVO problemTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList=new ArrayList<>();
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> integerList=new ArrayList<>();
        //新增题目
        List<Integer> newproblemList=new ArrayList<>();
        for (LocalDate localDate:localDateList){
            LocalDateTime beginTime=LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            Map map=new HashMap();
            map.put("end",endTime);
            //题目总数
            Integer countProblem = problemMapper.countProblem(map);
            integerList.add(countProblem==null?0:countProblem);
            //新增题目数
            Map mapnew=new HashMap();
            mapnew.put("begin",beginTime);
            mapnew.put("end",endTime);
            Integer countProblem1 = problemMapper.countProblem(mapnew);
            newproblemList.add(countProblem1==null?0:countProblem1);
        }
        return ProblemTrendVO.builder()
                .newProblemList(StringUtils.join(newproblemList,","))
                .dateList(StringUtils.join(localDateList,","))
                .turnoverList(StringUtils.join(integerList,","))
                .build();

    }

    @Override
    public RecordTrendVO recordTrend(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList=new ArrayList<>();
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            localDateList.add(begin);
        }
        //AC题目数
        List<Integer> AcProblem=new ArrayList<>();
        //通过率
        List<Double> Percent=new ArrayList<>();
        //提交记录
        List<Integer> recordList=new ArrayList<>();
        for (LocalDate localDate:localDateList){
            LocalDateTime beginTime=LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            Map map=new HashMap();
            map.put("end",endTime);
            //提交记录
            Integer record = subMissionMapper.countSubmission(map);
            recordList.add(record==null?0:record);
            //AC数
            map.put("begin",beginTime);
            map.put("status","Accepted");
            Integer submission = subMissionMapper.countSubmission(map);
            AcProblem.add(submission==null?0:submission);
            //通过率
            Double percent=0.0;
            if(record!=0){
                percent=submission.doubleValue()/record;
            }
            Percent.add(percent);

        }
        return RecordTrendVO.builder()
                .dateList(StringUtils.join(localDateList,","))
                .AcProblemList(StringUtils.join(AcProblem,","))
                .PercentList(StringUtils.join(Percent,","))
                .turnoverList(StringUtils.join(recordList,",")).build();
    }

    @Override
    public List<ProblemAcceptanceVO> problemAccept() {
        Long size = redisTemplate.opsForZSet().size(PROBLEM_ACCEPTANCE_TOP10_KEY);
        if(size!=null&&size>0){
            return redisTemplate.opsForZSet().reverseRange(PROBLEM_ACCEPTANCE_TOP10_KEY,0,9)
                    .stream().map(json->{
                        try {
                            return objectMapper.readValue(json, ProblemAcceptanceVO.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }).filter(dto->dto!=null)
                    .toList();

        }
        //查数据库
        List<ProblemAcceptanceVO> top10=problemMapper.selectCount10();
        //写入redis
        redisTemplate.opsForZSet().removeRange(PROBLEM_ACCEPTANCE_TOP10_KEY,0,-1);
        for (ProblemAcceptanceVO dto : top10) {
            try {
                if (dto==null){
                    break;
                }
                String json = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForZSet().add(PROBLEM_ACCEPTANCE_TOP10_KEY, json, dto.getAcceptance());
            } catch (Exception ignored) {}
        }
        redisTemplate.expire(PROBLEM_ACCEPTANCE_TOP10_KEY, CACHE_TTL, TimeUnit.MINUTES);
        if (top10.isEmpty()){
            return Collections.emptyList();
        }
        return top10;
    }




}
