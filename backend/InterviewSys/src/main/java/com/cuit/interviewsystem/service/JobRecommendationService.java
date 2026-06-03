package com.cuit.interviewsystem.service;

import com.cuit.interviewsystem.model.entity.JobPosition;
import com.cuit.interviewsystem.model.vo.JobPositionVo;

import java.util.List;

public interface JobRecommendationService {
    List<JobPositionVo> recommendJobsForCurrentUser(Integer limit);

    void syncJobEmbedding(JobPosition jobPosition);

    void deleteJobEmbedding(Long jobId);
}