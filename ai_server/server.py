"""
Kinetic AI Trainer — Python Backend Server
Uses raw HTTP to Groq API — no SDK dependency issues.

Setup:
  pip install fastapi uvicorn requests pydantic

Run:
  uvicorn server:app --host 0.0.0.0 --port 8000

Environment variables:
  PROVIDER=groq
  GROQ_API_KEY=gsk_...
  MODEL=llama-3.3-70b-versatile
"""

import os
import requests
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List

app = FastAPI(title="Kinetic AI Trainer")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
MODEL = os.getenv("MODEL", "llama-3.3-70b-versatile")
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
PROVIDER = os.getenv("PROVIDER", "groq")


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str
    system_prompt: str
    history: List[ChatMessage] = []


class ChatResponse(BaseModel):
    reply: str


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    try:
        if PROVIDER == "groq":
            reply = chat_groq(req.message, req.system_prompt, req.history)
        else:
            reply = chat_ollama(req.message, req.system_prompt, req.history)
    except Exception as e:
        reply = f"Error: {str(e)}"
    return ChatResponse(reply=reply)


def chat_groq(message: str, system_prompt: str, history: List[ChatMessage]) -> str:
    messages = [{"role": "system", "content": system_prompt}]
    recent = history[-20:] if len(history) > 20 else history
    for msg in recent:
        role = "assistant" if msg.role in ("ai", "assistant", "model") else "user"
        messages.append({"role": role, "content": msg.content})
    messages.append({"role": "user", "content": message})

    resp = requests.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json",
        },
        json={
            "model": MODEL,
            "messages": messages,
            "max_tokens": 512,
            "temperature": 0.7,
        },
        timeout=60,
    )
    resp.raise_for_status()
    return resp.json()["choices"][0]["message"]["content"]


def chat_ollama(message: str, system_prompt: str, history: List[ChatMessage]) -> str:
    messages = [{"role": "system", "content": system_prompt}]
    for msg in history:
        messages.append({"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": message})

    resp = requests.post(
        f"{OLLAMA_HOST}/api/chat",
        json={"model": MODEL, "messages": messages, "stream": False},
        timeout=60,
    )
    resp.raise_for_status()
    return resp.json()["message"]["content"]


@app.get("/health")
def health():
    return {"status": "ok", "provider": PROVIDER, "model": MODEL}
