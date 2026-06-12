# Alive Please 自訂 GAS 寄信服務

這份教學會帶你使用自己的 Google 帳號建立寄信服務，再把它連接到 Alive Please。

**從 Google Play 安裝的 Alive Please 也可以使用自訂 GAS。** 不需要修改 App 或重新安裝，只要完成本教學，最後把產生的 Webhook URL 貼進 App 即可。

## 開始前先選擇

### 我只想正常使用 Alive Please

你不需要閱讀本文件。Alive Please 已內建預設寄信服務：

1. 在 App 設定頁填寫通知對象的稱呼與 Email。
2. 儲存設定。
3. 點擊「寄送測試 Email」確認收得到信。

進階設定中的 `GAS Webhook URL` 保持空白即可。

### 我想使用自己的 Google 帳號寄信

請繼續閱讀。完成後：

- 通知信會由你的 Google 帳號透過 Apps Script 寄出。
- 你可以自行停止、更新或刪除寄信服務。
- 你可以選擇將寄送結果記錄到自己的 Google Sheet。
- Google 會要求你授權 Apps Script 寄信。
- 寄信數量受你的 Google 帳號與 Apps Script 配額限制。

整個設定流程大約需要 10 分鐘。建議使用電腦操作 Apps Script，手機保留在旁邊設定 Alive Please。

## 第一次設定：照著做

### 步驟 1：建立 Apps Script 專案

