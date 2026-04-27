# RAGTool

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Purpose

`RAGTool` (`org.mobchain.tools.OwnTools`) implements **Retrieval-Augmented Generation** — it allows agents to query a local knowledge base (documents stored in the Alpine rootfs) and retrieve relevant context before generating a response.

---

## How RAG Works in MobAgent

1. Documents are indexed and stored in a vector store accessible from the Alpine Linux environment
2. When an agent needs factual information from documents, it calls the RAGTool with a query
3. RAGTool searches the vector store and returns the most relevant document chunks
4. The LLM uses this retrieved context to produce an informed answer — without hallucinating

---

## Interface

Like all tools, `RAGTool` implements the `Tool` interface:

```java
public class RAGTool implements Tool {
    String toolName = "rag_search";
    String skillName;

    @Override
    public JSONObject runTool(JSONObject args) {
        // args expected: { "query": "..." }
        // Returns: { "results": [ { "content": "...", "score": 0.95 }, ... ] }
    }
}
```

---

## Configuration

`RAGTool` is typically configured as part of a skill that needs document retrieval capability. The vector store backend runs inside the Alpine environment (e.g., using a lightweight embedding model).

---

## Comparison with TerminalTool

| Aspect | RAGTool | TerminalTool |
|---|---|---|
| Implementation | Java class | External binary |
| Discovery | Registered programmatically | Discovered via ToolsScanner |
| Primary use | Document retrieval | General-purpose commands |
| Result format | JSON with relevance scores | Any JSON |

---

## See Also

- [TerminalTool](./terminal-tool.md)
- [NativeTool & SpawnAgentTool](./native-tools.md)
- [Skills System](../skills-system/skills-system.md)

