import request from '@/utils/request'

export const optimizeResumeModule = (data) => {
  return request({
    url: '/ai/resume/optimize',
    method: 'post',
    data,
    // AI 生成可能较慢，延长超时时间到 2 分钟
    timeout: 120000,
  })
}
