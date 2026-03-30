from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    base_url: str = "127.0.0.1"
    port: int = 8000
    model : str = "qwen3:0.6b"
    ollama_url: str = "http://localhost:11434"
    default_system_prompt: str = """"
    你是一个智能日程和待办提取助手。请分析以下文本，提取其中的“日程(SCHEDULE)”或“待办(TODO)”。
    如果没有明确的时间或任务，请返回空列表。
    请严格只返回标准的 JSON 格式，不要包含 markdown 标记（如 ```json),格式如下：
    [
    {{
        "type": "SCHEDULE" 或 "TODO",
        "title": "标题",
        "time": "YYYY-MM-DD HH:MM:SS (如果是相对时间请转换为绝对时间，参考当前日期)",
        "location": "地点 (可选)",
        "priority": "HIGH/MEDIUM/LOW (仅针对 TODO)"
    }}
    ]
    """

settings = Settings()