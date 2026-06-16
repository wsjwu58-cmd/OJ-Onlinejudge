"""知识检索模块 - 向量相似度搜索"""
from src.core.rag.store import get_vector_store


def retrieve_knowledge(query: str, context: str = "", top_k: int = 3) -> list[str]:
    """检索知识库，返回相关文本片段列表"""
    store = get_vector_store()
    if store is None:
        return []

    full_query = f"{query} {context}".strip()

    try:
        results = store.similarity_search_with_score(full_query, k=top_k)
        texts = []
        for doc, score in results:
            if score < 0.5:
                texts.append(doc.page_content)
        return texts[:top_k]
    except Exception:
        return []
