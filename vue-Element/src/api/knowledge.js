import request from '../util/request'

export const importPdfApi = (file, category) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  return request.post('/admin/knowledge/import/pdf', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const importDirectoryApi = (path, category) => {
  return request.post('/admin/knowledge/import/directory', null, {
    params: { path, category }
  })
}

export const clearKnowledgeBaseApi = () => {
  return request.delete('/admin/knowledge/clear')
}

export default {
  importPdfApi,
  importDirectoryApi,
  clearKnowledgeBaseApi
}
