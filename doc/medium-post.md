# I'm 20 Years Old and I Built the First ReAct Agent Framework in Java, Specifically for Android — From Scratch, Alone

*By Sudharsan Ramasamy — solo developer, 20 years old.*

---

Let me say something that might sound bold, but I've done the research and I'll stand behind it:

**There is no other ReAct-style multi-agent framework built in Java, designed specifically for Android, with a fully working plugin system for memory, LLM formatters, tools, and skills — including sub-agent delegation — that runs entirely on-device.**

Not one. I looked.

Every agentic framework I found — LangChain, AutoGen, CrewAI, LlamaIndex Agents — is Python-first, server-first, or cloud-first. They assume you have a machine. They assume you have internet. They assume you're running a backend somewhere. Nobody built this for Android. Nobody built it in Java. Nobody built the whole stack — the agent loop, the plugin system, the sub-agent skills engine, the local LLM runtime, and the Linux execution sandbox — as a single cohesive Android app.

So I did.

I'm Sudharsan Ramasamy. I'm 20 years old. And this is the story of **MobAgent** — an Android-native ReAct multi-agent framework I built solo, in Java, that runs a full agentic AI pipeline entirely on your phone with no cloud, no root, and no compromises.

---

## Why This Matters

The AI world keeps talking about "local AI" and "on-device inference" — but almost everything stops at the model. Run a quantized LLaMA on your phone, chat with it, done. That's not an *agent*. That's a chatbot.

An agent **plans**. It **calls tools**. It **delegates subtasks** to specialized sub-agents. It **remembers** context across a session. It **acts** in the world — browsing the web, running code, writing files. None of that exists in the Android ecosystem. Until now.

MobAgent isn't just local inference. It's a complete agentic runtime — the same class of system as AutoGen or CrewAI — except it fits in your pocket, speaks to a local `llama.cpp` server, executes real Linux binaries through a proot Alpine sandbox, and lets you extend every layer with your own plugins via Android DEX loading.

I built every layer of this at 20, working alone. The agent loop. The tool executor. The skills system. The plugin loader. The local LLM integration. The terminal infrastructure. The chat session persistence. All of it.

---

*A solo developer's journey building MobAgent: a pluggable agentic AI platform, a local LLM runtime, and a real Linux terminal — all inside a single Android app.*

---

When most people think "AI running on a phone," they picture a chat bubble that sends your messages to some server farm in Virginia. What I wanted to build was something different: a genuine agentic AI that **reasons, calls tools, browses the web, executes code, and runs multi-step tasks** — entirely on-device, with zero cloud dependency. No API keys required. No data leaving your pocket.

That idea turned into **MobAgent**. This is the story of how I built it, the architectural decisions that shaped it, and why a few of them were genuinely hard.

---

## The Core Idea: Three Layers in One App

MobAgent isn't just one thing. It's three deeply integrated systems working together:

1. **A real Linux terminal** — Alpine Linux runs inside a proot sandbox (no root required), giving native binaries a place to live and execute.
2. **A local LLM runtime** — Pre-compiled ARM64 `llama.cpp` binaries start an OpenAI-compatible HTTP server (`llama-server`) pointed at any `.gguf` model you drop on your device.
3. **A pluggable multi-agent framework** — Built entirely in Java on top of the first two: a ReAct-style agent loop, a tool system, a skills system for sub-agent delegation, and a DEX-based plugin system for extending everything at runtime.

The terminal is not the point. It's the sandbox — the execution environment that lets the AI actually *do things*.

---

## The Agent Loop: How the AI Reasons and Acts

The heart of the framework is a **ReAct-style agent loop** (Reason + Act). If you're not familiar with ReAct, the idea is simple: the LLM doesn't just answer questions. It decides whether to call a tool, gets the tool's result, feeds that result back into its memory, and keeps reasoning until it has a final answer.

Here's how it flows:

```
User Message
     │
     ▼
LlmHandler       ← calls the LLM via HTTP
     │
     ├── No tool call? → final answer → done
     │
     └── Tool call detected?
              │
              ▼
         ToolExecutorHandler
              ├── looks up the tool in ToolsManager
              ├── runs the binary inside Alpine via proot
              ├── wraps result back into memory
              │
              └── back to LlmHandler → repeat
```

I implemented this as a **Chain of Responsibility**: `LlmHandler` and `ToolExecutorHandler` are linked handlers. They pass a `MsgContext` (a simple key-value bag) between them, and the chain keeps looping until the LLM stops asking for tools. Clean, extensible, and easy to add new handler types to.

---

## Tools: Real Executables, Not Python Functions

In most Python agentic frameworks, a "tool" is just a function decorated with `@tool`. In MobAgent, a tool is a **real native binary** — something compiled for ARM64, sitting inside the Alpine filesystem, executed by the agent through a shell session.

Each tool is a directory with two files:

```
tools/
└── web_search/
    ├── config.json     ← schema: name, description, input parameters
    └── web_search      ← ARM64 binary
```

At startup, `ToolsScanner` walks the tools directory, parses each `config.json`, and registers the tool with `ToolsManager`. When the agent calls `web_search`, `TerminalTool` runs the binary inside a synchronous proot session and waits for the output wrapped in `==Result==` markers.

This means any language that compiles to ARM64 Linux can be a tool. Python, Go, Rust, C — doesn't matter. The agent just sends JSON arguments, and the binary does the work.

---

## Skills: Sub-Agents with Specializations

A flat list of tools only gets you so far. For complex tasks — research, coding, data analysis — you want specialized agents with their own system prompts and private toolsets. That's what **Skills** are.

A skill is defined by a `skill.json`:

