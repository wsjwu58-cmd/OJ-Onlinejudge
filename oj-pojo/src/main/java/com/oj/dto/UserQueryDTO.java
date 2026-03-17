package com.oj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
@Data
public class UserQueryDTO implements Serializable {

    @Schema(description = "页码")
    private int page;
    @Schema(description = "每页数量")
    private int pageSize;
    @Schema(description = "排序字段")
    private String orderBy;
    @Schema(description = "是否升序")
    private Boolean order;
    private String username;

    private String role;
}
