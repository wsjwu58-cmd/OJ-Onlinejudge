import request from '../utils/request'

/**
 * 上传PDF文件导入知识库
 * @param {File} file PDF文件
 * @param {string} category 知识分类
 * @returns {Promise}
 */
export const importPdfApi = (file, category = 'general') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  
  return request.post('/admin/knowledge/import/pdf', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量导入目录下的PDF文件
 * @param {string} path 目录路径
 * @param {string} category 知识分类
 * @returns {Promise}
 */
export const importDirectoryApi = (path, category = 'general') => {
  return request.post('/admin/knowledge/import/directory', null, {
    params: { path, category }
  })
}

/**
 * 清空知识库
 * @returns {Promise}
 */
export const clearKnowledgeBaseApi = () => {
  return request.delete('/admin/knowledge/clear')
}

export default {
  importPdfApi,
  importDirectoryApi,
  clearKnowledgeBaseApi
}