```json
{
  "name": "researcher",
  "overview": "Can research topics from the internet and summarize findings",
  "description": "You are an excellent researcher. You search the web, read articles, and produce clear summaries.",
  "privateTools": ["web_search", "summarizer"],
  "publicTools": ["file_writer"]
}
```

The root agent's system prompt is automatically extended with a list of available skills. When it decides to delegate — say, "this task needs research" — it calls `spawn_agent("researcher", task)`. A fresh sub-agent spins up with that skill's system prompt, its private tools, and a clean memory, runs its own full agent loop, and returns the result.

This is the part I'm most proud of. Proper sub-agent delegation inside a single Android process.

---

## Local LLM: llama.cpp, Fully On-Device

I didn't want to force users to rely on cloud API keys. So I embedded `llama.cpp` directly.

The app ships pre-compiled ARM64 `llama-server` binaries. When you drop a `.gguf` model file onto your device and tap "Start Server," MobAgent launches `llama-server` inside the Alpine sandbox on a configurable port. It exposes a standard OpenAI-compatible endpoint:

```
POST http://127.0.0.1:8080/v1/chat/completions
```

The agent framework speaks to it over localhost — same HTTP interface, same formatter, whether you're hitting a local llama-server, Ollama, or a remote OpenAI/Groq API. The model layer doesn't care.

No data leaves the device. No internet required. The AI runs in your pocket.

---

## The Plugin System: Extending the App Without Rebuilding It

This one was a genuinely interesting engineering challenge.

Android uses `.dex` bytecode, not standard JVM `.class` files. I wanted users to be able to add custom LLM formatters (for Claude, Gemini, whatever), custom memory backends, even custom model implementations — without me having to ship a new APK every time.

The solution: **runtime DEX loading**. Users upload `.jar` files containing `.dex` bytecode. `DexClassLoader` loads the class at runtime, casts it to a shared interface from `sharedToolInterface.jar` (which ships with the APK), and the framework uses it transparently.

```
Your plugin.jar (DEX)
    └── implements FormatterBuilder (from sharedToolInterface.jar)
            └── build() → FormatterInterface
                    └── used by agent to format requests
```

The key insight: the parent class loader of the plugin's `DexClassLoader` is the app's class loader. So both sides share the same `FormatterInterface` class object. The cast works. The interface contract is enforced.

I also added **built-in plugins** — an OpenAI formatter and an InMemory memory backend baked directly into the APK — so the app works immediately on a fresh install without any JAR uploads.

---

## The Part Nobody Talks About: Foreign Key Constraints and Sentinel IDs

Building this taught me one very practical lesson about Room + Android.

I had a clever idea: use `-1` as a sentinel ID for built-in plugins (since Room auto-generates positive PKs, `-1` can never collide). Store `-1` in the database to mean "built-in formatter, no DEX needed."

SQLite disagreed.

`ModelPlugin.formatter_id` has a `@ForeignKey` constraint pointing to `FormatterPlugin.plugin_id`. Storing `-1` triggers `FOREIGN KEY constraint failed (code 787)` because there's no row with `plugin_id = -1`.

The fix: **store `null` in the DB, resolve at runtime.** The sentinel ID lives only in memory. When the app reads `null` for `formatter_id`, it knows to use the built-in. No DB violation. No orphaned rows. Built-in plugins are a pure application-layer concept that the database never needs to know about.

Obvious in hindsight, but it took a crash at 11pm on a Friday to make it click.

---

## The Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────┐
│                         Android App                          │
│  ┌────────────┐  ┌──────────────────────────────────────┐   │
│  │  UI Layer  │  │         Core AI Framework             │   │
│  │ MainActivity│  │  ModelInterface (Builder pattern)    │   │
│  │ Settings   │  │  Memory  │  Tools  │  Skills          │   │
│  │ ChatSession│  │                                       │   │
│  └────────────┘  │     Agent Loop Engine                 │   │
│                  │  LlmHandler ⇄ ToolExecutorHandler      │   │
│  ┌────────────┐  └──────────────────────────────────────┘   │
│  │ Plugin Sys.│  ┌──────────────────────────────────────┐   │
│  │ DexLoader  │  │         Models Layer                  │   │
│  └────────────┘  │  OllamaModel / ChatModel              │   │
│                  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Terminal Infrastructure                     │   │
│  │  Alpine Linux proot · llama-server (ARM64) · SSH      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## What I Learned Building This Alone

**Scope creep is real.** What started as "a terminal app with an AI chat" turned into a multi-agent framework with a plugin system, a local LLM runtime, persistent chat sessions, and a skills engine. Each piece was individually justifiable. Collectively, it was a lot for one person.

**Android is a hostile environment for systems programming.** proot, DEX loading, pseudoterminals, ARM64 binary packaging, background thread management with Room + coroutines — none of this is what the Jetpack tutorials prepare you for. You have to go read kernel manuals and Termux source code.

**The right abstraction saves you weeks.** The `FormatterInterface` abstraction — where the formatter is responsible for both *building* the HTTP request JSON and *parsing* the response — means the entire agent loop is format-agnostic. Adding support for a new LLM API is one class, no changes to the loop.

**Build things that work first; make them elegant second.** The plugin system started as a hardcoded `if/else` for formatter types. It became a proper DexLoader only after I had the use case proven. Ship the idea, then refactor toward the right abstraction.

---

MobAgent is open source. If you're interested in local-first AI, Android systems programming, or building agentic frameworks from scratch — I'd love for you to take a look and contribute.

The goal was simple: **AI that belongs to you, running in your pocket, no strings attached.**

I think we're getting there.

---

*Built solo in Java for Android. Powered by llama.cpp, Alpine Linux, and proot.*

