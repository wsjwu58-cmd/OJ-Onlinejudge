package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupTypesRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer groupId;
    private Integer typeId;
    private LocalDateTime createdAt;
}
