import request from '@/utils/request'

export const getResumeById = (id) => {
  return request({
    url: `/resume/${id}`,
    method: 'get'
  })
}

export const getResumesPageByUserId = (params) => {
  return request({
    url: '/resume/list',
    method: 'get',
    params
  })
}

export const addResume = (resume, avatarFile) => {
  return request({
    url: '/resume',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: buildResumeMultipartData(resume, avatarFile)
  })
}

export const updateResume = (id, resume, avatarFile) => {
    return request({
      url: `/resume/${id}`,
      method: 'put',
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      data: buildResumeMultipartData(resume, avatarFile)
    })
}

const buildResumeMultipartData = (resume, avatarFile) => {
  const formData = new FormData()

  // DTO 作为 JSON part 发送，便于后端按对象接收
  formData.append(
    'resume',
    new Blob([JSON.stringify(resume)], { type: 'application/json' })
  )

  if (avatarFile) {
    formData.append('avatar', avatarFile)
  }

  return formData
}

export const deleteResume = (id) => {
    return request({
      url: `/resume/${id}`,
      method: 'delete'
    })
}

export const setDefaultResume = (id) => {
    return request({
      url: `/resume/default/${id}`,
      method: 'put'
    })
}

export const getResumePreview = (id) => {
    return request({
      url: `/resume/preview/${id}`,
      method: 'get'
    })
}


export const previewUnsavedResume = (data, templateId) => {
    return request({
      url: '/resume/preview',
      method: 'post',
      data: {
        ...data,
      },
      params: {
        templateId
      }
    })
}

export const getResumeDownload = (id, templateId) => {
    return request({
      url: `/resume/export/${id}`,
      method: 'get',
      responseType: 'blob',
      params: {
        templateId
      }
    })
}

export const getResumeTemplates = () => {
  return request({
    url: '/resume/templates',
    method: 'get'
  })
}

export const previewResume = (id, templateId) => {
  return request({
    url: `/resume/preview/${id}`,
    method: 'get',
    params: {
      templateId
    }
  })
}