package com.oj.contest.dto;

import com.oj.common.dto.PageQueryDTO;
import lombok.Data;

@Data
public class ContestQueryDTO extends PageQueryDTO {
    private String status;
    private String title;
}
