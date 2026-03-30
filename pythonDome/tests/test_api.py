import json
import sys
from pathlib import Path

import requests
from unittest import TestCase

# 添加 src 目录到 Python 路径
project_root = Path(__file__).parent.parent
src_path = project_root / "src"
sys.path.insert(0, str(src_path))

from config import Settings

BASE_URL = Settings.base_url

class TestAPI(TestCase):
    def test_get_posts(self):
        response = requests.get(f"{BASE_URL}")
        assert response.status_code == 200

    def test_ollama_post(self):
        request_data = {"content": "Hello world!"}
        response = requests.post(
            f"{BASE_URL}/analyze",
            json=request_data
            )
        print(response.text)

    def test_agent_post(self):
        request_data = {"content": "Hello World!"}
        response = requests.post(
            f"{BASE_URL}/agent",
            json=request_data
        )
        print(f"status_code = {response.status_code}")
        print(response.text)
        self.assertEqual(response.status_code, 200)

if __name__ == "__main__":
    test = TestAPI()
    # test.test_ollama_post()
    test.test_agent_post()