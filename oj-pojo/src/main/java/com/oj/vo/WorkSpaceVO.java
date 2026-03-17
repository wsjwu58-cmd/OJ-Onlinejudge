package com.oj.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WorkSpaceVO implements Serializable {
    /**
     * 活动VO类，用于前端显示最近活动
     */
        private static final long serialVersionUID = 1L;

        /**
         * 活动ID
         */
        private String id;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 活动类型
         * 例如：USER_LOGIN, PROBLEM_SUBMIT, GROUP_CREATE, PROBLEM_CREATE
         */
        private String  activityType;


        /**
         * 活动标题
         */
        private String title;

        /**
         * 活动描述
         */
        private String description;

        /**
         * 目标ID（如题目ID、题单ID等）
         */
        private Long targetId;

        /**
         * 目标类型（如problem、group等）
         */
        private String targetType;

        /**
         * 活动时间
         */
        private LocalDateTime createTime;

        /**
         * 活动状态（如成功、失败等）
         */
        private String status;

}
