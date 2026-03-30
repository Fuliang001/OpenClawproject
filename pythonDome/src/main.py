from fastapi import FastAPI
from contextlib import asynccontextmanager
import uvicorn
from langchain_ollama import ChatOllama

from config import settings
from router import router
from services.agents import Agent
from langchain.agents import create_agent


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    chatollama = ChatOllama(
        model=settings.model, 
        base_url=settings.ollama_url
    )
    app.state.chatollama = chatollama
    agent = create_agent(chatollama)
    app.state.agent = agent
    
    yield
    
    # Shutdown (如有需要清理的资源可在此处理)

app = FastAPI(title="AI Intelligence Service", version="1.0", lifespan=lifespan)


app.include_router(router)

if __name__ == "__main__":
    uvicorn.run(app, host=settings.base_url, port=settings.port)
