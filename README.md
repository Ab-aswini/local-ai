# 🤖 Local AI — Hybrid Android AI Assistant

> A premium Android AI assistant that runs locally on your device using `llama.cpp` and automatically switches to cloud AI (Gemini) for complex tasks. Built for low-end devices (~4GB RAM, ~$100 phones).

---

## 📱 What the App Does Right Now

- **Chat Interface** — A dark, premium chat UI built with Jetpack Compose
- **Smart Routing** — Automatically decides whether to answer locally or use the cloud
- **Local Engine** — Uses `llama.cpp` with `mmap` for memory-efficient on-device AI
- **Cloud Engine** — Connects to Google Gemini 1.5 Flash API for complex questions
- **Visual Indicators** — 🟢 Green dot = running locally, 🔵 Blue dot = using cloud

---

## 🗺️ Project Roadmap

### ✅ Phase 1 — Foundation (DONE)
- [x] Premium dark UI with Jetpack Compose
- [x] `SystemHealthMonitor` — detects device RAM and picks the best inference strategy
- [x] `TaskOrchestrator` — routes simple queries locally, complex queries to cloud
- [x] `LlamaCppEngine` — local inference layer (stub, awaiting GGUF model)
- [x] `MediaPipeEngine` — Google's hardware-accelerated alternative (stub)
- [x] `OnlineApiClient` — real Gemini API integration
- [x] JNI bridge (`native-lib.cpp`) — C++ layer for llama.cpp
- [x] NDK + CMake build configuration
- [x] App builds and installs on Android phone ✅

### 🔄 Phase 2 — Real AI (IN PROGRESS)
- [ ] Add your **Gemini API key** to activate cloud responses
- [ ] Integrate real **llama.cpp source files** into `app/src/main/cpp/`
- [ ] Download a **GGUF model** (Llama 3.2 1B, ~800MB) and push to device
- [ ] Uncomment and test the real JNI calls in `LlamaCppEngine.kt`

### 🔮 Phase 3 — Polish
- [ ] App icon and splash screen
- [ ] Settings screen (API key input, model selection)
- [ ] Conversation history (persistent chat)
- [ ] Typing animation (…) while waiting for response
- [ ] Export / share chat
- [ ] Offline fallback message when no internet and no model loaded

---

## 🏗️ Architecture

```
Your Message
      │
      ▼
┌─────────────────────┐
│   TaskOrchestrator  │  ← Analyzes complexity
│  (Hybrid Router)    │
└──────────┬──────────┘
           │
    ┌──────┴───────┐
    │              │
    ▼              ▼
 Simple         Complex
    │              │
    ▼              ▼
┌────────┐   ┌──────────────┐
│ Local  │   │  Gemini API  │
│Engine  │   │  (Cloud)     │
│llama.  │   │              │
│cpp/    │   │ Free tier ✅ │
│mmap    │   │              │
└────────┘   └──────────────┘
    │              │
    └──────┬───────┘
           ▼
      Chat Screen
```

### Key Files

| File | Purpose |
|------|---------|
| `core/SystemHealthMonitor.kt` | Detects device RAM, picks inference strategy |
| `core/TaskOrchestrator.kt` | Routes queries to local or cloud engine |
| `local/LlamaCppEngine.kt` | Runs GGUF models via llama.cpp JNI |
| `local/MediaPipeEngine.kt` | Google MediaPipe alternative engine |
| `remote/OnlineApiClient.kt` | Gemini 1.5 Flash API client |
| `ui/chat/ChatScreen.kt` | The main chat screen UI |
| `cpp/native-lib.cpp` | C++ JNI bridge to llama.cpp |
| `cpp/CMakeLists.txt` | Native build configuration |

---

## 🚀 Quick Start

### Step 1 — Clone the repo
```bash
git clone https://github.com/Ab-aswini/local-ai.git
```

### Step 2 — Open in Android Studio
- File → Open → select the `local-ai` folder (NOT the `app` subfolder)
- Wait for Gradle sync to complete (~2 min)

### Step 3 — Run the app (works immediately with mock AI)
- Connect your Android phone via USB
- Enable USB Debugging + **Install via USB** in Developer Options
- Press the ▶️ Run button in Android Studio

### Step 4 — Activate real AI (optional but recommended)
1. Get a **free Gemini API key** at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
2. Open `app/src/main/java/com/example/hybridai/remote/OnlineApiClient.kt`
3. Replace `YOUR_GEMINI_API_KEY_HERE` with your key
4. Rebuild and run

---

## 🧠 How the Routing Works

The `TaskOrchestrator` classifies every message as **SIMPLE** or **COMPLEX**:

| Condition | Classification | Goes to |
|-----------|---------------|---------|
| Short message (< 50 chars) | SIMPLE | Local Engine |
| Contains: code, analyze, explain, write | COMPLEX | Gemini Cloud |
| Long message (> 100 chars) | COMPLEX | Gemini Cloud |
| Everything else | SIMPLE | Local Engine |

### RAM-Based Strategy (SystemHealthMonitor)

| Device RAM | Strategy |
|-----------|---------|
| < 6GB | `LOCAL_QUANTIZED_4BIT` — uses 4-bit GGUF model + mmap |
| 6–12GB | `LOCAL_QUANTIZED_4BIT` — balanced mode |
| > 12GB | `LOCAL_HIGH_PRECISION` — full precision model |

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin + Coroutines/Flow |
| Local AI | llama.cpp (C++ via JNI) |
| Cloud AI | Google Gemini 1.5 Flash API |
| Build | Gradle + CMake + NDK |
| Min Android | API 26 (Android 8.0) |

---

## 🤝 Contributing

This is a personal learning project. Feel free to fork and experiment!

---

*Built with ❤️ for low-end devices that deserve AI too.*
