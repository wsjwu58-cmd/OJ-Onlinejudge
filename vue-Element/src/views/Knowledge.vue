<template>
  <div class="knowledge-container">
    <div class="page-header">
      <h2>知识库管理</h2>
      <p class="subtitle">导入PDF文档到向量数据库，用于AI智能问答</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <span>上传PDF文件</span>
              <el-tag type="info" size="small">单个文件</el-tag>
            </div>
          </template>
          
          <el-form :model="uploadForm" label-width="80px">
            <el-form-item label="知识分类">
              <el-select v-model="uploadForm.category" placeholder="选择分类" style="width: 100%">
                <el-option label="算法" value="algorithm" />
                <el-option label="数据结构" value="data-structure" />
                <el-option label="编程语言" value="language" />
                <el-option label="错误处理" value="error" />
                <el-option label="最佳实践" value="best-practice" />
                <el-option label="通用知识" value="general" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="PDF文件">
              <el-upload
                ref="uploadRef"
                class="pdf-uploader"
                drag
                :auto-upload="false"
                :limit="1"
                accept=".pdf"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                :file-list="fileList"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  拖拽PDF文件到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">只能上传 PDF 文件</div>
                </template>
              </el-upload>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                :loading="uploading" 
                :disabled="!selectedFile"
                @click="handleUpload"
              >
                <el-icon><Upload /></el-icon>
                开始导入
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="batch-card">
          <template #header>
            <div class="card-header">
              <span>批量导入目录</span>
              <el-tag type="warning" size="small">服务器路径</el-tag>
            </div>
          </template>
          
          <el-form :model="batchForm" label-width="80px">
            <el-form-item label="目录路径">
              <el-input 
                v-model="batchForm.directoryPath" 
                placeholder="例如: D:/knowledge/pdfs/"
              >
                <template #prepend>
                  <el-icon><Folder /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="知识分类">
              <el-select v-model="batchForm.category" placeholder="选择分类" style="width: 100%">
                <el-option label="算法" value="algorithm" />
                <el-option label="数据结构" value="data-structure" />
                <el-option label="编程语言" value="language" />
                <el-option label="错误处理" value="error" />
                <el-option label="最佳实践" value="best-practice" />
                <el-option label="通用知识" value="general" />
              </el-select>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="warning" 
                :loading="batchImporting"
                :disabled="!batchForm.directoryPath"
                @click="handleBatchImport"
              >
                <el-icon><FolderAdd /></el-icon>
                批量导入
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="result-card" v-if="importResult">
      <template #header>
        <div class="card-header">
          <span>导入结果</span>
          <el-tag :type="importResult.success ? 'success' : 'danger'" size="small">
            {{ importResult.success ? '成功' : '失败' }}
          </el-tag>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件/目录">
          {{ importResult.filename || importResult.directory }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          {{ importResult.category }}
        </el-descriptions-item>
        <el-descriptions-item label="文档片段数">
          <el-tag type="success">{{ importResult.documentCount || importResult.totalDocumentCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消息">
          {{ importResult.message }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="danger-card">
      <template #header>
        <div class="card-header">
          <span class="danger-title">危险操作</span>
        </div>
      </template>
      
      <el-alert
        title="清空知识库"
        type="warning"
        description="此操作将清空向量数据库中的所有知识，不可恢复！请谨慎操作。"
        show-icon
        :closable="false"
      />
      
      <el-button 
        type="danger" 
        class="clear-btn"
        :loading="clearing"
        @click="handleClear"
      >
        <el-icon><Delete /></el-icon>
        清空知识库
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  UploadFilled, 
  Upload, 
  Folder, 
  FolderAdd, 
  Delete 
} from '@element-plus/icons-vue'
import { importPdfApi, importDirectoryApi, clearKnowledgeBaseApi } from '../api/knowledge'

const uploadRef = ref(null)
const selectedFile = ref(null)
const fileList = ref([])
const uploading = ref(false)
const batchImporting = ref(false)
const clearing = ref(false)
const importResult = ref(null)

const uploadForm = reactive({
  category: 'general'
})

const batchForm = reactive({
  directoryPath: '',
  category: 'general'
})

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择PDF文件')
    return
  }
  
  uploading.value = true
  importResult.value = null
  
  try {
    const res = await importPdfApi(selectedFile.value, uploadForm.category)
    importResult.value = {
      success: true,
      ...res.data
    }
    ElMessage.success('PDF导入成功！')
    fileList.value = []
    selectedFile.value = null
  } catch (err) {
    importResult.value = {
      success: false,
      message: err.message || '导入失败'
    }
    ElMessage.error('导入失败: ' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

const handleBatchImport = async () => {
  if (!batchForm.directoryPath) {
    ElMessage.warning('请输入目录路径')
    return
  }
  
  batchImporting.value = true
  importResult.value = null
  
  try {
    const res = await importDirectoryApi(batchForm.directoryPath, batchForm.category)
    importResult.value = {
      success: true,
      ...res.data
    }
    ElMessage.success('批量导入成功！')
  } catch (err) {
    importResult.value = {
      success: false,
      message: err.message || '批量导入失败'
    }
    ElMessage.error('批量导入失败: ' + (err.message || '未知错误'))
  } finally {
    batchImporting.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空知识库吗？此操作不可恢复！',
      '危险操作',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    clearing.value = true
    
    await clearKnowledgeBaseApi()
    ElMessage.success('知识库已清空')
    importResult.value = null
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('清空失败: ' + (err.message || '未知错误'))
    }
  } finally {
    clearing.value = false
  }
}
</script>

<style scoped>
.knowledge-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.upload-card,
.batch-card,
.result-card,
.danger-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pdf-uploader {
  width: 100%;
}

.pdf-uploader :deep(.el-upload-dragger) {
  width: 100%;
}

.danger-title {
  color: #f56c6c;
}

.clear-btn {
  margin-top: 16px;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style>
