package com.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.constant.MessageConstant;
import com.oj.common.constant.StatusConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.exception.DeletionNotAllowedException;
import com.oj.common.result.Result;
import com.oj.common.result.PageResult;
import com.oj.contest.dto.ContestDTO;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.entity.Contest;
import com.oj.contest.entity.ContestProblem;
import com.oj.contest.mapper.ContestMapper;
import com.oj.contest.mapper.ContestProblemMapper;
import com.oj.contest.service.ContestService;
import com.oj.contest.vo.ContestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContestServiceImpl implements ContestService {

    @Autowired
    private ContestMapper contestMapper;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private UserClient userClient;

    @Override
    public void saveContest(ContestDTO contestDTO) {
        Long currentId = BaseContext.getCurrentId();
        Contest contest = new Contest();
        BeanUtils.copyProperties(contestDTO, contest);
        contest.setCreatedAt(LocalDateTime.now());
        contest.setUpdatedAt(LocalDateTime.now());
        contest.setCreatedBy(currentId);
        contestMapper.insert(contest);

        var problemList = contestDTO.getProblemList();
        if (problemList != null && !problemList.isEmpty()) {
            List<ContestProblem> contestProblems = problemList.stream().map(p -> {
                ContestProblem cp = new ContestProblem();
                cp.setContestId(contest.getId());
                cp.setScore(p.getScore() != null ? p.getScore() : 10);
                cp.setCreatedAt(LocalDateTime.now());
                cp.setProblemId(p.getId());
                return cp;
            }).toList();
            contestProblemMapper.insert(contestProblems);
        }
    }

    @Override
    public PageResult pageContest(ContestQueryDTO contestQueryDTO) {
        Page<ContestVO> contestVOPage = contestQueryDTO.toPageDefaultSortByCreateTime("created_at");
        QueryWrapper<Contest> queryWrapper = new QueryWrapper<>();
        String title = contestQueryDTO.getTitle();
        String status = contestQueryDTO.getStatus();
        queryWrapper.like(title != null && !title.isEmpty(), "c.title", title)
                .eq(status != null && !status.isEmpty(), "c.status", status);
        Page<ContestVO> contestVOList = contestMapper.selectPage(contestVOPage, queryWrapper);

        // 填充创建者名称（原为JOIN user表，微服务改为Feign调用）
        fillCreatedName(contestVOList.getRecords());

        return new PageResult(contestVOList.getTotal(), contestVOList.getRecords());
    }

    @Override
    public ContestVO selectId(Long id) {
        Contest contest = contestMapper.selectById(id);
        ContestVO contestVO = new ContestVO();
        if (contest != null) {
            BeanUtils.copyProperties(contest, contestVO);
        }

        // 查询竞赛关联的题目ID列表
        List<Long> problemIds = contestProblemMapper.selectListAll(id);
        if (!problemIds.isEmpty()) {
            // 通过Feign调用获取题目详情
            Result<List<ProblemFeignDTO>> result = problemClient.getProblemsByIds(
                    problemIds.stream().map(Long::intValue).toList());
            if (result != null && result.getData() != null) {
                List<ContestVO.ContestProblemVO> problemVOList = result.getData().stream().map(p -> {
                    ContestVO.ContestProblemVO pvo = new ContestVO.ContestProblemVO();
                    pvo.setId(p.getId());
                    pvo.setTitle(p.getTitle());
                    pvo.setDifficulty(p.getDifficulty());
                    pvo.setAcceptance(p.getAcceptance());
                    pvo.setStatus(p.getStatus());
                    return pvo;
                }).toList();
                contestVO.setProblemList(problemVOList);
            }
        } else {
            contestVO.setProblemList(Collections.emptyList());
        }

        return contestVO;
    }

    @Override
    public void update(ContestDTO contestDTO) {
        Contest contest = new Contest();
        BeanUtils.copyProperties(contestDTO, contest);
        contestMapper.updateById(contest);

        var problemList = contestDTO.getProblemList();
        contestProblemMapper.deleteProblem(contestDTO.getId());
        if (problemList != null && !problemList.isEmpty()) {
            List<ContestProblem> contestProblems = problemList.stream().map(p -> {
                ContestProblem cp = new ContestProblem();
                cp.setProblemId(p.getId());
                cp.setScore(p.getScore());
                cp.setCreatedAt(LocalDateTime.now());
                cp.setContestId(contestDTO.getId());
                return cp;
            }).toList();
            contestProblemMapper.insert(contestProblems);
        }
    }

    @Override
    public void deleteId(Long id) {
        List<Long> problemIds = contestProblemMapper.selectListAll(id);
        // 通过Feign检查题目是否发布
        for (Long problemId : problemIds) {
            Result<ProblemFeignDTO> result = problemClient.getProblemById(problemId.intValue());
            if (result != null && result.getData() != null
                    && result.getData().getStatus() != null
                    && result.getData().getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.CONTEXT_PROBLEM);
            }
        }
        contestMapper.deleteById(id);
        contestProblemMapper.deleteProblem(Math.toIntExact(id));
    }

    @Override
    public void updateContestStatus() {
        List<Contest> contestList = contestMapper.selectList(null);
        LocalDateTime now = LocalDateTime.now();
        contestList.forEach(contest -> {
            String newStatus = calculateStatus(contest, now);
            if (!contest.getStatus().equals(newStatus)) {
                contest.setStatus(newStatus);
                contestMapper.updateById(contest);
            }
        });
    }

    private String calculateStatus(Contest contest, LocalDateTime now) {
        if (contest.getStartTime().isAfter(now)) {
            return "Upcoming";
        } else if (!contest.getEndTime().isBefore(now)) {
            return "Running";
        } else {
            return "Ended";
        }
    }

    private void fillCreatedName(List<ContestVO> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> createdByList = records.stream()
                .map(ContestVO::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (createdByList.isEmpty()) return;

        Result<List<UserFeignDTO>> result = userClient.getUsersByIds(createdByList);
        if (result != null && result.getData() != null) {
            Map<Long, String> nameMap = result.getData().stream()
                    .collect(Collectors.toMap(UserFeignDTO::getId, UserFeignDTO::getUsername, (a, b) -> a));
            records.forEach(vo -> {
                if (vo.getCreatedBy() != null) {
                    vo.setCreatedName(nameMap.get(vo.getCreatedBy()));
                }
            });
        }
    }
}
