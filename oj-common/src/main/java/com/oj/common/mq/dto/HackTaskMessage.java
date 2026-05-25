package com.oj.common.mq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HackTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long hackId;
    private Integer contestId;
    private Integer problemId;
    private Long hackerId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String targetLanguage;
    private String validatorPath;
    private String validatorExePath;
    private String validatorSrcHash;
    private String referencePath;
    private String referenceLanguage;
    private String hackInput;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
}
