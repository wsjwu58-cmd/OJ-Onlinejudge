package com.oj.problem.dto;

import com.oj.common.dto.PageQueryDTO;
import lombok.Data;

@Data
public class GroupQueryDTO extends PageQueryDTO {
    private String title;
}
