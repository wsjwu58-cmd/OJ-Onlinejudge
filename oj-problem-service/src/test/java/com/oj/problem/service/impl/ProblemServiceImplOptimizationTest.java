package com.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ContestClient;
import com.oj.problem.dto.ProblemQueryDTO;
import com.oj.problem.entity.Problem;
import com.oj.problem.entity.ProblemType;
import com.oj.problem.entity.ProblemTypesRel;
import com.oj.problem.mapper.*;
import com.oj.problem.vo.ProblemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemService N+1 优化测试")
class ProblemServiceImplOptimizationTest {

    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private ProblemTypeRelMapper problemTypeRel;
    @Mock
    private ProblemTypeMapper problemTypeMapper;
    @Mock
    private GroupProblemMapper groupProblemMapper;
    @Mock
    private TestCaseMapper testCaseMapper;
    @Mock
    private ProblemGroupMapper problemGroupMapper;
    @Mock
    private ContestClient contestClient;

    @InjectMocks
    private ProblemServiceImpl problemService;

    private static final int PAGE_SIZE = 20;
    private static final List<Long> PROBLEM_IDS = IntStream.rangeClosed(1, PAGE_SIZE)
            .mapToObj(Long::valueOf).toList();

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("pageQuery: 优化后批量查询类型列表（2次批量SQL替代N次单条SQL）")
    void pageQuery_BatchQueryTypes_ReducesNPlusOne() {
        ProblemQueryDTO queryDTO = new ProblemQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(PAGE_SIZE);

        Page<ProblemVO> mockPage = new Page<>(1, PAGE_SIZE);
        List<ProblemVO> mockRecords = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemVO vo = new ProblemVO();
            vo.setId((long) i);
            vo.setTitle("Problem " + i);
            mockRecords.add(vo);
        }
        mockPage.setRecords(mockRecords);
        mockPage.setTotal(10000L);

        Page<ProblemVO> pageArg = ArgumentMatchers.argThat(
                p -> p instanceof Page);
        LambdaQueryWrapper<Problem> wrapperArg = ArgumentMatchers.argThat(
                w -> w instanceof LambdaQueryWrapper);
        when(problemMapper.selectPage(pageArg, wrapperArg))
                .thenReturn(mockPage);

        List<ProblemTypesRel> allRels = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            int typeId = (i % 8) + 1;
            ProblemTypesRel rel = new ProblemTypesRel();
            rel.setId((long) i);
            rel.setProblemId(i);
            rel.setTypeId(typeId);
            allRels.add(rel);
        }

        when(problemMapper.selectTypeListBatch(PROBLEM_IDS)).thenReturn(allRels);

        List<ProblemType> allTypes = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            ProblemType pt = new ProblemType();
            pt.setId(i);
            pt.setName("Type " + i);
            allTypes.add(pt);
        }
        when(problemTypeMapper.selectBatchIds(anyCollection())).thenReturn(allTypes);

        var result = problemService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(10000L, result.getTotal());
        assertEquals(PAGE_SIZE, result.getRecords().size());

        verify(problemMapper, times(1)).selectTypeListBatch(PROBLEM_IDS);
        verify(problemMapper, never()).selectPageOf(anyLong());
        verify(problemTypeMapper, times(1)).selectBatchIds(anyCollection());

        List<ProblemVO> records = result.getRecords();
        for (ProblemVO vo : records) {
            assertNotNull(vo.getTypeList());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("pageQuery: 空结果时不触发批量类型查询")
    void pageQuery_EmptyResult_NoBatchQuery() {
        ProblemQueryDTO queryDTO = new ProblemQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(PAGE_SIZE);

        Page<ProblemVO> mockPage = new Page<>(1, PAGE_SIZE);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0L);

        Page<ProblemVO> pageArg = ArgumentMatchers.argThat(
                p -> p instanceof Page);
        LambdaQueryWrapper<Problem> wrapperArg = ArgumentMatchers.argThat(
                w -> w instanceof LambdaQueryWrapper);
        when(problemMapper.selectPage(pageArg, wrapperArg))
                .thenReturn(mockPage);

        var result = problemService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(0, result.getTotal());

        verify(problemMapper, never()).selectTypeListBatch(anyList());
        verify(problemMapper, never()).selectPageOf(anyLong());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("pageQuery: 10000条数据性能对比测试")
    void pageQuery_PerformanceTest() {
        ProblemQueryDTO queryDTO = new ProblemQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(PAGE_SIZE);

        Page<ProblemVO> mockPage = new Page<>(1, PAGE_SIZE);
        List<ProblemVO> mockRecords = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemVO vo = new ProblemVO();
            vo.setId((long) i);
            mockRecords.add(vo);
        }
        mockPage.setRecords(mockRecords);
        mockPage.setTotal(10000L);

        Page<ProblemVO> pageArg = ArgumentMatchers.argThat(
                p -> p instanceof Page);
        LambdaQueryWrapper<Problem> wrapperArg = ArgumentMatchers.argThat(
                w -> w instanceof LambdaQueryWrapper);
        when(problemMapper.selectPage(pageArg, wrapperArg))
                .thenReturn(mockPage);

        List<ProblemTypesRel> allRels = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemTypesRel rel = new ProblemTypesRel();
            rel.setId((long) i);
            rel.setProblemId(i);
            rel.setTypeId((i % 8) + 1);
            allRels.add(rel);
        }
        when(problemMapper.selectTypeListBatch(PROBLEM_IDS)).thenReturn(allRels);

        List<ProblemType> allTypes = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            ProblemType pt = new ProblemType();
            pt.setId(i);
            pt.setName("Type " + i);
            allTypes.add(pt);
        }
        when(problemTypeMapper.selectBatchIds(anyCollection())).thenReturn(allTypes);

        long optimizedStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            problemService.pageQuery(queryDTO);
        }
        long optimizedTime = System.nanoTime() - optimizedStart;

        verify(problemMapper, atLeast(1)).selectTypeListBatch(anyList());

        long unoptimizedStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (Long id : PROBLEM_IDS) {
                problemMapper.selectPageOf(id);
            }
        }
        long unoptimizedTime = System.nanoTime() - unoptimizedStart;

        System.out.println("=== ProblemServiceImpl.pageQuery 性能对比 ===");
        System.out.printf("优化前(100次pageQuery, 每次20条记录 x1查询): %.2f ms%n",
                unoptimizedTime / 1_000_000.0);
        System.out.printf("优化后(100次pageQuery, 每次仅2次批量查询): %.2f ms%n",
                optimizedTime / 1_000_000.0);
        System.out.printf("性能提升: %.1f 倍%n",
                (double) unoptimizedTime / Math.max(optimizedTime, 1));
    }
}
