import operator
from typing import Annotated, List, TypedDict
from langchain_ollama import ChatOllama
from langchain.messages import AnyMessage, HumanMessage
# from langchain_core.messages import HumanMessage, AIMessage, BaseMessage

class MessagesState(TypedDict):
    messages: Annotated[list[AnyMessage], operator.add]
    llm_calls: int

def chat_ollama(model :ChatOllama, message: str) ->  str | list[str | dict]:
    ai_msg = model.invoke(message)
    return ai_msg.content


class AgentResponse(TypedDict):
    messages: List[AnyMessage]

def chat_agent(agent, message: str) -> str:
    input_data = {
    "messages": [
        HumanMessage(content=message)
    ]}
    agent.get_graph()
    response: dict = agent.invoke(input_data) # type: ignore[arg-type] 传dict就行了，直接绕过泛型检查
    result = response["messages"][1].content
    return result