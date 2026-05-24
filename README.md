# CoreAgent
# 提示詞：移動端多智能體 AI Agent APP 移動端開發藍圖

## 1. 專案目標 (Project Objective)
請為我開發一個標準的 Android APK 項目。核心功能是實作一個基於「多任務工作空間（Multi-Workspace）」與「中央記憶庫狀態機（Memory-Bank State Machine）」調度的多智能體架構。App 允許用戶自定義配置、動態下載外部擴展工具鏈，在完全隔離的 Session 環境中執行複雜任務拆解與雙重校驗，確保商用級別的準確性與數據獨立性。

## 2. 技術棧與基準架構 (Technical Stack)
* **平台環境：** Android (Target SDK 34/35)，使用 Kotlin 编写底層核心，UI 採用 Jetpack Compose 實作多頁面導航（Navigation）。
* **核心架構：** 客戶端/服務端（C/S）雙軌架構。手機端（Client）負責 UI 渲染、本地狀態緩存與 API Key 管理；雲端服務端（Server）負責中央工作空間狀態同步、用戶權限攔截及高級中繼處理。數據庫採用 PostgreSQL 或 MySQL 進行多租戶工作空間隔離（Multi-Workspace DB Isolation）。

---

## 3. 分步實作步驟與核心邏輯 (Implementation Details)

### 第一步：多任務空間（Multi-Workspace）與記憶庫初始化
1. **工作空間隔離：** 用戶新建任務時，系統分發唯一的 `Workspace_ID`。數據庫基於該 ID 對聊天記錄、記憶狀態、生成資產進行物理隔離。
2. **中央記憶庫（Memory Bank）：** 每個空間初始化一個結構化對象，包含 `GlobalContext`（全局專案宣告與目標摘要）及 `TaskQueue`（任務陣列）。

### 第二步：動態任務拆解與外掛工具安全校驗 (Dynamic Tool Binding)
1. **任務拆解：** 接收用戶指令後，調度器將其線性拆解為多個子步驟（Steps）存入 `TaskQueue`。每步包含：`step_id`、`task_description`、`assigned_agent`、`status`（PENDING/RUNNING/COMPLETED）及`required_tools`（所需工具清單）。
2. **外掛市場與安全下載：** 連接遠端工具群組倉庫（Plugin Repository）。App 執行步驟前檢查本地 `required_tools`。若未安裝，中斷 Pipeline 並提示用戶一鍵下載（含工具 JSON Schema 與執行指令碼）。下載時必須觸發 SHA-256 雜湊與數位簽章校驗，防範惡意代碼注入。

### 第三步：跨越上下文限制的「會話隔離與無衝突執行」 (Session Isolation)
為了繞過長對話導致的邏輯衰退與 Token 費用爆炸：
1. **依賴型上下文注入：** 啟動子步驟時，App 為大模型強制建立**完全乾淨、隔離的新會話（New Session）**。僅注入：`GlobalContext` + `當前步驟描述` + `具備直接依賴關係的前置步驟生產物` + `已驗證工具的 Function Calling 宣告`。嚴禁帶入歷史對話廢話。
2. **合規代碼執行器 (Compliant Tool Runner)：** 當 Agent 觸發工具調用（如 GitHub API）時，輸出結構化參數。本地沙盒必須採用符合 Android 安全規範的內置解釋器（如純 JVM 環境下的 JS/Kotlin 解析內核）或將執行權安全委託給 Cloud Server 執行，規避 Android 10+ 的 W^X 執行限制。
3. **狀態機履歷更新：** 審查智能體（Critic Agent）進行自我反思核對（上限 3 次）。驗證通過後，將精確成品寫入 `produced_artifacts`，變更步驟狀態為 `COMPLETED`，記錄 `used_tools`。調度器隨即自動推進下一任務，全案完成後由潤色智能體（Refiner Agent）整合輸出最終報告。

