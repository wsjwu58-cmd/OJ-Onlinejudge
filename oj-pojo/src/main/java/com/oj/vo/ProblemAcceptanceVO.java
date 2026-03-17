package com.oj.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("problems")
public class ProblemAcceptanceVO {
    private Integer id;
    private String title;

    private Double acceptance;
}
