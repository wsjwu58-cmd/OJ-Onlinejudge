package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.constant.MessageConstant;
import com.oj.constant.StatusConstant;
import com.oj.context.BaseContext;
import com.oj.dto.ContestDTO;
import com.oj.dto.ContestQueryDTO;
import com.oj.entity.Contest;
import com.oj.entity.ContestProblem;
import com.oj.entity.Problem;
import com.oj.exception.DeletionNotAllowedException;
import com.oj.mapper.ContestMapper;
import com.oj.mapper.ContestProblemMapper;
import com.oj.mapper.GroupProblemMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.result.PageResult;
import com.oj.service.ContestService;
import com.oj.vo.ContestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContestServiceImpl implements ContestService {

    @Autowired
    private ContestMapper contestMapper;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private GroupProblemMapper groupProblemMapper;

    @Override
    public void saveContest(ContestDTO contestDTO) {
        //获取创建人ID
        Long currentId = BaseContext.getCurrentId();
        System.out.println(currentId);
        //获取比赛基本信息
        Contest contest=new Contest();
        BeanUtils.copyProperties(contestDTO,contest);
        //设置基本信息
        contest.setCreatedAt(LocalDateTime.now());
        contest.setUpdatedAt(LocalDateTime.now());
        contest.setCreatedBy(currentId);
        contestMapper.insert(contest);
        //获取题目信息
        List<Problem> problemList=contestDTO.getProblemList();
        if(problemList!=null&&!problemList.isEmpty()) {
            List<ContestProblem> contestProblems = problemList.stream().map(problem -> {
                if (problem.getScore() == null) {
                    //如果不设置分数，默认为10分
                    problem.setScore(10);
                }
                ContestProblem contestProblem = new ContestProblem();
                contestProblem.setContestId(contest.getId());
                contestProblem.setScore(problem.getScore());
                contestProblem.setCreatedAt(LocalDateTime.now());
                contestProblem.setProblemId(problem.getId());
                return contestProblem;
            }).toList();
            contestProblemMapper.insert(contestProblems);
        }

    }

    @Override
    public PageResult pageContest(ContestQueryDTO contestQueryDTO) {
        //根据创建时间排序
        Page<ContestVO> contestVOPage=contestQueryDTO.ToPageDefaultSortByCreateTime("created_at");
        //创建构造器（使用 QueryWrapper 手动指定表别名，避免 LEFT JOIN user 时 status 歧义）
        QueryWrapper<Contest> queryWrapper=new QueryWrapper<>();
        //获取查询条件
        String title=contestQueryDTO.getTitle();
        String status = contestQueryDTO.getStatus();
        queryWrapper.like(title!=null&&!title.isEmpty(),"c.title",title)
                .eq(status!=null&&!status.isEmpty(),"c.status",status);
        //分页查询
        Page<ContestVO> contestVOList= contestMapper.selectPage(contestVOPage,queryWrapper);

        return new PageResult(contestVOList.getTotal(),contestVOList.getRecords());
    }

    @Override
    public ContestVO selectId(Long id) {
        Contest contest=contestMapper.selectById(id);
        ContestVO contestVO=new ContestVO();
        if(contest!=null){

            BeanUtils.copyProperties(contest,contestVO);
        }
        List<Problem> problemList=contestMapper.SelectById(id);
        contestVO.setProblemList(problemList);
        return contestVO;
    }

    @Override
    public void update(ContestDTO contestDTO) {
        Contest contest=new Contest();
        BeanUtils.copyProperties(contestDTO,contest);
        contestMapper.updateById(contest);
        //修改题目
        List<Problem> problemList=contestDTO.getProblemList();
        //删除原来的数据
        contestProblemMapper.deleteProblem(contestDTO.getId());
        //插入修改后的数据
        List<ContestProblem> contestProblems = problemList.stream().map(problem -> {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setProblemId(problem.getId());
            contestProblem.setScore(problem.getScore());
            contestProblem.setCreatedAt(LocalDateTime.now());
            contestProblem.setContestId(contestDTO.getId());
            return contestProblem;
        }).toList();
        contestProblemMapper.insert(contestProblems);
    }

    @Override
    public void deleteId(Long id) {
        //如果比赛中有发布的题目，则不能删除
        List<Long> ids=contestProblemMapper.selectListAll(id);
        List<Problem> list = ids.stream().map(id1 -> {
            Problem problem = problemMapper.selectById(id1);
            if (problem.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.Context_PROBLEM);
            }
            return problem;
        }).toList();
        if(!list.isEmpty()){
            contestMapper.deleteById(id);
            contestProblemMapper.deleteProblem(Math.toIntExact(id));
        }
    }

    @Override
    public void updateContestStatus() {
        //获取所有比赛
        List<Contest> contestList=contestMapper.selectList(null);
        //当前时间
        LocalDateTime localDateTime=LocalDateTime.now();
        //批量更新状态
        contestList.forEach(contest -> {
            //计算比赛状态
            String newStatus = calculateStatus(contest, localDateTime);
            if (!contest.getStatus().equals(newStatus)){
                contest.setStatus(newStatus);
                contestMapper.updateById(contest);
            }
        });
    }

    private String calculateStatus(Contest contest, LocalDateTime localDateTime) {
        LocalDateTime startTime = contest.getStartTime();
        LocalDateTime endTime = contest.getEndTime();

        //判断状态
        if(startTime.isAfter(LocalDateTime.now())){
            return "Upcoming";
        }else if (startTime.isBefore(LocalDateTime.now())&&endTime.isAfter(LocalDateTime.now())){
            return "Running";
        }else {
            return "Ended";
        }
    }

}
