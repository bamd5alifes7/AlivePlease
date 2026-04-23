# Alive Please GAS Webhook

這份 `Google Apps Script` 是給目前 app 直接用的 webhook，接收 app 送出的 JSON：

```json
{
  "to": "family@example.com",
  "subject": "Alive Please check-in",
  "body": "message text"
}
```

## 功能

- 驗證 `to / subject / body`
- 可選擇用 `token` 保護 webhook
- 寄送 email
- 把每次請求另外寫進 Google Sheet，方便追蹤使用量
- 失敗時回傳 JSON，方便 app 和 Apps Script 兩邊排查

## 設定步驟

1. 到 [Google Apps Script](https://script.google.com/) 建一個新專案。
2. 把 [`AlivePleaseWebhook.gs`](/C:/Users/Adrain Hui/.gemini/antigravity/scratch/AlivePlease/docs/AlivePleaseWebhook.gs) 全部貼進去。
3. 視需要調整 `CONFIG`：

```javascript
const CONFIG = {
  WEBHOOK_TOKEN: 'your-secret-token',
  MAIL_FROM_NAME: 'Alive Please',
  ALLOW_HTML: false,
  MAX_SUBJECT_LENGTH: 200,
  MAX_BODY_LENGTH: 20000,
  DEBUG_MODE: true,
  USAGE_LOG_SHEET_ID: 'your-google-sheet-id',
  USAGE_LOG_SHEET_NAME: 'usage_log',
  LOG_HEALTHCHECKS: true,
};
```

4. 建立一份 Google 試算表，複製網址中的 spreadsheet ID，填進 `USAGE_LOG_SHEET_ID`。
5. `Deploy` -> `New deployment` -> 選 `Web app`。
6. `Execute as` 選你自己。
7. `Who has access` 依你的需求設定，通常是 `Anyone` 或 `Anyone with the link`。
8. 把部署後的 `/exec` URL 貼回 app 的 `GAS Webhook URL`。
9. 如果有設定 token，就把 URL 改成：

```text
https://script.google.com/macros/s/DEPLOYMENT_ID/exec?token=your-secret-token
```

## Google Sheet 會記什麼

第一次寫入時，程式會自動建立工作表與表頭。每筆請求會記錄：

- `timestamp_iso`
- `timestamp_local`
- `event`
- `ok`
- `http_method`
- `recipient`
- `subject_length`
- `body_length`
- `post_body_length`
- `token_provided`
- `query_keys`
- `error_name`
- `error_message`

你之後可以直接用這張表做：

- 每日請求量
- 成功/失敗次數
- 常見錯誤原因
- 單一收件者的使用次數

## 目前會寫進表格的事件

- `healthcheck`: `GET /exec`
- `mail_sent`: webhook 成功寄信
- `mail_error`: webhook 驗證或寄信失敗
- `test_mail_sent`: 手動執行 `testSendEmail()`

## 注意事項

- 如果 `USAGE_LOG_SHEET_ID` 留空，程式就不會寫入表格。
- logging 寫入失敗不會影響主要寄信流程，只會在 Apps Script log 裡留下 `usage_log_failed`。
- 這份 usage log 主要是追蹤用量，不建議把完整信件內容寫進表格，避免敏感資料外流。
- Apps Script 和試算表都要用同一個 Google 帳號，或至少該帳號要有該試算表的編輯權限。

## 測試方式

打開 webhook URL，應該會看到：

```json
{
  "ok": true,
  "service": "alive-please-webhook"
}
```

之後從 app 送一次請求，或手動執行 `testSendEmail()`，就能在 Google Sheet 看到一筆新紀錄。
