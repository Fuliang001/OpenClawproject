from typing import List, TypedDict
from pydantic import BaseModel


class Message(BaseModel):
    content: str

class OllamaResponse(BaseModel):
    content: str
