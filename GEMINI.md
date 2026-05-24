# CoreAgent 專案記憶檔 (GEMINI.md)

## 【開發憲法：絕對禁止事項】
1. **嚴禁虛假代碼：** 嚴禁使用 `return true`、`println` 偽裝、`// TODO` 標籤來規避實作。
2. **生產級異常：** 若該功能需依賴無法本地實作的環境，必須拋出明確的 `NotImplementedError` 或 `UnsupportedOperationException`。
3. **強制審計：** 在進行任何編輯前，必須搜索代碼庫中的虛假標記，若發現必須先清理。
4. **完整執行性：** 所有功能必須提供 100% 完整、經過測試且具備正確錯誤處理的生產級程式碼。任何涉及外部進程調用的邏輯，必須嚴格檢查進程退出代碼 (`exitValue() == 0`) 並捕獲錯誤流。

---

## 專案概述
CoreAgent 是一個 Android 多智能體 AI Agent 應用程式，旨在實現基於「多任務工作空間」與「中央記憶庫狀態機」的調度架構。

## 模組實作狀態清單
| 模組名稱 | 狀態 | 說明 |
| :--- | :--- | :--- |
| **WorkspaceManager** | [x] 已實作 | 完成空間 ID 生成與物理目錄隔離。 |
| **MemoryBank** | [x] 已實作 | 完成狀態機定義與持久化 (`memory.dat`)。 |
| **TaskScheduler** | [x] 已實作 | 完成任務指令拆解與佇列邏輯。 |
| **ToolManager** | [x] 已實作 | 完成基礎架構與安全校驗邏輯。 |
| **BillingManager** | [x] 已實作 | 完成權限驗證與計費攔截介面。 |
| **SecretManager** | [x] 已實作 | 完成 Android EncryptedSharedPreferences 實作。 |
| **AgentRouter** | [x] 已實作 | 完成 Critic/Refiner 迭代邏輯。 |
| **ExecutionEngine** | [x] 已實作 | 完成生產級沙盒：具備進程級隔離、執行逾時限制與資源管控。 |

| **AgentService** | [x] 已實作 | 完成全鏈路執行迴圈整合。 |
| **SettingsManager** | [x] 已實作 | 完成 DataStore 配置持久化。 |
| **LLMClient** | [x] 已實作 | 完成 OpenAI/Gemini 真實網路請求框架。 |
| **Dashboard UI** | [x] 已實作 | 完成基礎佈局與新建互動。 |
| **Settings UI** | [x] 已實作 | 完成配置修改與持久化互動。 |

## 關鍵缺失功能 (待實作)
| 功能模組 | 優先級 | 說明 |
| :--- | :--- | :--- |
| **C/S 雲端同步層** | [x] 已實作 | 完成基於 Ktor WebSocket 的實時同步協議框架。 |
| **插件倉庫介面** | [x] 已實作 | 實作 JSON Schema 解析、檔案完整性驗證機制。 |

| **斷點續傳恢復器** | [x] 已實作 | 實作 ResilienceManager 自動恢復未完成任務。 |
| **會話隔離器** | [x] 已實作 | 實作 SessionIsolator 確保 Prompt 絕對純淨。 |

| **來源標籤系統** | [x] 已實作 | 完成 Citation 模型與 UI Chip 組件開發。 |
| **IAP/支付驗證** | [x] 已實作 | 擴充介面並建立 Google Play 與 Webhook 驗證框架。 |

| **執行上下文注入器** | [x] 已實作 | 實作 ContextBuilder，實現精確的 LLM Prompt 注入。 |


## 下一步工作建議
1. **執行上下文注入器**：這是 AI Agent 運作的關鍵，需先完善 Context Builder，確保 LLM 接收到正確資訊。
2. **插件倉庫介面**：完善 `ToolManager` 以支援遠端 JSON Schema 解析，這是實現動態工具繫結的基礎。
3. **C/S 同步協議**：規劃網絡請求與多租戶身份驗證流程。

---
*本文件為 CoreAgent 開發過程中的核心記憶與狀態清單，請在每次重啟會話後查閱。*
