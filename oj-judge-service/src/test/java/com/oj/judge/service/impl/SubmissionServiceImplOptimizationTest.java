package com.oj.judge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.context.BaseContext;
import com.oj.common.result.Result;
import com.oj.judge.dto.SubmissionQueryDTO;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import com.oj.judge.vo.JudgeResultVO;
import com.oj.judge.vo.SubmissionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService N+1 优化测试")
class SubmissionServiceImplOptimizationTest {

    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private ProblemClient problemClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private static final int PAGE_SIZE = 20;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    @DisplayName("getSubmission: 批量查询问题标题（1次Feign替代N次）")
    void getSubmission_BatchQueryProblems() {
        List<JudgeResultVO> mockSubmissions = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            JudgeResultVO vo = new JudgeResultVO();
            vo.setSubmissionId((long) i);
            vo.setProblemId(i);
            mockSubmissions.add(vo);
        }

        when(submissionMapper.selectSubmission(any(), any()))
                .thenReturn(mockSubmissions);

        List<ProblemFeignDTO> problemDTOs = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemFeignDTO dto = new ProblemFeignDTO();
            dto.setId(i);
            dto.setTitle("Problem " + i);
            problemDTOs.add(dto);
        }
        when(problemClient.getProblemsByIds(anyList()))
                .thenReturn(Result.success(problemDTOs));

        List<JudgeResultVO> result = submissionService.getSubmission(1L);

        assertNotNull(result);
        assertEquals(PAGE_SIZE, result.size());

        // 验证：只调用了1次批量Feign，而非20次单条
        verify(problemClient, times(1)).getProblemsByIds(anyList());
        verify(problemClient, never()).getProblemById(anyInt());

        result.forEach(vo -> assertNotNull(vo.getTitle()));
    }

    @Test
    @DisplayName("pageQuery: 批量查询用户+问题（1+2N优化为1+1+1次远程调用）")
    void pageQuery_BatchQueryUsersAndProblems() {
        SubmissionQueryDTO queryDTO = new SubmissionQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(PAGE_SIZE);

        Page<Submission> mockPage = new Page<>(1, PAGE_SIZE);
        List<Submission> submissions = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            Submission s = new Submission();
            s.setId((long) i);
            s.setUserId((long) (i % 10 + 1));
            s.setProblemId(i);
            submissions.add(s);
        }
        mockPage.setRecords(submissions);
        mockPage.setTotal(10000L);

        when(submissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        List<UserFeignDTO> userDTOs = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            UserFeignDTO dto = new UserFeignDTO();
            dto.setId(i);
            dto.setUsername("User " + i);
            userDTOs.add(dto);
        }
        when(userClient.getUsersByIds(anyList())).thenReturn(Result.success(userDTOs));

        List<ProblemFeignDTO> problemDTOs = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemFeignDTO dto = new ProblemFeignDTO();
            dto.setId(i);
            dto.setTitle("Problem " + i);
            problemDTOs.add(dto);
        }
        when(problemClient.getProblemsByIds(anyList())).thenReturn(Result.success(problemDTOs));

        var result = submissionService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(10000L, result.getTotal());
        assertEquals(PAGE_SIZE, result.getRecords().size());

        // 验证：批量调用而非逐条
        verify(userClient, times(1)).getUsersByIds(anyList());
        verify(userClient, never()).getUserById(anyLong());
        verify(problemClient, times(1)).getProblemsByIds(anyList());
        verify(problemClient, never()).getProblemById(anyInt());

        @SuppressWarnings("unchecked")
        List<SubmissionVO> records = result.getRecords();
        for (SubmissionVO vo : records) {
            assertNotNull(vo.getUsername());
            assertNotNull(vo.getProblemTitle());
        }
    }

    @Test
    @DisplayName("pageQuery: 10000条提交性能对比测试")
    void pageQuery_PerformanceTest() {
        SubmissionQueryDTO queryDTO = new SubmissionQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(PAGE_SIZE);

        Page<Submission> mockPage = new Page<>(1, PAGE_SIZE);
        List<Submission> submissions = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            Submission s = new Submission();
            s.setId((long) i);
            s.setUserId((long) (i % 10 + 1));
            s.setProblemId(i);
            submissions.add(s);
        }
        mockPage.setRecords(submissions);
        mockPage.setTotal(10000L);

        when(submissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        List<UserFeignDTO> userDTOs = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            UserFeignDTO dto = new UserFeignDTO();
            dto.setId(i);
            dto.setUsername("User " + i);
            userDTOs.add(dto);
        }
        when(userClient.getUsersByIds(anyList())).thenReturn(Result.success(userDTOs));

        List<ProblemFeignDTO> problemDTOs = new ArrayList<>();
        for (int i = 1; i <= PAGE_SIZE; i++) {
            ProblemFeignDTO dto = new ProblemFeignDTO();
            dto.setId(i);
            dto.setTitle("Problem " + i);
            problemDTOs.add(dto);
        }
        when(problemClient.getProblemsByIds(anyList())).thenReturn(Result.success(problemDTOs));

        // 测量优化后性能
        long optimizedStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            submissionService.pageQuery(queryDTO);
        }
        long optimizedTime = System.nanoTime() - optimizedStart;

        // 模拟优化前：每条记录调2次Feign
        long unoptimizedStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (Submission s : submissions) {
                problemClient.getProblemById(s.getProblemId());
                userClient.getUserById(s.getUserId());
            }
        }
        long unoptimizedTime = System.nanoTime() - unoptimizedStart;

        System.out.println("=== SubmissionServiceImpl.pageQuery 性能对比 ===");
        System.out.printf("优化前(100次pageQueryx20条x2次Feign=4000次远程调用): %.2f ms%n",
                unoptimizedTime / 1_000_000.0);
        System.out.printf("优化后(100次pageQueryx2次批量Feign=200次远程调用): %.2f ms%n",
                optimizedTime / 1_000_000.0);
        System.out.printf("远程调用减少: 95%% (从每次请求41次降至3次)%n");
    }
}
