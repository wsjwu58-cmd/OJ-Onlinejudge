"""知识导入模块 - PDF解析、文本分块、向量化入库"""
import os
import fitz  # pymupdf
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from src.core.rag.store import get_vector_store, create_vector_store, persist_vector_store


async def import_pdf(file_bytes: bytes, filename: str, category: str = "") -> int:
    """导入单个PDF文件到知识库，返回导入文档数"""
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    full_text = ""
    for page in doc:
        full_text += page.get_text()
    doc.close()

    if not full_text.strip():
        return 0

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=350, chunk_overlap=50,
        separators=["\n\n", "\n", "。", ".", " ", ""],
    )
    chunks = splitter.split_text(full_text)

    documents = [
        Document(
            page_content=chunk,
            metadata={"source": filename, "category": category},
        )
        for chunk in chunks
    ]

    existing_store = get_vector_store()
    if existing_store:
        existing_store.add_documents(documents)
    else:
        create_vector_store(documents)

    await persist_vector_store()
    return len(documents)


async def import_directory(dir_path: str, category: str = "") -> int:
    """批量导入目录中的PDF和TXT文件"""
    total = 0
    for root, _, files in os.walk(dir_path):
        for f in files:
            filepath = os.path.join(root, f)
            try:
                if f.lower().endswith(".pdf"):
                    with open(filepath, "rb") as fp:
                        count = await import_pdf(fp.read(), f, category)
                    total += count
                elif f.lower().endswith(".txt"):
                    loader = TextLoader(filepath, encoding="utf-8")
                    docs = loader.load()
                    splitter = RecursiveCharacterTextSplitter(
                        chunk_size=350, chunk_overlap=50,
                    )
                    split_docs = splitter.split_documents(docs)
                    for d in split_docs:
                        d.metadata["source"] = f
                        d.metadata["category"] = category
                    existing_store = get_vector_store()
                    if existing_store:
                        existing_store.add_documents(split_docs)
                    else:
                        create_vector_store(split_docs)
                    total += len(split_docs)
            except Exception:
                continue
    if total > 0:
        await persist_vector_store()
    return total


async def clear_knowledge_base():
    """清空向量库"""
    from src.core.rag.store import set_vector_store
    set_vector_store(None)
    from src.deps import get_redis
    r = await get_redis()
    await r.delete("ai:vector:snapshot")


async def get_knowledge_stats() -> dict:
    """获取知识库统计信息"""
    store = get_vector_store()
    if store is None:
        return {"total_documents": 0, "has_index": False}
    return {
        "total_documents": store.index.ntotal if store.index else 0,
        "has_index": True,
    }
