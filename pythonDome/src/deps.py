from fastapi import Request
from langchain_ollama import ChatOllama

from services.agents import Agent

def get_chatollama(request: Request) -> ChatOllama:
    return request.app.state.chatollama

def get_agent(request: Request) -> Agent:
    return request.app.state.agent