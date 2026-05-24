package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("test_cases")
public class TestCase implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer problemId;
    private String inputData;
    private String outputData;
    private Boolean isSample = false;
    private Short orderNum = 0;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Double scoreWeight = 1.00;
    private Integer status = 1;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
