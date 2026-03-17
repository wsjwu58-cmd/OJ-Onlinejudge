package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题组类型关联表
 */
@Data
public class GroupTypesRel {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题组ID，关联 problem_groups.id
     */
    private Integer groupId;

    /**
     * 类型ID，关联 problem_types.id
     */
    private Integer typeId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
