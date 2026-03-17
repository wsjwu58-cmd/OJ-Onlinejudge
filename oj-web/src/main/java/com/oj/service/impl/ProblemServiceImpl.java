package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.constant.MessageConstant;
import com.oj.constant.StatusConstant;
import com.oj.context.BaseContext;
import com.oj.dto.ProblemDTO;
import com.oj.dto.ProblemQueryDTO;
import com.oj.entity.*;
import com.oj.enumeration.ActivityType;
import com.oj.exception.DeletionNotAllowedException;
import com.oj.mapper.*;
import com.oj.result.PageResult;
import com.oj.service.ProblemService;
import com.oj.service.WorkSpaceService;
import com.oj.utils.MarkdownUtil;
import com.oj.vo.ProblemVO;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProblemServiceImpl implements ProblemService {
    @Autowired
    ProblemMapper problemMapper;
    @Autowired
    ProblemTypeRel problemTypeRel;
    @Autowired
    private Parser parser; // 注入配置类中的 Bean

    @Autowired
    private HtmlRenderer renderer; // 注入配置类中的 Bean

    MarkdownUtil markdownUtil;
    @Autowired
    private GroupProblemMapper groupProblemMapper;
    @Autowired
    private ContestProblemMapper contestProblemMapper;
    @Autowired
    private ProblemGroupMapper problemGroupMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkSpaceService workSpaceService;


    //新增题目
    @Override
    public void problemSave(ProblemDTO problemDTO) {
        //创建者ID
        Long currentId = BaseContext.getCurrentId();
        User user = userMapper.selectById(currentId);
        Problem problem=new Problem();
        BeanUtils.copyProperties(problemDTO,problem);
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        problemMapper.insert(problem);
        //获取题目ID
        long id=problem.getId();
        List<ProblemType> typeList=problemDTO.getTypeList();
        //获取种类和题目对应关系数据
        List<ProblemTypesRel> typesRels = typeList.stream().map(type -> {
            ProblemTypesRel problemTypesRel = new ProblemTypesRel();
            problemTypesRel.setTypeId(type.getId());
            problemTypesRel.setProblemId((int) id);
            problemTypesRel.setCreatedAt(LocalDateTime.now());
            return problemTypesRel;
        }).toList();
        problemTypeRel.insert(typesRels);

        workSpaceService.recordWorkSpace(currentId, String.valueOf(ActivityType.PROBLEM_CREATE),"题目创建",
                user.getUsername()+"创建了题目"+problem.getTitle(),null,"Problem");
    }

    @Override
    public PageResult pageQuery(ProblemQueryDTO problemQueryDTO) {
        Page<ProblemVO> problemVOPage=problemQueryDTO.ToPageDefaultSortByCreateTime("created_at");
        LambdaQueryWrapper<Problem> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        String title=problemQueryDTO.getTitle();
        String difficulty = problemQueryDTO.getDifficulty();
        lambdaQueryWrapper.like(title!=null&&!title.isEmpty(),Problem::getTitle,title)
                .eq(difficulty!=null&&!difficulty.isEmpty(),Problem::getDifficulty,difficulty);
        Page<ProblemVO> problemVOPage1=problemMapper.selectPage(problemVOPage,lambdaQueryWrapper);
        // 4. 将原始查询结果聚合为 ProblemVO 列表
        List<ProblemVO> list = problemVOPage1.getRecords().stream().map(problemVO -> {
            List<ProblemType> typeList = problemMapper.selectPageOf(problemVO.getId());
            problemVO.setTypeList(typeList);
            return problemVO;
        }).toList();


        return new PageResult(problemVOPage1.getTotal(),list);
    }

    @Override
    public ProblemVO problemById(Long id) {
        Problem problem=problemMapper.selectById(id);
        //获取markdown文本进行渲染
        if(problem!=null){
            MarkdownUtil.parseMarkdownField(problem,"content","contentHtml",parser,renderer);
        }
        ProblemVO problemVO=new ProblemVO();
        if (problem != null) {
            BeanUtils.copyProperties(problem,problemVO);
            problemVO.setId(Long.valueOf(problem.getId()));
        }
        List<ProblemType> typeList=problemMapper.selectPageOf(id);
        problemVO.setTypeList(typeList);
        LambdaQueryWrapper<TestCase> testCaseLambdaQueryWrapper=new LambdaQueryWrapper<>();
        testCaseLambdaQueryWrapper.eq(TestCase::getProblemId,id);
        List<TestCase> testCases = testCaseMapper.selectList(testCaseLambdaQueryWrapper);
        problemVO.setTestCaseList(testCases);
        return problemVO;

    }

    @Override
    public void problemStatus(Integer status, Long id) {
        Problem problem=new Problem();
        problem.setId(Math.toIntExact(id));
        problem.setStatus(status);
        //下架题目
        problemMapper.updateById(problem);

        //如果题目关联题组，就停用题组
        if(status== StatusConstant.DISABLE){
            List<GroupProblems> list=groupProblemMapper.selectGroup(id);
            if(list!=null&&!list.isEmpty()){
                for (GroupProblems groupProblems:list){
                    ProblemGroup problemGroup=problemGroupMapper.selectById(groupProblems.getGroupId());
                    problemGroup.setStatus(status);
                    problemGroupMapper.updateById(problemGroup);
                }
            }
        }
    }

    @Override
    public void deleteProblem(Long id) {
        //如果关联题组，则不能删除
        Long count=groupProblemMapper.selectProblemCount(id);
        if(count>0){
            throw new DeletionNotAllowedException(MessageConstant.GROUP_PROBLEM);
        }
        //如果关联比赛，则不能删除
        Long contextCont=contestProblemMapper.selectContentCount(id);
        if(contextCont>0){
            throw new DeletionNotAllowedException(MessageConstant.Context_PROBLEM);
        }
        problemMapper.deleteById(id);
    }



    @Override
    public void updateProblem(ProblemDTO problemDTO) {
        Problem problem = new Problem();
        BeanUtils.copyProperties(problemDTO, problem);
        problemMapper.updateById(problem);
        //获取分类数据
        List<ProblemType> list = problemDTO.getTypeList();
        //删除原来的数据
        problemTypeRel.deleteType(problemDTO.getId());
        //插入新的数据
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
        //判断当前题目是否能被删除
        ids.forEach(newid->{
            Problem problem=problemMapper.selectById(newid);
            if(problem.getStatus()!=StatusConstant.DISABLE){
                throw new DeletionNotAllowedException(MessageConstant.PROBLEM_STATUS);
            }
        });

        //是否被题组关联
        LambdaQueryWrapper<GroupProblems> groupProblemsLambdaQueryWrapper=new LambdaQueryWrapper<>();
        groupProblemsLambdaQueryWrapper.in(GroupProblems::getProblemId,ids)
                .select(GroupProblems::getGroupId);
        List<Integer> list = groupProblemMapper.selectList(groupProblemsLambdaQueryWrapper).stream().map(GroupProblems::getGroupId).toList();
        if(!list.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.GROUP_PROBLEM);
        }
        //是否和比赛关联
        LambdaQueryWrapper<ContestProblem> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(ContestProblem::getProblemId,ids)
                        .select(ContestProblem::getContestId);
        List<Integer> list1 = contestProblemMapper.selectList(lambdaQueryWrapper).stream().map(ContestProblem::getContestId).toList();
        if (!list1.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.Context_PROBLEM);
        }
        //删除题目，关联类型，测试用例
        problemMapper.deleteByIds(ids);
        problemTypeRel.deleteProblem(ids);
        testCaseMapper.deleteProblem(ids);
    }

    @Override
    public PageResult queryType(ProblemQueryDTO problemQueryDTO) {
        Page<Problem> problemPage=problemQueryDTO.ToPageDefaultSortByCreateTime("created_at");
        //查询条件
        LambdaQueryWrapper<Problem> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        String difficulty=problemQueryDTO.getDifficulty();
        Integer status=problemQueryDTO.getStatus();
        Integer typeId=problemQueryDTO.getProblemTypeId();
        lambdaQueryWrapper.eq(difficulty!=null&&!difficulty.isEmpty(),Problem::getDifficulty,difficulty)
                .eq(status!=null,Problem::getStatus,status);
        if(typeId!=null){
            // 使用子查询：先查询关联表中包含该类型的题目ID
            LambdaQueryWrapper<ProblemTypesRel> relWrapper = new LambdaQueryWrapper<>();
            relWrapper.eq(ProblemTypesRel::getTypeId, typeId);
            List<ProblemTypesRel> rels = problemMapper.selectProblemTypesRel(relWrapper);

            if (!rels.isEmpty()) {
                List<Integer> problemIds = rels.stream()
                        .map(ProblemTypesRel::getProblemId)
                        .toList();
                lambdaQueryWrapper.in(Problem::getId, problemIds);
            } else {
                // 如果没有关联记录，返回空结果
                lambdaQueryWrapper.eq(Problem::getId, -1);
            }
        }
        //分页查询
        // 执行分页查询
        Page<Problem> resultPage = problemMapper.selectPage(problemPage, lambdaQueryWrapper);

        //转换成vo
        List<ProblemVO> problemVOList = resultPage.getRecords().stream().map(problem -> {
            ProblemVO problemVO = new ProblemVO();
            BeanUtils.copyProperties(problem, problemVO);
            problemVO.setId(Long.valueOf(problem.getId()));
            List<ProblemType> typeList = problemMapper.selectPageOf(Long.valueOf(problem.getId()));
            problemVO.setTypeList(typeList);
            return problemVO;
        }).toList();

        // 构建返回结果
        return new PageResult(resultPage.getTotal(), problemVOList);
    }


}