1. 用你想負責寄信的 Google 帳號登入。
2. 前往 [Google Apps Script](https://script.google.com/)。
3. 點擊「新專案」。
4. 點擊左上角的專案名稱，將它改成容易辨識的名稱，例如 `Alive Please Mail`。
5. 中央編輯器會顯示一個名為 `Code.gs` 的檔案，以及預設的 `myFunction` 程式。
6. 將編輯器內的內容全部刪除。
7. 開啟 [`AlivePleaseWebhook.gs`](AlivePleaseWebhook.gs)，複製完整程式碼並貼到 `Code.gs`。
8. 按下 `Ctrl + S`，或點擊上方的儲存圖示。

完成時，編輯器最前面應該能看到 `const CONFIG = {`。

### 步驟 2：先使用最簡單的設定

在程式最上方找到 `CONFIG`，先改成下面這樣：

```javascript
const CONFIG = {
  WEBHOOK_TOKEN: '',
  MAIL_FROM_NAME: 'Alive Please',
  ALLOW_HTML: false,
  MAX_SUBJECT_LENGTH: 200,
  MAX_BODY_LENGTH: 20000,
  DEBUG_MODE: false,
  USAGE_LOG_SHEET_ID: '',
  USAGE_LOG_SHEET_NAME: 'usage_log',
  LOG_HEALTHCHECKS: false,
};
```

第一次測試時，建議只修改這幾項：

- `WEBHOOK_TOKEN`：保持空字串 `''` 即可正常使用。這是完全選用的安全性功能，不設定也不影響寄信。
- `MAIL_FROM_NAME`：收件人看到的寄件者顯示名稱。
- `USAGE_LOG_SHEET_ID`：請先保持空字串 `''`，避免尚未設定 Sheet 時產生紀錄錯誤。

其他項目先不要改。修改完成後再次儲存。

### 步驟 3：授權 Google 帳號寄信

1. 在 Apps Script 編輯器上方的函式選單中，選擇 `testSendEmail`。
2. 點擊「執行」。
3. 第一次執行時，Google 會要求授權。點擊「審查權限」。
4. 選擇你剛才登入的 Google 帳號。
5. 閱讀權限內容後允許 Apps Script 寄送 Email。
6. 回到編輯器，等待執行完成。
7. 打開自己的 Google 帳號信箱，尋找主旨為 `Alive Please GAS test` 的信。

`testSendEmail()` 只會寄送測試信到執行 Apps Script 的 Google 帳號，不會寄到 App 內設定的通知對象。

如果沒有收到信：

- 檢查垃圾郵件匣。
- 查看 Apps Script 畫面下方的執行紀錄。
- 點擊左側 `Executions`，查看失敗原因。

確認測試信成功後，再繼續下一步。

### 步驟 4：部署成 App 可以呼叫的 Web App

1. 點擊右上角 `Deploy`。
2. 選擇 `New deployment`。
3. 點擊部署類型旁的齒輪圖示。
4. 選擇 `Web app`。
5. `Execute as` 選擇自己，讓寄信使用你的 Google 帳號執行。
6. `Who has access` 選擇 `Anyone`。App 沒有登入你的 Google 帳號，因此必須允許它呼叫這個網址。
7. 點擊 `Deploy`。
8. 如果 Google 再次要求授權，依畫面完成授權。
9. 複製畫面顯示的 `Web app URL`。

正確網址會類似：

```text
https://script.google.com/macros/s/一長串部署代碼/exec
```

請確認網址最後是 `/exec`。不要將 Apps Script 編輯器網址或 `/dev` 測試網址貼進 App。

### 步驟 5：先用瀏覽器確認 Webhook 可用

1. 在瀏覽器新分頁貼上剛才複製的 `/exec` URL。
2. 按下 Enter。
3. 正常時會看到包含以下內容的文字：

```json
{
  "ok": true,
  "service": "alive-please-webhook",
  "message": "Webhook is running."
}
```

看到這個回應，代表部署網址可以被呼叫。這一步只檢查服務是否在線，不會寄信。

### 步驟 6：把 Webhook URL 貼進 Alive Please

1. 在手機開啟 Alive Please。Google Play 版本可以直接使用這項功能。
2. 進入設定頁。
3. 先確認至少有一位通知對象，而且每位都有稱呼與有效 Email。
4. 向下滑到「進階寄信設定」。
5. 在 `GAS Webhook URL` 欄位貼上剛才複製的完整 `/exec` URL。
6. 點擊「儲存設定」。
7. 點擊「寄送測試 Email」。
8. 請每位通知對象檢查收件匣與垃圾郵件匣。

Alive Please 支援多位通知對象。App 會為每位通知對象分別呼叫一次 Webhook，讓每封信使用對應的稱呼，且收件人不會看到其他人的 Email。

如果所有通知對象都收到測試信，就完成了。之後 Alive Please 會使用你的自訂 GAS，而不是內建預設寄信服務。

## 哪些設定要在哪裡修改？

這兩個地方的設定用途不同：

| 想調整的內容 | 修改位置 |
| --- | --- |
| 通知對象、稱呼、多久沒打卡後通知 | Alive Please App |
| 自訂 Webhook URL | Alive Please App |
| `WEBHOOK_TOKEN`、寄件者顯示名稱、Sheet 紀錄 | 你的 Apps Script `CONFIG` |
| Apps Script 程式內容 | 你的 Apps Script 專案 |

Google Play 版本可以輸入自訂 Webhook URL，但不能直接編輯 Apps Script 的 `CONFIG`。要修改 `CONFIG`，請回到你自己的 Apps Script 專案。

## 更新已部署的程式

修改 Apps Script 程式後，既有 `/exec` URL 不會自動使用新版本。請重新部署：

1. 點擊 `Deploy` → `Manage deployments`。
2. 編輯現有 Web App deployment。
3. 選擇建立新版本。
4. 再次部署。

若只修改 Google Sheet 內容或 App 內設定，則不需要重新部署 Apps Script。

## 選用：使用 WEBHOOK_TOKEN 保護網址

**這整個章節可以略過。沒有設定 `WEBHOOK_TOKEN`，Alive Please 仍可正常寄送測試信與親友通知。**

保持以下設定時，App 直接使用原始 `/exec` URL：

```javascript
WEBHOOK_TOKEN: '',
```

缺點是任何取得 Webhook URL 的人都能呼叫它，可能消耗你的 Google 寄信配額。如果你希望降低這項風險，才需要繼續設定 token。

### 1. 在 Apps Script 設定 token

回到 Apps Script，在 `CONFIG` 中設定一組自己產生、難以猜測的長字串：

```javascript
WEBHOOK_TOKEN: 'replace-with-a-long-random-secret',
```

儲存後，依照「更新已部署的程式」建立新版本並重新部署。

### 2. 在 App 的 URL 加入相同 token

回到 Alive Please，將 `GAS Webhook URL` 改成：

```text
https://script.google.com/macros/s/DEPLOYMENT_ID/exec?token=replace-with-a-long-random-secret
```

儲存後，再寄一次測試 Email。

注意事項：

- 不要把 token 公開貼在 issue、截圖或公開文件中。
- 更換 token 後，需要重新部署 Apps Script，並同步更新 App 內的 Webhook URL。
- `DEBUG_MODE` 正式使用建議設為 `false`。
- 寄信數量受 Google Apps Script 與 Gmail 帳號配額限制。

## 選用：記錄使用狀況到 Google Sheet

若希望記錄健康檢查、寄信成功與錯誤，可建立 Google Sheet，並將試算表 ID 填入：

```javascript
USAGE_LOG_SHEET_ID: 'your-google-sheet-id',
USAGE_LOG_SHEET_NAME: 'usage_log',
```

試算表 ID 位於網址中：

```text
https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit
```

首次寫入時，程式會自動建立 `usage_log` 工作表與標題列。欄位包含：

| 欄位 | 說明 |
| --- | --- |
| `timestamp_iso` | UTC ISO 時間 |
| `timestamp_local` | Apps Script 專案時區的時間 |
| `event` | 事件類型 |
| `ok` | 是否成功 |
| `http_method` | `GET`、`POST` 或 `MANUAL` |
| `recipient` | 收件人 Email |
| `subject_length` | 主旨字數，不包含主旨內容 |
| `body_length` | 內文字數，不包含信件內容 |
| `post_body_length` | POST body 長度 |
| `token_provided` | 請求是否帶有 token，不記錄 token 內容 |
| `query_keys` | Query parameter 名稱 |
| `error_name` | 錯誤類型 |
| `error_message` | 錯誤訊息 |

不需要 Sheet 紀錄時，將 `USAGE_LOG_SHEET_ID` 留空。Sheet 寫入失敗不會阻止寄信，錯誤只會出現在 Apps Script 執行紀錄中。

## 常見問題

### 我使用 Google Play 版本，也能自訂 GAS 嗎？

可以。自行架設 GAS 後，將完整 `/exec` URL 貼入 App 的進階寄信設定即可。App 不需要重新安裝。

### 我應該把 `CONFIG` 貼到 App 的哪裡？

不需要，也不能把整份 `CONFIG` 貼進 App。`CONFIG` 只在你自己的 Apps Script 專案中修改。App 只需要完整 Webhook URL。

### 一定要設定 `WEBHOOK_TOKEN` 嗎？

不用。將 `WEBHOOK_TOKEN` 保持為空字串 `''`，並在 App 填入原始 `/exec` URL，就能正常寄信。

token 只用來保護公開 Webhook URL，避免知道網址的人任意呼叫。它是選用的安全性功能，不是寄信必要條件。

### App 顯示寄送失敗

依序確認：

1. Webhook URL 是否以 `/exec` 結尾。
2. Web App 是否允許 App 呼叫。
3. Apps Script 是否已完成 Gmail 授權。
4. App 與 Apps Script 設定的 token 是否完全一致。
5. 修改程式後是否建立新版本並重新部署。
6. Google 帳號是否仍有寄信配額。
7. Apps Script 的 `Executions` 頁面是否有錯誤紀錄。

### 測試函式成功，但 App 寄信失敗

`testSendEmail()` 只測試 Apps Script 是否能使用該 Google 帳號寄信。請另外確認 Web App 部署權限、`/exec` URL，以及 token 設定。

### Google Sheet 沒有新增紀錄

確認：

- `USAGE_LOG_SHEET_ID` 是否為試算表 ID，而不是完整網址。
- 執行 Apps Script 的 Google 帳號是否有權限編輯該試算表。
- `USAGE_LOG_SHEET_NAME` 是否符合預期。
- Apps Script 執行紀錄中是否出現 `usage_log_failed`。

### 多位通知對象只收到部分信件

Alive Please 會逐位寄送，因此部分成功、部分失敗是可能的。請查看 App 執行紀錄與 Apps Script `Executions`，確認失敗收件人的 Email 與寄信配額。

## Webhook API 規格

一般使用者不需要手動呼叫此 API。本節提供給開發與除錯使用。

### POST request

```json
{
  "to": "family@example.com",
  "subject": "Alive Please check-in",
  "body": "message text"
}
```

必要欄位：

- `to`：單一有效 Email。多位通知對象由 App 分別送出多次 request。
- `subject`：不可為空，長度不可超過 `MAX_SUBJECT_LENGTH`。
- `body`：不可為空，長度不可超過 `MAX_BODY_LENGTH`。

成功回應：

```json
{
  "ok": true,
  "message": "Email sent.",
  "to": "family@example.com",
  "time": "2026-06-12T00:00:00.000Z"
}
```

失敗回應會包含 `ok: false` 與 `error`。當 `DEBUG_MODE` 為 `true` 時，回應可能另外包含 stack。

### 事件類型

- `healthcheck`：瀏覽器或工具透過 `GET /exec` 檢查服務。
- `mail_sent`：Webhook 成功寄信。
- `mail_error`：驗證、授權或寄信失敗。
- `test_mail_sent`：手動執行 `testSendEmail()` 成功。
