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
@TableName("contest_participants")
public class ContestParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer contestId;
    private Long userId;
    private Integer score;
    private Integer solvedCount;
    private LocalDateTime registeredAt;
}
