"""管理端知识库管理接口"""
from fastapi import APIRouter, UploadFile, File, Form

from src.core.rag.importer import import_pdf, import_directory, clear_knowledge_base, get_knowledge_stats

router = APIRouter(prefix="/admin/knowledge", tags=["管理端-知识库"])


@router.post("/import/pdf")
async def import_pdf_endpoint(file: UploadFile = File(...), category: str = Form("")):
    """上传PDF导入知识库"""
    contents = await file.read()
    count = await import_pdf(contents, file.filename, category)
    return {"code": 200, "data": {"imported_count": count, "filename": file.filename}}


@router.post("/import/dir")
async def import_dir_endpoint(dir_path: str = Form(...), category: str = Form("")):
    """批量导入目录"""
    count = await import_directory(dir_path, category)
    return {"code": 200, "data": {"imported_count": count, "dir_path": dir_path}}


@router.delete("/clear")
async def clear_knowledge_endpoint():
    """清空知识库"""
    await clear_knowledge_base()
    return {"code": 200, "message": "知识库已清空"}


@router.get("/stats")
async def knowledge_stats():
    """获取知识库统计信息"""
    stats = await get_knowledge_stats()
    return {"code": 200, "data": stats}
