package com.oj.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 判题链路性能监控工具
 * 用于测量和记录判题链路各环节的耗时
 */
@Slf4j
@Component
public class JudgePerformanceMonitor {

    private static final ThreadLocal<PerformanceContext> contextThreadLocal = new ThreadLocal<>();
    private static final Map<String, List<Long>> metricsCache = new ConcurrentHashMap<>();

    public static class PerformanceContext {
        private String submissionToken;
        private Map<String, Long> startTimes = new HashMap<>();
        private Map<String, Long> durations = new HashMap<>();

        public void recordStart(String phase) {
            startTimes.put(phase, System.currentTimeMillis());
        }

        public void recordEnd(String phase) {
            if (startTimes.containsKey(phase)) {
                long duration = System.currentTimeMillis() - startTimes.get(phase);
                durations.put(phase, duration);
                log.info("[性能监控] submissionToken={}, 环节={}, 耗时={}ms",
                        submissionToken, phase, duration);
            }
        }

        public void setSubmissionToken(String token) {
            this.submissionToken = token;
        }

        public Map<String, Long> getDurations() {
            return durations;
        }

        public void clear() {
            submissionToken = null;
            startTimes.clear();
            durations.clear();
        }
    }

    public static void start(String submissionToken, String phase) {
        PerformanceContext context = contextThreadLocal.get();
        if (context == null) {
            context = new PerformanceContext();
            context.setSubmissionToken(submissionToken);
            contextThreadLocal.set(context);
        }
        context.recordStart(phase);
    }

    public static void end(String phase) {
        PerformanceContext context = contextThreadLocal.get();
        if (context != null) {
            context.recordEnd(phase);
        }
    }

    public static void record(String submissionToken, String phase, long durationMs) {
        log.info("[性能监控] submissionToken={}, 环节={}, 耗时={}ms", submissionToken, phase, durationMs);

        String key = phase;
        metricsCache.computeIfAbsent(key, k -> new ArrayList<>()).add(durationMs);
    }

    public static Map<String, Long> getStatistics(String phase) {
        List<Long> durations = metricsCache.get(phase);
        if (durations == null || durations.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> stats = new HashMap<>();
        stats.put("count", (long) durations.size());

        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (Long duration : durations) {
            sum += duration;
            min = Math.min(min, duration);
            max = Math.max(max, duration);
        }

        stats.put("sum", sum);
        stats.put("avg", sum / durations.size());
        stats.put("min", min);
        stats.put("max", max);

        return stats;
    }

    public static void printAllStatistics() {
        log.info("========== 判题链路性能统计 ==========");
        for (String phase : metricsCache.keySet()) {
            Map<String, Long> stats = getStatistics(phase);
            log.info("环节: {}, 调用次数: {}, 平均耗时: {}ms, 最小: {}ms, 最大: {}ms",
                    phase,
                    stats.get("count"),
                    stats.get("avg"),
                    stats.get("min"),
                    stats.get("max"));
        }
        log.info("========================================");
    }

    public static void clearContext() {
        contextThreadLocal.remove();
    }

    public static void clearAllMetrics() {
        metricsCache.clear();
        log.info("性能监控数据已清空");
    }
}
