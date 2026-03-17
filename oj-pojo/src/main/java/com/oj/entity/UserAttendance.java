package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户签到表
 */
@Data
public class UserAttendance {
    /**
     * 签到ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 签到日期
     */
    private LocalDate date;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
