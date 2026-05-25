# CoreAgent
# 提示詞：移動端多智能體 AI Agent APP 移動端開發藍圖

## 1. 專案目標 (Project Objective)
請為我開發一個標準的 Android APK 項目。核心功能是實作一個基於「多任務工作空間（Multi-Workspace）」與「中央記憶庫狀態機（Memory-Bank State Machine）」調度的多智能體架構。App 採用完全本地優先（Local-First）架構，允許用戶自定義配置、動態下載外部擴展工具鏈，在完全隔離的本地 Session 環境中執行複雜任務拆解與雙重校驗，確保商用級別的準確性、極致的數據隱私與零伺服器成本。

## 2. 技術棧與基準架構 (Technical Stack)
* **平台環境：** Android (Target SDK 34/35)，使用 Kotlin 編寫底層核心，UI 採用 Jetpack Compose 實作多頁面導航（Navigation）。
* **核心架構：** 本地優先（Local-First）獨立運行架構。手機端負責 UI 渲染、狀態調度、API Key 加密管理與本地代碼沙盒執行。
* **數據持久層：** 採用 Android Room (SQLite) 本地資料庫進行多租戶工作空間隔離（Multi-Workspace Local DB Isolation），嚴禁依賴外部雲端資料庫，確保無網環境下依然能正常讀取歷史紀錄。

---

## 3. 分步實作步驟與核心邏輯 (Implementation Details)

### 第一步：多任務空間（Multi-Workspace）與本地 Room 記憶庫初始化
1. **工作空間 Room 隔離：** 用戶新建任務時，系統分發唯一的 `Workspace_ID`。本地 Room 資料庫利用外鍵（Foreign Key）關聯，對每個空間的聊天記錄（Chat History）、任務步驟（Task Steps）、記憶狀態與生成資產進行物理表級別的邏輯隔離。
2. **中央記憶庫（Memory Bank）：** 每個空間在 Room 中維護其結構化狀態，包含 `GlobalContext`（全局專案宣告與目標摘要）及 `TaskQueue`（本地任務步驟隊列）。每次狀態變更必須通過 Room 進行原子寫入（Atomic Write），防範斷電損壞。

### 第二步：動態任務拆解與外掛工具安全校驗 (Dynamic Tool Binding)
1. **任務拆解與欄位補全：** 接收用戶指令後，調度器將其線性拆解為多個子步驟（Steps）存入 Room 的 `TaskQueue` 表。每個步驟必須包含：`step_id`、`task_description`、`assigned_agent`（指定負責的智能體角色）、`status`（PENDING/RUNNING/COMPLETED/FAILED）及 `required_tools`（該步驟所需工具清單）。
2. **外掛安全下載與雙重校驗：** 連接遠端工具群組倉庫（Plugin Repository URL）。App 執行步驟前檢查本地已安裝工具。若工具缺失，中斷 Pipeline 並觸發 UI 提示用戶下載。下載工具包（含 JSON Schema 與 JS 執行腳本）時，必須同時觸發 SHA-256 雜湊值與數位簽章（Digital Signature）雙重校驗，防範惡意代碼注入本地沙盒。

### 第三步：跨越上下文限制的「會話隔離與無衝突執行」 (Session Isolation)
為了繞過長對話導致的邏輯衰退與 Token 費用爆炸：
1. **依賴型上下文注入：** 啟動子步驟時，App 為大模型強制建立完全乾淨、隔離的新會話（New Session）。僅注入：`GlobalContext` + `當前步驟描述` + `具備直接依賴關係的前置步驟生產物` + `已驗證工具的 Function Calling (JSON Schema) 宣告`。嚴禁帶入歷史對話廢話。
2. **合規本地代碼執行器 (Compliant Tool Runner)：** 當 Agent 觸發工具調用時，必須支援真實的 Function Calling 互動（而非純文字比對）。本地沙盒必須採用符合 Android 安全規範的內置解釋器（如 Duktape JS 引擎 或純 JVM 環境下的解析內核）在本地沙盒環境執行腳本，徹底規避 Android 10+ 的 W^X 執行限制。
3. **狀態機雙重校驗與角色分工：** 每個子步驟執行完畢後，由 審查智能體（Critic Agent） 進行本地自我反思核對（上限 3-5 次）。驗證通過後，將精確成品寫入 Room 的 `produced_artifacts`，變更步驟狀態為 `COMPLETED`。全案所有步驟完成後，由 潤色智能體（Refiner Agent） 整合所有步驟成品，輸出最終全局報告。

### 第四步：本地斷點續傳與回復 (Resilience & Recovery)
* 網絡崩潰或大模型 API 異常時，Room 資料庫完整保留當前停滯步驟的上下文明細。App 不得崩潰，將該步驟狀態置為 `FAILED`。支援用戶在 UI 上點擊「重試」按鈕，直接從出錯的步驟讀取 Room 記憶庫並新開會話續傳執行。

