package com.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ContestClient;
import com.oj.api.UserClient;
import com.oj.api.dto.UserFeignDTO;
import com.oj.api.dto.WorkspaceActivityFeignDTO;
import com.oj.common.constant.MessageConstant;
import com.oj.common.constant.StatusConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.enumeration.ActivityType;
import com.oj.common.exception.DeletionNotAllowedException;
import com.oj.common.result.PageResult;
import com.oj.common.utils.MarkdownUtil;
import com.oj.problem.dto.ProblemDTO;
import com.oj.problem.dto.ProblemQueryDTO;
import com.oj.problem.entity.*;
import com.oj.problem.mapper.*;
import com.oj.problem.service.ProblemService;
import com.oj.problem.vo.ProblemVO;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ProblemServiceImpl implements ProblemService {
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private ProblemTypeRelMapper problemTypeRel;
    @Autowired
    private Parser parser;
    @Autowired
    private HtmlRenderer renderer;
    @Autowired
    private GroupProblemMapper groupProblemMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private ProblemGroupMapper problemGroupMapper;
    @Autowired
    private UserClient userClient;
    @Autowired
    private ContestClient contestClient;

    @Override
    public void problemSave(ProblemDTO problemDTO) {
        Long currentId = BaseContext.getCurrentId();
        Problem problem = new Problem();
        BeanUtils.copyProperties(problemDTO, problem);
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        problemMapper.insert(problem);

        long id = problem.getId();

        // 处理 Hack 资源文件（validator.cpp + reference.cpp）
        saveHackFiles((int) id, problemDTO);

        List<ProblemType> typeList = problemDTO.getTypeList();
        List<ProblemTypesRel> typesRels = typeList.stream().map(type -> {
            ProblemTypesRel problemTypesRel = new ProblemTypesRel();
            problemTypesRel.setTypeId(type.getId());
            problemTypesRel.setProblemId((int) id);
            problemTypesRel.setCreatedAt(LocalDateTime.now());
            return problemTypesRel;
        }).toList();
        problemTypeRel.insert(typesRels);

        // 通过Feign记录活动
        try {
            UserFeignDTO user = userClient.getUserById(currentId).getData();
            String username = user != null ? user.getUsername() : "用户";
            WorkspaceActivityFeignDTO dto = WorkspaceActivityFeignDTO.builder()
                    .userId(currentId)
                    .activityType(String.valueOf(ActivityType.PROBLEM_CREATE))
                    .title("题目创建")
                    .description(username + "创建了题目" + problem.getTitle())
                    .targetId(null)
                    .targetType("Problem")
                    .build();
            contestClient.recordWorkspaceActivity(dto);
        } catch (Exception e) {
            log.warn("记录题目创建活动失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult pageQuery(ProblemQueryDTO problemQueryDTO) {
        Page<ProblemVO> problemVOPage = problemQueryDTO.toPageDefaultSortByCreateTime("created_at");
        LambdaQueryWrapper<Problem> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String title = problemQueryDTO.getTitle();
        String difficulty = problemQueryDTO.getDifficulty();
        lambdaQueryWrapper.like(title != null && !title.isEmpty(), Problem::getTitle, title)
                .eq(difficulty != null && !difficulty.isEmpty(), Problem::getDifficulty, difficulty);
        Page<ProblemVO> problemVOPage1 = problemMapper.selectPage(problemVOPage, lambdaQueryWrapper);
        List<ProblemVO> list = problemVOPage1.getRecords().stream().map(problemVO -> {
            List<ProblemType> typeList = problemMapper.selectPageOf(problemVO.getId());
            problemVO.setTypeList(typeList);
            return problemVO;
        }).toList();
        return new PageResult(problemVOPage1.getTotal(), list);
    }

    @Override
    public ProblemVO problemById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem != null) {
            MarkdownUtil.parseMarkdownField(problem, "content", "contentHtml", parser, renderer);
        }
        ProblemVO problemVO = new ProblemVO();
        if (problem != null) {
            BeanUtils.copyProperties(problem, problemVO);
            problemVO.setId(Long.valueOf(problem.getId()));
        }
        List<ProblemType> typeList = problemMapper.selectPageOf(id);
        problemVO.setTypeList(typeList);
        LambdaQueryWrapper<TestCase> testCaseLambdaQueryWrapper = new LambdaQueryWrapper<>();
        testCaseLambdaQueryWrapper.eq(TestCase::getProblemId, id);
        List<TestCase> testCases = testCaseMapper.selectList(testCaseLambdaQueryWrapper);
        problemVO.setTestCaseList(testCases);
        return problemVO;
    }

    @Override
    public void problemStatus(Integer status, Long id) {
        Problem problem = new Problem();
        problem.setId(Math.toIntExact(id));
        problem.setStatus(status);
        problemMapper.updateById(problem);

        if (status == StatusConstant.DISABLE) {
            List<GroupProblems> list = groupProblemMapper.selectGroup(id);
            if (list != null && !list.isEmpty()) {
                for (GroupProblems groupProblems : list) {
                    ProblemGroup problemGroup = problemGroupMapper.selectById(groupProblems.getGroupId());
                    problemGroup.setStatus(status);
                    problemGroupMapper.updateById(problemGroup);
                }
            }
        }
    }

    @Override
    public void deleteProblem(Long id) {
        Long count = groupProblemMapper.selectProblemCount(id);
        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.GROUP_PROBLEM);
        }
        // 通过Feign检查是否关联比赛
        try {
            var result = contestClient.countContestByProblemId(Math.toIntExact(id));
            if (result.getData() != null && result.getData() > 0) {
                throw new DeletionNotAllowedException(MessageConstant.CONTEXT_PROBLEM);
            }
        } catch (DeletionNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("检查题目是否关联比赛失败: {}", e.getMessage());
        }
        problemMapper.deleteById(id);
    }

    @Override
    public void updateProblem(ProblemDTO problemDTO) {
        Problem problem = new Problem();
        BeanUtils.copyProperties(problemDTO, problem);
        problemMapper.updateById(problem);

        // 处理 Hack 资源文件
        saveHackFiles(problemDTO.getId(), problemDTO);

        List<ProblemType> list = problemDTO.getTypeList();
        problemTypeRel.deleteType(problemDTO.getId());
        List<ProblemTypesRel> typesRels = list.stream().map(problemType -> {
            ProblemTypesRel problemTypesRel = new ProblemTypesRel();
            problemTypesRel.setCreatedAt(LocalDateTime.now());
            problemTypesRel.setProblemId(problemDTO.getId());
            problemTypesRel.setTypeId(problemType.getId());
            return problemTypesRel;
        }).toList();
        problemTypeRel.insert(typesRels);
    }

    @Override
    public List<Problem> selectAll() {
        return problemMapper.selectAll();
    }

    @Override
    public void deleteAll(List<Long> ids) {
        ids.forEach(newid -> {
            Problem problem = problemMapper.selectById(newid);
            if (problem.getStatus() != StatusConstant.DISABLE) {
                throw new DeletionNotAllowedException(MessageConstant.PROBLEM_STATUS);
            }
        });

        LambdaQueryWrapper<GroupProblems> groupProblemsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        groupProblemsLambdaQueryWrapper.in(GroupProblems::getProblemId, ids)
                .select(GroupProblems::getGroupId);
        List<Integer> list = groupProblemMapper.selectList(groupProblemsLambdaQueryWrapper).stream().map(GroupProblems::getGroupId).toList();
        if (!list.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.GROUP_PROBLEM);
        }

        try {
            for (Long id : ids) {
                var result = contestClient.countContestByProblemId(Math.toIntExact(id));
                if (result.getData() != null && result.getData() > 0) {
                    throw new DeletionNotAllowedException(MessageConstant.CONTEXT_PROBLEM);
                }
            }
        } catch (DeletionNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("检查题目是否关联比赛失败: {}", e.getMessage());
        }

        problemMapper.deleteByIds(ids);
        problemTypeRel.deleteProblem(ids);
        testCaseMapper.deleteProblem(ids);
    }

    @Override
    public PageResult queryType(ProblemQueryDTO problemQueryDTO) {
        Page<Problem> problemPage = problemQueryDTO.toPageDefaultSortByCreateTime("created_at");
        LambdaQueryWrapper<Problem> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String difficulty = problemQueryDTO.getDifficulty();
        Integer status = problemQueryDTO.getStatus();
        Integer typeId = problemQueryDTO.getProblemTypeId();
        lambdaQueryWrapper.eq(difficulty != null && !difficulty.isEmpty(), Problem::getDifficulty, difficulty)
                .eq(status != null, Problem::getStatus, status);
        if (typeId != null) {
            LambdaQueryWrapper<ProblemTypesRel> relWrapper = new LambdaQueryWrapper<>();
            relWrapper.eq(ProblemTypesRel::getTypeId, typeId);
            List<ProblemTypesRel> rels = problemMapper.selectProblemTypesRel(relWrapper);
            if (!rels.isEmpty()) {
                List<Integer> problemIds = rels.stream()
                        .map(ProblemTypesRel::getProblemId)
                        .toList();
                lambdaQueryWrapper.in(Problem::getId, problemIds);
            } else {
                lambdaQueryWrapper.eq(Problem::getId, -1);
            }
        }
        Page<Problem> resultPage = problemMapper.selectPage(problemPage, lambdaQueryWrapper);
        List<ProblemVO> problemVOList = resultPage.getRecords().stream().map(problem -> {
            ProblemVO problemVO = new ProblemVO();
            BeanUtils.copyProperties(problem, problemVO);
            problemVO.setId(Long.valueOf(problem.getId()));
            List<ProblemType> typeList = problemMapper.selectPageOf(Long.valueOf(problem.getId()));
            problemVO.setTypeList(typeList);
            return problemVO;
        }).toList();
        return new PageResult(resultPage.getTotal(), problemVOList);
    }

    private void saveHackFiles(int problemId, ProblemDTO dto) {
        try {
            String baseDir = "E:" + File.separator + "oj-microservice" + File.separator + "hack-data" + File.separator + "problem-" + problemId;
            Path dirPath = Paths.get(baseDir);
            Files.createDirectories(dirPath);

            String validatorPath = null;
            String validatorExePath = null;
            String validatorSrcHash = null;
            String referencePath = null;

            if (dto.getValidatorCode() != null && !dto.getValidatorCode().isEmpty()) {
                validatorPath = dirPath.resolve("validator.cpp").toString();
                Files.writeString(Paths.get(validatorPath), dto.getValidatorCode());
                log.info("Validator 源码已写入: {}", validatorPath);

                validatorExePath = dirPath.resolve("validator.exe").toString();
                validatorSrcHash = sha256(Paths.get(validatorPath));
            }

            if (dto.getReferenceCode() != null && !dto.getReferenceCode().isEmpty()) {
                String refLang = dto.getReferenceLanguage() != null ? dto.getReferenceLanguage() : "C++";
                String ext = switch (refLang.toLowerCase()) {
                    case "java" -> "java";
                    case "python" -> "py";
                    default -> "cpp";
                };
                referencePath = dirPath.resolve("reference." + ext).toString();
                Files.writeString(Paths.get(referencePath), dto.getReferenceCode());
                log.info("标准解答已写入: {}", referencePath);
            }

            if (validatorPath != null || referencePath != null) {
                Problem update = new Problem();
                update.setId(problemId);
                update.setValidatorPath(validatorPath);
                update.setValidatorExePath(validatorExePath);
                update.setValidatorSrcHash(validatorSrcHash);
                update.setReferencePath(referencePath);
                update.setReferenceLanguage(dto.getReferenceLanguage() != null ? dto.getReferenceLanguage() : "C++");
                problemMapper.updateById(update);
            }
        } catch (Exception e) {
            log.error("写入 Hack 资源文件失败: {}", e.getMessage(), e);
        }
    }

    private String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
