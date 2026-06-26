"""知识检索工具 - FAISS向量检索（@tool装饰器）"""
from langchain_core.tools import tool
from src.core.rag.retriever import retrieve_knowledge as _retrieve


@tool
def search_knowledge(query: str, top_k: int = 3) -> str:
    """搜索知识库，获取与查询相关的编程知识、算法、错误解决方案。当用户问概念性问题时应优先调用。"""
    results = _retrieve(query, "", top_k)
    if not results:
        return "未在知识库中找到相关知识"
    return "\n\n".join(f"【知识{i + 1}】\n{t}" for i, t in enumerate(results))
