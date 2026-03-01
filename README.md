# G85 Report Generator

符合 **SEMI G85-1101** 標準的晶圓圖報表產生系統，基於 Spring Boot 3 建置，提供 REST API 介面，可依批號（Lot ID）自動產生每片晶圓的 XML 報表。

---

## 目錄

- [專案簡介](#專案簡介)
- [技術架構](#技術架構)
- [快速開始](#快速開始)
- [專案結構](#專案結構)
- [資料模型](#資料模型)
- [API 說明](#api-說明)
  - [1. 產生 G85 報表](#1-產生-g85-報表)
  - [2. 下載個別晶圓報表](#2-下載個別晶圓報表)
  - [3. 查詢報表產生歷程](#3-查詢報表產生歷程)
  - [4. 產生報表並取得 SQL 溯源軌跡](#4-產生報表並取得-sql-溯源軌跡)
- [G85 XML 格式](#g85-xml-格式)
- [設定檔](#設定檔)
- [範例資料](#範例資料)

---

## 專案簡介

半導體製程中，每批晶圓（Lot）完成測試後須產出晶圓圖（Wafer Map），記錄每顆 Die 的測試分類結果（Bin）。本系統依照業界標準 **SEMI G85-1101** 格式產生 XML 晶圓圖報表，供下游製程、品管及客戶使用。

**主要功能：**

- 依批號一鍵產生該批所有晶圓的 G85 XML 報表
- 支援個別晶圓報表下載
- 查詢歷史報表產生紀錄
- **即時 SQL 溯源**：產生報表的同時，回傳每片晶圓的 SQL 執行軌跡（含真實參數值與說明文字）
- 內建 H2 嵌入式資料庫，無需安裝額外資料庫
- 首次啟動自動建立範例資料（25 片晶圓）

---

## 技術架構

| 元件 | 版本 / 說明 |
|------|------------|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Spring Data JPA | Hibernate ORM |
| H2 Database | 檔案模式（File-based），路徑 `./data/g85db` |
| datasource-proxy | 1.10，JDBC 層 SQL 攔截，用於即時 SQL 溯源 |
| Lombok | 減少樣板程式碼 |
| Maven | 建置工具 |

---

## 快速開始

### 前置需求

- JDK 17+
- Maven 3.8+

### 建置與啟動

```bash
# 複製專案
git clone <repository-url>
cd g85-report

# 建置
mvn clean package -DskipTests

# 啟動
java -jar target/g85-report-1.0.0-SNAPSHOT.jar
```

或直接以 Maven 啟動：

```bash
mvn spring-boot:run
```

服務啟動後監聽 `http://localhost:8080`。

### 產生第一份報表

首次啟動會自動建立批號 `999999` 的 25 片晶圓範例資料。執行以下指令立即產生報表：

```bash
curl -X POST http://localhost:8080/api/report/g85/generate \
     -H "Content-Type: application/json" \
     -d '{"lotId": "999999"}'
```

產出的 XML 檔案位於 `./reports/999999/` 目錄。

---

## 專案結構

```
g85-report/
├── src/main/java/com/example/g85report/
│   ├── G85ReportApplication.java       # Spring Boot 入口
│   ├── config/
│   │   ├── DataInitializer.java        # 範例資料初始化
│   │   ├── SqlCaptureHolder.java       # ThreadLocal SQL 收集工具
│   │   ├── SqlCaptureListener.java     # datasource-proxy SQL 攔截監聽器
│   │   └── DataSourceProxyBeanPostProcessor.java  # DataSource 代理包裝
│   ├── controller/
│   │   └── G85ReportController.java    # REST API 端點
│   ├── dto/
│   │   ├── GenerateReportRequest.java      # 報表產生請求 DTO
│   │   ├── GenerateReportResponse.java     # 報表產生回應 DTO
│   │   ├── GenerateWithTraceResponse.java  # 含 SQL 溯源的回應 DTO
│   │   ├── SqlTraceStep.java               # 單筆 SQL 步驟（含說明）
│   │   └── WaferSqlTrace.java              # 單片晶圓的 SQL 步驟清單
│   ├── entity/
│   │   ├── WaferInfo.java              # 晶圓基本資訊
│   │   ├── DieResult.java              # Die 測試結果
│   │   ├── BinDefinition.java          # Bin 分類定義
│   │   └── ReportLog.java              # 報表產生歷程
│   ├── repository/
│   │   ├── WaferInfoRepository.java
│   │   ├── DieResultRepository.java
│   │   ├── BinDefinitionRepository.java
│   │   └── ReportLogRepository.java
│   └── service/
│       ├── G85ReportService.java       # 報表產生業務邏輯
│       └── G85XmlGenerator.java        # G85 XML 格式產生器
├── src/main/resources/
│   └── application.properties          # 應用程式設定
├── data/                               # H2 資料庫檔案
├── reports/                            # 報表輸出目錄
├── pom.xml
└── README.md
```

---

## 資料模型

### WaferInfo（晶圓資訊）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `waferId` | String(50) | 晶圓唯一識別碼（主鍵），格式如 `999999-0001` |
| `lotId` | String(20) | 批號 |
| `productId` | String(30) | 產品代號 |
| `waferSize` | String | 晶圓尺寸（mm），如 `300` |
| `supplierName` | String(50) | 供應商名稱 |
| `dieRows` | int | Die 格數（行） |
| `dieCols` | int | Die 格數（列） |
| `orientation` | int | 晶圓旋轉角度（0/90/180/270） |
| `originLoc` | int | 原點位置（1-4，角落） |
| `nullBin` | int | 無效區域的 Bin 碼（預設 255） |
| `createDate` | LocalDateTime | 建立時間 |

### DieResult（Die 測試結果）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | Long | 自增主鍵 |
| `waferId` | String | 關聯晶圓 ID |
| `dieRow` | int | Die 行座標（0-based） |
| `dieCol` | int | Die 列座標（0-based） |
| `binCode` | int | 測試分類碼 |

### BinDefinition（Bin 分類定義）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | Long | 自增主鍵 |
| `productId` | String | 產品代號 |
| `binCode` | int | Bin 碼 |
| `binQuality` | String | 品質分類：`Pass` 或 `Fail` |
| `binDesc` | String | 說明文字 |

### ReportLog（報表產生歷程）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `reportId` | String(36) | UUID，本次產生的唯一識別碼 |
| `lotId` | String | 批號 |
| `status` | String | `SUCCESS` 或 `FAIL` |
| `waferCount` | int | 本次產生的晶圓片數 |
| `outputPath` | String | 報表輸出目錄 |
| `errorMsg` | String | 失敗時的錯誤訊息 |
| `createdAt` | LocalDateTime | 產生時間 |

---

## API 說明

### 1. 產生 G85 報表

```
POST /api/report/g85/generate
Content-Type: application/json
```

**Request Body：**

```json
{
  "lotId": "999999"
}
```

**Response（成功）：**

```json
{
  "reportId": "a1b2c3d4-...",
  "lotId": "999999",
  "waferCount": 25,
  "status": "SUCCESS",
  "outputPath": "./reports/999999",
  "files": [
    "999999-0001.xml",
    "999999-0002.xml",
    "..."
  ],
  "errorMsg": null
}
```

**Response（失敗）：**

```json
{
  "reportId": "a1b2c3d4-...",
  "lotId": "INVALID",
  "waferCount": 0,
  "status": "FAIL",
  "outputPath": null,
  "files": [],
  "errorMsg": "查無此 Lot ID 的晶圓資料"
}
```

---

### 2. 下載個別晶圓報表

```
GET /api/report/g85/download/{waferId}
```

**範例：**

```bash
curl -O http://localhost:8080/api/report/g85/download/999999-0001
```

回傳 XML 檔案，Content-Type 為 `application/xml`，以附件形式下載。

---

### 3. 查詢報表產生歷程

```
GET /api/report/g85/history?lotId={lotId}
```

**範例：**

```bash
curl "http://localhost:8080/api/report/g85/history?lotId=999999"
```

**Response：**

```json
[
  {
    "reportId": "a1b2c3d4-...",
    "lotId": "999999",
    "status": "SUCCESS",
    "waferCount": 25,
    "outputPath": "./reports/999999",
    "errorMsg": null,
    "createdAt": "2024-01-01T12:00:00"
  }
]
```

---

### 4. 產生報表並取得 SQL 溯源軌跡

```
POST /api/report/g85/generate-with-trace
Content-Type: application/json
```

行為與 `/generate` 完全相同，額外在 Response 中回傳每片晶圓的 SQL 執行軌跡（含真實參數值）。SQL 僅在該次請求期間以 ThreadLocal 暫存，不寫入資料庫。

**Request Body：**

```json
{
  "lotId": "999999"
}
```

**Response（成功）：**

```json
{
  "reportId": "a1b2c3d4-...",
  "lotId": "999999",
  "waferCount": 25,
  "status": "SUCCESS",
  "outputPath": "./reports/999999/",
  "files": ["999999-0001.xml", "999999-0002.xml", "..."],
  "errorMsg": null,
  "sqlTrace": [
    {
      "waferId": "999999-0001",
      "steps": [
        {
          "stepOrder": 1,
          "sqlType": "WAFER_INFO",
          "sql": "select wi1_0.wafer_id,... from wafer_info wi1_0 where wi1_0.lot_id='999999'",
          "explanation": "依批號（lot_id）查詢該批次內所有晶圓的基本資訊，包含產品代號、晶圓尺寸、Die 格數等，是整份報表產生流程的起點。"
        },
        {
          "stepOrder": 2,
          "sqlType": "DIE_RESULT",
          "sql": "select dr1_0.id,... from die_result dr1_0 where dr1_0.wafer_id='999999-0001'",
          "explanation": "依晶圓 ID（wafer_id）查詢該晶圓所有 Die 的座標（row, col）與 Bin 分類碼（bin_code），用於填入 G85 XML 的晶圓格點陣列。"
        },
        {
          "stepOrder": 3,
          "sqlType": "BIN_DEFINITION",
          "sql": "select bd1_0.id,... from bin_definition bd1_0 where bd1_0.product_id='PROD-001' order by bd1_0.bin_code",
          "explanation": "依產品代號（product_id）查詢所有 Bin 碼的品質分類（Pass/Fail）與描述文字，用於在 G85 XML 輸出 <Bin> 標籤。"
        }
      ]
    },
    {
      "waferId": "999999-0002",
      "steps": ["..."]
    }
  ]
}
```

**Response（失敗）：**

```json
{
  "reportId": "...",
  "lotId": "NOTEXIST",
  "waferCount": 0,
  "status": "FAIL",
  "outputPath": "./reports/NOTEXIST/",
  "files": [],
  "errorMsg": "LotId not found: NOTEXIST",
  "sqlTrace": []
}
```

**sqlTrace 欄位說明：**

| 欄位 | 說明 |
|------|------|
| `waferId` | 晶圓 ID |
| `steps[].stepOrder` | 步驟序號（1-based） |
| `steps[].sqlType` | 查詢的目標表格：`WAFER_INFO` / `DIE_RESULT` / `BIN_DEFINITION` |
| `steps[].sql` | 含真實參數值的完整 SQL |
| `steps[].explanation` | 該 SQL 的業務說明文字 |

> **實作原理：** 使用 `datasource-proxy` 在 JDBC 層攔截所有 SQL 執行，以 `ThreadLocal<List<String>>` 收集當次請求的 SQL 軌跡，請求結束後自動清除，不占用額外資料庫資源。

---

## G85 XML 格式

產生的 XML 符合 **SEMI G85-1101** 標準，結構如下：

```xml
<?xml version="1.0" encoding="utf-8"?>
<Maps>
  <Map xmlns:semi="http://www.semi.org" WaferId="999999-0001" FormatRevision="G85-1101">
    <Device ProductId="PROD-001"
            WaferSize="300"
            LotId="999999"
            WaferType="Production"
            Rows="20"
            Columns="15"
            NullBin="255"
            Orientation="0"
            OriginLocation="3">
      <Bin BinCode="000" BinCount="3"   BinQuality="Fail" BinDescription="No Pick Site"/>
      <Bin BinCode="001" BinCount="132" BinQuality="Pass" BinDescription="Pick"/>
      <Bin BinCode="091" BinCount="14"  BinQuality="Fail" BinDescription="Ugly"/>
      <Bin BinCode="092" BinCount="5"   BinQuality="Fail" BinDescription="Electrical Fail"/>
      <SupplierData ProductCode="PROD-001" RecipeName="" ToolType=""/>
      <Data MapName="WAFER_MAP" Version="2">
        <Row><![CDATA[255 255 255 001 001 001 255 255 255 255 255 255 255 255 255]]></Row>
        <!-- ... 20 rows total ... -->
      </Data>
    </Device>
  </Map>
</Maps>
```

**格式說明：**

- `BinCode` 以 3 位數零填補（如 `001`、`091`）
- `NullBin`（預設 `255`）標記晶圓有效區域外的 Die 位置
- `<Data>` 區段的每個 `<Row>` 以 CDATA 包覆，Die 值以空格分隔
- `OriginLocation` 為座標原點角落（`3` = 左下角）

---

## 設定檔

`src/main/resources/application.properties`：

```properties
# 伺服器埠號
server.port=8080

# H2 資料庫（檔案模式）
spring.datasource.url=jdbc:h2:file:./data/g85db;AUTO_SERVER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2 Web Console（開發用）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# 報表輸出根目錄
report.output.base-path=./reports
```

H2 Web Console 位於 `http://localhost:8080/h2-console`（僅供開發環境使用）。

---

## 範例資料

系統首次啟動且資料庫為空時，`DataInitializer` 會自動建立：

| 項目 | 內容 |
|------|------|
| 批號（Lot ID） | `999999` |
| 晶圓片數 | 25 片（`999999-0001` ～ `999999-0025`） |
| 產品代號 | `PROD-001` |
| 晶圓尺寸 | 300 mm |
| Die 格數 | 20 行 × 15 列 |
| 有效區域 | 模擬圓形晶圓，邊角標記 NullBin（255） |

**Bin 分類定義：**

| Bin 碼 | 品質 | 說明 | 比例 |
|--------|------|------|------|
| 0 | Fail | No Pick Site | 約 2% |
| 1 | Pass | Pick | 約 85% |
| 91 | Fail | Ugly | 約 10% |
| 92 | Fail | Electrical Fail | 約 3% |

產生的報表輸出於 `./reports/999999/`，共 25 個 XML 檔案。
