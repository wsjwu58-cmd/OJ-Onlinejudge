package com.oj.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 判题/提交完成后推送给管理端的 WebSocket 消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeNotifyMessage implements Serializable {

    /**
     * 固定类型标识，方便前端做路由
     */
    private String type;

    private Long submissionId;
    private Long userId;
    private Long problemId;

    /**
     * 例如：PENDING/RUNNING/ACCEPTED/WRONG_ANSWER/TIME_LIMIT/COMPILE_ERROR/SYSTEM_ERROR
     */
    private String status;

    private Integer runtimeMs;
    private Integer memoryKb;

    private Integer testCasesPassed;
    private Integer testCasesTotal;

    private String errorInfo;

    private LocalDateTime time;
}