### 第四步：斷點續傳與回復 (Resilience & Recovery)
* 網絡崩潰或 API 異常時，記憶庫完整保留當前停滯步驟的上下文明細。App 不得崩潰，支援用戶點擊「重試」時，直接從出錯的步驟讀取記憶庫並新開會話續傳執行。

---

## 4. UI/UX 介面設計規範 (Jetpack Compose Multi-Screen)
利用 Navigation 組件實作以下畫面流暢切換，將後台狀態高度可視化：

* **畫面一：多專案工作空間儀表板 (Dashboard Screen)**
  * 列表展示所有歷史 `Workspace_ID` 項目。頂部設置「+ 新建任務」按鈕，點擊彈出命名框，確認後開闢全新乾淨對話與獨立記憶庫。
* **畫面二：主聊天與狀態看板畫面 (Chat & Workspace Screen)**
  * 顯示當前項目的對話流。頂部配備可摺疊的 **「記憶庫進度看板」組件 (Memory Bank Accordion Widget)**，實時綁定 `TaskQueue` 的狀態並以動畫展示後台智能體流水線進度。
  * 對話氣泡支援 **Citations System**，將工具調用來源渲染為乾淨的「來源標籤卡片（Source Chips）」。工具缺失時自動彈出 **底部 Sheet 攔截視窗** 引导用戶下載。
* **畫面三：外掛工具市場 (Tool Marketplace Screen)**
  * 網格展示雲端群組提供的工具外掛（如自動運維、數據庫對接），具備一鍵下載進度條與「已激活/未安裝」狀態。
* **畫面四：高度擴展配置設定頁面 (Settings Screen)**
  * 提供以下模組化配置項：
    1. *API Key 核心管理（安全加密）：* 供用戶輸入並本地加密儲存自備的多廠商（OpenAI/Gemini/Anthropic）API Key。
    2. *大模型專家路由映射：* 允許高級用戶為調度、審查、潤色智能體自定義映射不同的底層模型。
    3. *反思循環上限：* 滑動調整 Self-Correction Loop 次數（1 - 5 次）。
    4. *自定義工具源網址 (Repository URL)：* 預留輸入框，供用戶切換第三方工具群組伺服器地址。

---

## 5. 商用對接與自備金鑰計費模式 (BYOK Monetization)
本項目採用「用戶自備大模型 API Key (BYOK)」模式，平台不承擔模型算力成本。
* **權限攔截與數據庫結算：** 雲端數據庫根據 `User_ID` 維護其訂閱狀態（包月制/包年制/計次點數制）。每當一個工作空間的所有子步驟成功 COMPLETED 並輸出後，後端數據庫扣除 1 次計次。權限用盡則暫停狀態機運行並彈出付費牆（Paywall）。
* **外掛市場增值收費：** 設置「工具購買記錄表」。基礎工具免費下載，高級工具外掛（如企業級自動化運維插件）需通過 Google Play IAP 購買授權，後端 Webhook 驗證成功後，解鎖該用戶對該遠端工具包的下載與同步權限。設定頁面需配備顯眼的繁體中文免責聲明。

## 6. 期待交付物 (Expected Deliverables)
1. **系統架構圖：** 用 Mermaid 流程圖展現用戶新建空間 -> 初始化記憶庫 -> 分步依賴注入 -> 執行器合規調用 -> 狀態機更新的完整閉環。
2. **核心 Kotlin 類別原始碼：** 提供 `WorkspaceManager`（空間數據隔離）、`MemoryBank`（狀態機結構體）、`ToolManager`（具備安全校驗的工具下載與 Schema 註冊）底層框架。
3. **Jetpack Compose 視圖層代碼：** 包含多頁面導航、側邊欄專案切換、記憶庫摺疊組件以及設定頁面的 UI 實作。
4. **付費與外掛權限介面：** 用於後端攔截與 IAP 憑證校驗的 `BillingManager` 抽象類別原始碼。
