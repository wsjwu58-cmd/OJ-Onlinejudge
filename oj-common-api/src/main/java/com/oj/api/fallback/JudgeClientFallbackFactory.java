package com.oj.api.fallback;

import com.oj.api.JudgeClient;
import com.oj.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
public class JudgeClientFallbackFactory implements FallbackFactory<JudgeClient> {
    @Override
    public JudgeClient create(Throwable cause) {
        log.error("判题服务调用失败: {}", cause.getMessage());
        return new JudgeClient() {
            @Override
            public Result<Long> countSubmissions(Map<String, Object> params) {
                return Result.error("判题服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Long> getUserSubmissionCount(Long userId) {
                return Result.error("判题服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countSubmissionsByDate(String begin, String end) {
                return Result.error("判题服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countSubmissionsByDateAndStatus(String begin, String end, String status) {
                return Result.error("判题服务调用失败: " + cause.getMessage());
            }
        };
    }
}
