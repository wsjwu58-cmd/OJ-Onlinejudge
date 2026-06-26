"""Agent工具统一导出 — 按节点分组"""
from src.agent.tools.solution import get_problem_detail
from src.agent.tools.judge import get_test_cases_for_judge
from src.agent.tools.learning import get_user_submission_stats, get_user_progress
from src.agent.tools.retrieval import search_knowledge

SOLUTION_TOOLS = [get_problem_detail]
CODE_TOOLS = [get_test_cases_for_judge]
LEARNING_TOOLS = [get_user_submission_stats, get_user_progress]
KNOWLEDGE_TOOLS = [search_knowledge]
