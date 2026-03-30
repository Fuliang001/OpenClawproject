import json
from typing import Annotated

from langchain_ollama import ChatOllama
from fastapi import Depends, APIRouter, Body
from fastapi.responses import HTMLResponse, JSONResponse

from deps import get_chatollama, get_agent
from services.agents import Agent
from services.ai_service import chat_ollama, chat_agent
from schemas import Message

router = APIRouter()

@router.get("/")
async def root() -> HTMLResponse:
    content = "<h1>Hello World</h1>"
    result = HTMLResponse(
        content=content,
    )
    return result



@router.post("/agent")
async def chat_with_agent(
    msg : Message,
    agent : Annotated[Agent, Depends(get_agent)]
) -> JSONResponse:
    agent_response = chat_agent(agent, msg.content)
    result = JSONResponse(content=str(agent_response))
    return result

@router.post("/analyze")

async def analyze_text(
    model : Annotated[ChatOllama, Depends(get_chatollama)],
    msg: Message,
) -> JSONResponse:
    response_text = chat_ollama(model, msg.content)
    if not response_text:
        response = JSONResponse(content={"error": "No response from model"})
        response.status_code = 500
        return response
    response = JSONResponse(response_text)
    return response