---

## 4. UI/UX 介面設計規範 (Jetpack Compose Multi-Screen)
利用 Navigation 組件實作以下畫面流暢切換，將後台狀態高度可視化：

* **畫面一：多專案工作空間儀表板 (Dashboard Screen)**
  * 列表展示所有歷史 `Workspace_ID` 項目。頂部設置「+ 新建任務」按鈕，點擊彈出命名框，確認後開闢全新乾淨對話與獨立本地 Room 記憶庫。
* **畫面二：主聊天與狀態看板畫面 (Chat & Workspace Screen)**
  * **對話歷史 UI 流：** 完美渲染用戶與 Agent 的對話氣泡，支援 Citations System（將工具調用來源、腳本執行結果渲染為乾淨的「來源標籤卡片 Source Chips」）。
  * **記憶庫進度看板：** 頂部配備可摺疊的 「記憶庫進度看板」組件 (Memory Bank Accordion Widget)，實時綁定本地 Room 的 `TaskQueue` 狀態並以動畫展示 `assigned_agent` 的流水線進度。
  * **工具攔截彈窗：** 檢測到步驟工具缺失時，自動彈出 底部 Sheet 攔截視窗 (Bottom Sheet)，顯示工具詳情並引導用戶一鍵點擊下載。
* **畫面三：外掛工具市場 (Tool Marketplace Screen)**
  * 網格展示雲端倉庫提供的工具外掛，具備一鍵下載進度條與「已激活/未安裝/有更新」的狀態標籤。
* **畫面四：高度擴展配置設定頁面 (Settings Screen)**
  * 提供以下模組化配置項：
    1. *API Key 本地安全加密管理：* 供用戶輸入多廠商（OpenAI/Gemini/Anthropic）API Key。必須使用 `EncryptedSharedPreferences` 或帶有 SQLCipher 加密的 Room 進行本地底層加密儲存，嚴禁明文留存。
    2. *大模型專家路由映射：* 允許高級用戶為調度、審查（Critic）、潤色（Refiner）智能體自定義映射不同的底層模型。
    3. *反思循環上限：* 提供 Slider（滑塊） 供用戶調整 Self-Correction Loop 次數（1 - 5 次）。
    4. *自定義工具源網址：* 預留 Repository URL 輸入框，供用戶切換第三方或自建的工具群組伺服器地址。
    5. *法律免責聲明：* 介面顯眼處配備繁體中文免責聲明（宣告本 App 為純本地工具，不收集用戶隱私與金鑰）。

---

## 5. 本地商用化與內購授權模式 (Local BYOK Monetization)
本項目採用「用戶自備大模型 API Key (BYOK)」與「本地功能限制解鎖」相結合的商業模式，平台不承擔模型算力與伺服器數據庫成本。
* **本地權權攔截 (Google Play IAP)：** 移除任何雲端伺服器扣費邏輯。改為本地權限控制：
  * *免費版限制：* 用戶最多只能同時建立 3 個本地隨機工作空間（Workspace），且只能使用基礎工具包。
  * *Premium 升級：* 整合 Google Play Billing Library。當用戶超過限制或點擊高級工具時，彈出付費牆（Paywall），引導用戶購買一次性永久解鎖（Lifetime）或本地訂閱制（Subscription）。
* **內購憑證校驗：** 透過 `BillingManager` 的本地抽象類別，在每次運行狀態機循環（`executeTaskLoop`）前進行本地權限攔截。若未解鎖且超出限制，則暫停狀態機並彈出付費牆。

## 6. 期待交付物 (Expected Deliverables)
1. **本地架構與 Room 關係圖：** 用 Mermaid 流程圖展現用戶新建空間 -> Room 資料庫初始化 -> 任務步驟與 Agent 路由拆解 -> 本地 Duktape 沙盒執行 -> 狀態原子寫入 Room 的完整閉環。
2. **核心 Kotlin 類別原始碼 (Room 本地化)：** 提供包含 `@Entity`、`@Dao` 的 Room 資料庫結構設計，以及 `WorkspaceManager`（基於 Room 的數據隔離）與 `ToolManager`（含數位簽章校驗與本地解壓）。
3. **Jetpack Compose 視圖層代碼：** 包含包含真正的對話氣泡流（支援 Citation 卡片）、底部分頁（Bottom Sheet）工具下載攔截、設定頁面（Slider 與 URL 輸入框）的完整 UI 實作。
4. **本地 Billing 攔截介面：** 基於 Google Play 內購機制的 `BillingManager` 實作原始碼，確保與狀態機執行循環無縫掛鉤。
