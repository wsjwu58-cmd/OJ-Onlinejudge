"""知识检索工具 - FAISS向量检索"""
from src.core.rag.retriever import retrieve_knowledge as _retrieve


def search_knowledge(query: str, context: str = "", top_k: int = 3) -> str:
    """搜索知识库"""
    results = _retrieve(query, context, top_k)
    if not results:
        return "未在知识库中找到相关知识"
    parts = [f"从知识库中检索到 {len(results)} 条相关知识：\n"]
    for i, text in enumerate(results, 1):
        parts.append(f"【知识{i}】\n{text}\n")
    return "\n".join(parts)


def search_problem_knowledge(problem_id: int, question: str) -> str:
    """检索与特定题目相关的知识"""
    results = _retrieve(question, f"题目ID: {problem_id}", 3)
    if not results:
        return f"未找到与题目 {problem_id} 相关的知识"
    parts = [f"题目 {problem_id} 的相关知识：\n"]
    for i, text in enumerate(results, 1):
        parts.append(f"{i}. {text}\n")
    return "\n".join(parts)
