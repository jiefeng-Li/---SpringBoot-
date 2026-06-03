import request from '@/utils/request'

export const getJobPositionList = (data) => {
  return request({
    method: 'get',
    url: '/job/list',
    params: data
  })
}

export const getRecommendedJobPositionList = (limit = 8) => {
  return request({
    method: 'get',
    url: '/job/recommendations',
    params: {
      limit,
    },
  })
}

export const getJobPositionById = (id) => {
  return request({
    method: 'get',
    url: '/job',
    params: {
      id
    }
  })
}


export const getAllJobTags = () => {
  return request({
    method: 'get',
    url: '/job/tags'
  })
}

export const addJobPosition = (data) => {
  return request({
    method: 'post',
    url: '/job',
    data
  })
}

export const updateJobPosition = (id, data) => {
  return request({
    method: 'put',
    url: `/job/${id}`,
    data
  })
}

export const updateJobPositionStatus = (id, status) => {
  return request({
    method: 'put',
    url: `/job/status/${id}`,
    params: {
      status
    }
  })
}

export const deleteJobPosition = (id) => {
  return request({
    method: 'delete',
    url: `/job/${id}`
  })
}

export const jobView = (id) => {
  return request({
    method: 'get',
    url: 'job/view',
    params: {
      jobId: id
    }
  })
}