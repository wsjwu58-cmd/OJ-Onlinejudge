package com.oj.service.impl;

import com.alibaba.fastjson.JSON;
import com.oj.constant.RedisConstant;
import com.oj.constant.StatusConstant;
import com.oj.mapper.ContestMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.WorkSpaceService;
import com.oj.vo.ContestDataVO;
import com.oj.vo.ProblemDataVO;
import com.oj.vo.WorkDataVO;
import com.oj.vo.WorkSpaceVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkSpaceServiceImpl implements WorkSpaceService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //活动数量限制
    private static final int MAX_ACTIVITIES=50;
    @Autowired
    private SubMissionMapper subMissionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProblemMapper problemMapper;

    // Redis 键常量

    private static final long STATS_EXPIRE_TIME = 3600; // 1小时过期
    @Autowired
    private ContestMapper contestMapper;


    @Override
    public List<WorkSpaceVO> getWorkspace(Integer limit) {
        try {
            //获取活动（时间倒叙）
            Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisConstant.RECENT_ACTIVITIES_KEY, 0, limit - 1);
            if(typedTuples!=null&&!typedTuples.isEmpty()){
                //解析redis数据为VO
                return typedTuples.stream()
                        .map(tuple -> JSON.parseObject(tuple.getValue(), WorkSpaceVO.class))
                        .collect(Collectors.toList());

            }
            //无活动数据，返回空
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("获取活动失败",e);
            return Collections.emptyList();
        }
    }
    /*
    记录活动（直接写入redis）
     */
    public void recordWorkSpace(Long userId,String activityType,String title,
                                String description,Long targetId,String targetType){
        try {
            //创建Vo
            WorkSpaceVO workSpaceVO=new WorkSpaceVO();
            workSpaceVO.setId(generateActivityId());
            workSpaceVO.setTitle(title);
            workSpaceVO.setDescription(description);
            workSpaceVO.setCreateTime(LocalDateTime.now());
            workSpaceVO.setActivityType(activityType);
            //序列化为JSON
            String activityJson=JSON.toJSONString(workSpaceVO);
            //添加到 REDIS ZSET
            stringRedisTemplate.opsForZSet().add(RedisConstant.RECENT_ACTIVITIES_KEY,activityJson,
                    System.currentTimeMillis());
            // 限制活动数量，只保留最近的 MAX_ACTIVITIES 条
            stringRedisTemplate.opsForZSet().removeRange(RedisConstant.RECENT_ACTIVITIES_KEY,
                    0, -MAX_ACTIVITIES - 1);
        } catch (Exception e) {
            log.error("记录活动到redis失败",e);
        }

    }


    @Override
    public WorkDataVO getWorkData(LocalDateTime begin,LocalDateTime end) {
        try {
            // 尝试从 Redis 获取数据
            String statsJson = stringRedisTemplate.opsForValue().get(RedisConstant.DASHBOARD_STATS_KEY);
            if (statsJson != null) {
                log.info("从Redis获取运营数据");
                return JSON.parseObject(statsJson, WorkDataVO.class);
            }

            // Redis 中没有数据，从数据库获取
            WorkDataVO workDataVO = calculateWorkData(begin,end);

            // 将数据存储到 Redis
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.DASHBOARD_STATS_KEY,
                    JSON.toJSONString(workDataVO),
                    STATS_EXPIRE_TIME
            );

            return workDataVO;
        } catch (Exception e) {
            log.error("获取运营数据失败", e);
            // 异常时从数据库获取
            return calculateWorkData(begin,end);
        }
    }

    @Override
    public ProblemDataVO getProblem() {
        Map map=new HashMap();
        map.put("status", StatusConstant.DISABLE);
        //查询未发布题目数量
        Integer total=problemMapper.countProblemDis(map);
        map.put("status",StatusConstant.ENABLE);
        //发布题目数量
        Integer newTotal=problemMapper.countProblem(map);
        return ProblemDataVO.builder().disSend(total).send(newTotal).build();
    }

    @Override
    public ContestDataVO getContest() {
        Map map=new HashMap();
        map.put("status","Upcoming");
        //查询未开始比赛
        Integer total=contestMapper.CountContest(map);
        map.put("status","Running");
        //进行中比赛
        Integer newTotal=contestMapper.CountContest(map);
        map.put("status","Ended");
        //结束比赛
        Integer finalTotal=contestMapper.CountContest(map);
        return ContestDataVO.builder().disSend(total).send(newTotal).finalSend(finalTotal).build();

    }

    @Override
    public void export(HttpServletResponse httpServletResponse) {
        //设置日期为30天前到一天前
        LocalDate begin=LocalDate.now().minusDays(30);
        LocalDate end=LocalDate.now().minusDays(1);

        WorkDataVO workDataVO=this.getWorkData(LocalDateTime.of(begin,LocalTime.MIN),LocalDateTime.of(end,LocalTime.MAX));
        InputStream inputStream=this.getClass().getClassLoader().getResourceAsStream("template/businessData.xlsx");
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            //获取页
            XSSFSheet sheet1 = xssfWorkbook.getSheet("Sheet1");
            //获取行
            XSSFRow xssfRow=sheet1.getRow(3);
            XSSFRow xssfRow1=sheet1.getRow(4);
            //获取单元格
            xssfRow.getCell(2).setCellValue(workDataVO.getTotalUsers());
            xssfRow.getCell(4).setCellValue(workDataVO.getActiveUsersToday());
            xssfRow.getCell(6).setCellValue(workDataVO.getTotalProblems());

            xssfRow1.getCell(2).setCellValue(workDataVO.getActiveChange());
            xssfRow1.getCell(4).setCellValue(workDataVO.getProblemChange());

            //明细数据
            for(int i=0;i<30;i++){
                LocalDate data=begin.plusDays(i);
                WorkDataVO workData=this.getWorkData(LocalDateTime.of(data,LocalTime.MIN),LocalDateTime.of(data,LocalTime.MAX));
                xssfRow=sheet1.getRow(7+i);
                xssfRow.getCell(1).setCellValue(data.toString());
                xssfRow.getCell(2).setCellValue(workDataVO.getTotalUsers());
                xssfRow.getCell(3).setCellValue(workDataVO.getActiveChange());
                xssfRow.getCell(4).setCellValue(workDataVO.getActiveUsersToday());
                xssfRow.getCell(5).setCellValue(workDataVO.getProblemChange());
                xssfRow.getCell(6).setCellValue(workDataVO.getTotalProblems());
            }
            //输出
            ServletOutputStream servletOutputStream=httpServletResponse.getOutputStream();
            xssfWorkbook.write(servletOutputStream);
            xssfWorkbook.close();
            servletOutputStream.flush();
            servletOutputStream.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }


    public WorkDataVO calculateWorkData(LocalDateTime begin, LocalDateTime end) {
        Map map= new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        //提交记录数
        Integer total=subMissionMapper.countSubmission(map);
        //通过的记录
        map.put("status","Accepted");
        Integer currentTotal=subMissionMapper.countSubmission(map);
        //总用户数
        Long l = userMapper.selectCount(null);
        //题目总数
        Long l1 = problemMapper.selectCount(null);
        //今日活跃用户
        Integer todayUser=userMapper.countUserToday(map);
        //增长率
        LocalDate yesterDay = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayBegin=LocalDateTime.of(yesterDay,LocalTime.MIN);
        LocalDateTime yesterdayEnd=LocalDateTime.of(yesterDay,LocalTime.MAX);
        Map mapyesterDay=new HashMap();
        mapyesterDay.put("begin",yesterdayBegin);
        mapyesterDay.put("end",yesterdayEnd);
        //用户增长率
        Integer userYesterday=userMapper.countUserBeforeToday(begin);
        //活跃用户变化率
        Integer userHotYesterday=userMapper.countUserToday(mapyesterDay);
        //提交数变化率
        Integer submissionYesterday=subMissionMapper.countSubmission(mapyesterDay);
        //题目总数变化率
        Integer problemYesterday=problemMapper.countProblem(mapyesterDay);

        return WorkDataVO.builder().totalUsers(Math.toIntExact(l)).
                submissionsToday(currentTotal).
                activeUsersToday(todayUser).
                totalProblems(Math.toIntExact(l1)).
                userChange(calculateGrowthRate(Math.toIntExact(l),userYesterday)).
                activeChange(calculateGrowthRate(todayUser,userHotYesterday)).
                submissionChange(calculateGrowthRate(Math.toIntExact(l1),submissionYesterday)).
                problemChange(calculateGrowthRate(Math.toIntExact(l1),problemYesterday)).
                build();
    }

    /**
     * 计算增长率
     */
    private Double calculateGrowthRate(Integer current, Integer previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0; // 之前为0，现在有值，增长率100%
        }
        return ((double) (current - previous) / previous) * 100;
    }


    //生成UUID
    private String generateActivityId() {
        // 使用 UUID 生成唯一 ID
        return UUID.randomUUID().toString().replace("-", "");
    }
}
