package com.cuit.interviewsystem.model.dto.interview;


import com.cuit.interviewsystem.model.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class InterviewNoticeListDto extends PageDto {
    private Long companyId;
    private Long jobSeekerId;
    private Integer status;
    private Integer interviewType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
