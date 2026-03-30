from langchain.agents import create_agent

from langchain.tools import tool

@tool
def get_weather(location: str) -> str:
    """Get weather information for a location."""
    return f"Weather in {location}: Sunny, 72°F"

basic_tools = [get_weather]

class Agent:
    def __init__(self, model, 
        system_prompt,
        tools=basic_tools, 
    ):
        self.agent = create_agent(
        model, 
        tools, 
        system_prompt=system_prompt)
