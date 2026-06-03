import request from '@/utils/request'

export const getCompanyById = (id) => {
  return request({
    method: 'get',
    url: `/company/${id}`
  })
}

export const registerCompany = (formData) => {
  return request({
    method: 'post',
    url: '/company',
    data: formData
  })
}


export const getCompanyList = (params) => {
  return request({
    method: 'get',
    url: '/company/list',
    params
  })
}

export const adminGetCompanyList = (params) => {
  return request({
    method: 'get',
    url: '/company/admin/list',
    params
  })
}


export function updateCompany(id, data) {
  return request({
    url: `/company/${id}`,
    method: 'put',
    data
  });
}

export const getCompanyStatusList = () => {
  return request({
    method: 'get',
    url: '/company/status'
  })
}

export const getCompanyUserList = (params) => {
  return request({
    method: 'get',
    url: '/company/user/list',
    params
  })
}

export const updateCompanyStatus = (id, data) => {
  return request({
    method: 'put',
    url: `/company/${id}/status`,
    data
  })
}
