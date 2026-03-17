package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 比赛参赛表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("contest_participants")
public class ContestParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛ID */
    private Integer contestId;

    /** 用户ID */
    private Long userId;

    /** 比赛总得分 */
    private Integer score;

    /** 通过题数 */
    private Integer solvedCount;

    /** 报名时间 */
    private LocalDateTime registeredAt;
}
