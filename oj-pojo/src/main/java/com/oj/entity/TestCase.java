package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer problemId; // 关联的题目ID

    private String inputData;  // 输入数据
    private String outputData; // 期望输出数据

    private Boolean isSample = false; // 是否为示例用例
    private Short orderNum = 0;       // 顺序

    private Integer timeLimitMs; // 时间限制 (可选)
    private Integer memoryLimitMb; // 内存限制 (可选)

    private Double scoreWeight = 1.00; // 分数权重 (可选)

    private Integer status = 1; // 状态

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
