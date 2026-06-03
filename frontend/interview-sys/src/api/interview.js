import request from '@/utils/request'

export const getInterviewById = (id) => {
    return request({
      url: '/interviewNotice',
      method: 'get',
      params: {
          noticeId: id
      }
    })
}

export const getInterviewNoticeList = (data) => {
    return request({
      url: '/interviewNotice/list',
      method: 'get',
      params: {
        ...data
      }
    })
}

export const getOwnInterviewNoticeList = (data) => {
    return request({
      url: '/interviewNotice/list/own',
      method: 'get',
      params: {
        ...data
      }
    })
}

export const addInterviewNotice = (data) => {
    return request({
      url: '/interviewNotice/add',
      method: 'post',
      data
    })
}

export const updateInterviewNotice = (data) => {
    return request({
      url: '/interviewNotice/update',
      method: 'put',
      data
    })
}

export const cancelInterviewNotice = (data) => {
    return request({
      url: '/interviewNotice/cancel',
      method: 'post',
      data
    })
}

export const respondInterviewNotice = (data) => {
    return request({
      url: '/interviewNotice/response',
      method: 'post',
      data
    })
}