package com.oj.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserAttendance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate date;

    private LocalDateTime createdAt;
}
