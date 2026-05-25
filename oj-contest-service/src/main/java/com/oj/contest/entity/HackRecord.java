package com.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hack_records")
public class HackRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer contestId;
    private Integer problemId;
    private Long hackerId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String hackInput;
    private String hackOutput;
    private String status;
    private String errorInfo;
    private String targetResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
