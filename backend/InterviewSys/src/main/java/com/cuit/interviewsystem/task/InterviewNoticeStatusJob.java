package com.cuit.interviewsystem.task;

import com.cuit.interviewsystem.service.InterviewNoticeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 面试记录状态自动流转任务。
 */
@Slf4j
@Component
public class InterviewNoticeStatusJob {

    @Resource
    private InterviewNoticeService interviewNoticeService;

    /**
     * 每分钟扫描一次：将“已确认”且结束时间已到的面试置为“已结束”。
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void autoFinishExpiredInterviews() {
        int updated = interviewNoticeService.autoFinishExpiredAcceptedNotices();
        if (updated > 0) {
            log.info("自动更新 {} 个已结束面试记录", updated);
        }
    }
}
