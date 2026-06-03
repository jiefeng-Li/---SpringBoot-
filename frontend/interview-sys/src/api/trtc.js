import request from '@/utils/request'

export const getTrtcSig = (noticeId) => {
  return request({
    method: 'get',
    url: '/trtc/sig',
    params: {
      noticeId
    }
  })
}