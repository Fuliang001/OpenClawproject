from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from langchain_ollama import OllamaLLM
# from langchain_community.llms import Ollama
from langchain_core.prompts import PromptTemplate
# from langchain.prompts import PromptTemplate
import json

# 1. 初始化 FastAPI
app = FastAPI(title="AI Intelligence Service", version="1.0")


# 2. 定义接收的数据模型 (对应 Java 发送的 JSON)
class AnalysisRequest(BaseModel):
    text: str
    # 可以扩展更多字段，比如 file_path, user_id 等


# 3. 配置 LangChain + Ollama
# 确保你的 Ollama 正在运行，且已经 pull 了模型 (例如 qwen2.5 或 llama3)
# ollama_llm = Ollama(model="qwen2.5:7b", base_url="http://localhost:11434")
ollama_llm = OllamaLLM(model="qwen2.5:7b", base_url="http://localhost:11434")
# 定义 Prompt 模板 (这是核心逻辑，以后改这里就行，不用动 Java)
template = """
你是一个智能日程和待办提取助手。请分析以下文本，提取其中的“日程(SCHEDULE)”或“待办(TODO)”。
如果没有明确的时间或任务，请返回空列表。

请严格只返回标准的 JSON 格式，不要包含 markdown 标记（如 ```json），格式如下：
[
    {{
        "type": "SCHEDULE" 或 "TODO",
        "title": "标题",
        "time": "YYYY-MM-DD HH:MM:SS (如果是相对时间请转换为绝对时间，参考当前日期)",
        "location": "地点 (可选)",
        "priority": "HIGH/MEDIUM/LOW (仅针对 TODO)"
    }}
]

当前参考日期：2026-03-24
用户输入文本：{text}
"""

prompt = PromptTemplate(input_variables=["text"], template=template)


@app.post("/analyze")
async def analyze_text(request: AnalysisRequest):
    try:
        # 构造完整的 Prompt
        full_prompt = prompt.format(text=request.text)

        # 调用 Ollama (通过 LangChain)
        # stream=False 表示等待完整结果返回
        response_text = ollama_llm.invoke(full_prompt)

        # 清洗数据 (防止模型偶尔输出 ```json ... ```)
        clean_json = response_text.strip()
        if clean_json.startswith("```json"):
            clean_json = clean_json[7:]
        if clean_json.endswith("```"):
            clean_json = clean_json[:-3]
        clean_json = clean_json.strip()

        # 尝试解析为 JSON 对象列表
        result_data = json.loads(clean_json)

        return {
            "success": True,
            "data": result_data,
            "raw_response": response_text  # 调试用
        }

    except Exception as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # 启动服务，监听 8000 端口
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)