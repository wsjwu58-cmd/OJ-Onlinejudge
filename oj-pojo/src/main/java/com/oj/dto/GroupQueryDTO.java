package com.oj.dto;

import lombok.Data;

@Data
public class GroupQueryDTO extends PageQueryDTO{
    /**
     * 题组标题
     */
    private String title;

}
