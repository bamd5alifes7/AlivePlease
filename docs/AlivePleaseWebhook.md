# Alive Please GAS Webhook

這份 `Google Apps Script` 是給目前 app 直接用的，對應它現在送出的 JSON：

```json
{
  "to": "family@example.com",
  "subject": "主旨",
  "body": "內文"
}
```

## 部署

1. 到 [Google Apps Script](https://script.google.com/) 建一個新專案。
2. 把 [`AlivePleaseWebhook.gs`](/C:/Users/Adrain Hui/.gemini/antigravity/scratch/AlivePlease/docs/AlivePleaseWebhook.gs) 全部貼進去。
3. 如果你想加簡單保護，把 `CONFIG.WEBHOOK_TOKEN` 改成自己的字串。
4. `Deploy` -> `New deployment` -> 類型選 `Web app`。
5. `Execute as` 選你自己。
6. `Who has access` 選 `Anyone`。
7. 部署後拿到 `/exec` URL，貼回 app 的 `GAS Webhook URL`。
8. 如果有設 token，app 裡填的 URL 改成：

```text
https://script.google.com/macros/s/你的部署ID/exec?token=你的密鑰
```

## 測試

先用瀏覽器開 webhook URL，應該會看到：

```json
{
  "ok": true,
  "service": "alive-please-webhook"
}
```

再用 app 裡的「測試信件」按鈕送一次。

## 這版做了什麼

- 驗證 `to / subject / body` 是否存在。
- 檢查 email 格式。
- 限制主旨與內文長度，避免異常 payload。
- 可選 token 驗證。
- 成功時回傳 JSON，失敗時直接丟錯，方便在 Apps Script 執行紀錄查原因。

## 注意

- 第一次部署後，Google 會要求授權 `MailApp.sendEmail`。
- 如果你重新部署了新版，app 最好更新成最新的 `/exec` URL。
- 你目前 Android 端只看 HTTP 是否成功；如果之後你想把錯誤訊息更明確顯示在 app，我也可以順手幫你把 `WebhookHelper.kt` 改成讀取回傳 JSON。
