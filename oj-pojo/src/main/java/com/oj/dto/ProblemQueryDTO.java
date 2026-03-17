package com.oj.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class ProblemQueryDTO extends PageQueryDTO implements Serializable {

    /*
    题目名称
     */
    private String title;
    /*
     题目难度
     */
    private String difficulty;
    /*
      题目状态
     */
    private Integer status;

    /*
     题目分类
     */
    private Integer problemTypeId;


}
