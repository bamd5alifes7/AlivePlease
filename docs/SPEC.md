# Alive Please 規格文件

最後整理日期：2026-05-24

本文件整理目前 repository 中可觀察到的產品與技術規格，主要依據 Android App 程式碼、Google Apps Script Webhook、既有 README/docs 與測試案例推導。未在程式中明確實作的內容會標示為「待確認」或「已知限制」。

## 1. 產品定位

Alive Please 是一個 Android 自我報平安 App。使用者透過每日或定期打卡表示自己狀態正常；若超過設定時間未打卡，App 會透過 Google Apps Script Webhook 寄送 Email 給指定家人或聯絡人。

產品同時提供照護型提醒、打卡連續天數、祝福進度、日曆紀錄、設定教學與執行紀錄，核心目標是讓使用者能低成本維持「我還好」的訊號，並在失聯風險出現時通知家人。

## 2. 技術概況

- 平台：Android，`minSdk 24`，`targetSdk 35`。
- 語言：Kotlin。
- UI：Jetpack Compose、Material 3、Navigation Compose、HorizontalPager。
- 背景任務：WorkManager。
- 儲存：`SharedPreferences`，名稱為 `alive_please_prefs`。
- 網路：OkHttp，POST JSON 到 GAS Webhook。
- 外部服務：Google Apps Script `MailApp.sendEmail`，可選 Google Sheet usage log。
- 測試：JUnit、Robolectric、WorkManager testing、MockK。

## 3. 主要使用流程

### 3.1 首次啟動

- App 依 `is_first_launch` 決定起始頁。
- 首次啟動顯示 Onboarding。
- Onboarding 主要操作會：
  - 將首次啟動狀態設為完成。
  - 進入首頁。
  - 接著導向設定教學頁。
- Onboarding 次要操作會：
  - 將首次啟動狀態設為完成。
  - 直接進入首頁。

### 3.2 首頁打卡

- 首頁顯示：
  - 今日照護訊息。
  - 圓形打卡按鈕。
  - 累積打卡天數。
  - 目前連續打卡天數。
  - 距離下一次祝福的天數。
  - 家人通知倒數。
  - 打卡日曆。
- 使用者按下打卡後：
  - 更新 `last_check_in_time`。
  - 若今天尚未打卡，將今天加入 `check_in_dates`。
  - 累積打卡天數設為打卡日期集合數量。
  - 重新排程家人通知。
  - 顯示打卡成功回饋約 2 秒。
- 若連續打卡天數為 3 的倍數，顯示祝福卡約 6 秒。
- 首頁照護訊息：
  - 從 `care_messages` 隨機抽取。
  - 進入頁面時刷新一次。
  - 每 60 秒自動輪替。
  - 下拉刷新會更換訊息。

### 3.3 設定

設定頁可由首頁右上角進入，或以教學模式開啟。

目前可設定項目：

- 使用者顯示名稱。
- 打卡提醒週期，單位為小時，預設 12 小時。
- 家人通知等待時間，單位為小時，可為小數，預設 28 小時。
- 家人 Email。
- 收件人稱謂。
- 照護提醒開關，預設開啟。
- 安靜時段開關，預設開啟。
- 安靜時段開始與結束時間，預設 23:00 到 07:00。
- GAS Webhook URL；若未填寫，使用內建預設 URL。

儲存規則：

- 家人 Email 非空時必須符合 Android `Patterns.EMAIL_ADDRESS`。
- 安靜時段必須符合 `HH:mm` 格式，且小時為 `00..23`、分鐘為 `00..59`。
- 打卡提醒週期需能轉為大於 0 的整數才會寫入。
- 家人通知等待時間需能轉為大於 0 的浮點數才會寫入。
- 儲存成功後呼叫外層 `onSettingsSaved`，重新排程背景任務。

設定頁也提供：

- 寄送測試 Email。
- 檢視執行紀錄。
- 重新播放設定教學。
- 頂部狀態摘要：照護提醒是否開啟、家人通知是否已設定、郵件服務使用預設或自訂 URL。

### 3.4 設定教學

設定教學模式目前有 4 個步驟：

1. 使用者名稱，必要。
2. 家人 Email，必要。
3. 照護提醒開關，選用。
4. 儲存設定，必要。

教學模式會：

- 依步驟高亮對應設定區塊。
- 將非目標區塊降低透明度。
- 將說明浮層固定在畫面底部。
- 最後一步顯示最低設定需求。

## 4. 背景任務與通知規格

### 4.1 通知權限

- App 宣告 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、`INTERNET`。
- Android 13 以上會請求通知權限。
- 通知權限取得後會排程背景任務。

### 4.2 通知 Channel

App 建立 3 個通知 channel：

- `check_in_reminder`：打卡提醒，高重要性，啟用震動。
- `care_message`：照護訊息，一般重要性，不震動。
- `family_status`：家人通知寄送狀態，一般重要性，啟用震動。

### 4.3 打卡提醒

- 使用 PeriodicWorkRequest 排程 `CheckInReminderWorker`。
- 週期取自 `notify_interval`，預設 12 小時。
- ExistingPeriodicWorkPolicy 使用 `UPDATE`。
- 若觸發時在安靜時段內：
  - 不立刻發通知。
  - 改以一次性工作延後到安靜時段結束。
- 若不在安靜時段，發送打卡提醒通知。
- 打卡提醒通知會顯示距離下一次 3 日祝福還剩幾天。

### 4.4 照護提醒

- 照護提醒預設開啟。
- 每次排程隨機延遲 4 到 8 小時。
- 使用 unique one-time work：`care_notification`。
- 若功能關閉，取消照護提醒工作。
- 若觸發時在安靜時段內：
  - 不發通知。
  - 重新排程到安靜時段結束。
- 若不在安靜時段：
  - 從 `care_messages` 隨機抽取一則訊息。
  - 發送照護通知。
  - 再排程下一次照護提醒。

### 4.5 家人逾時通知

排程條件：

- 使用者至少打卡過一次。
- 家人 Email 不為空。

排程方式：

- 使用 unique one-time work：`family_notification_check`。
- delay 為距離 `last_check_in_time + family_notify_interval` 的剩餘時間。
- 若剩餘時間小於等於 0，使用至少 1 秒 delay。
- 需要網路連線。
- WorkManager backoff 為 exponential，最小 15 分鐘。

寄送條件：

- 觸發時仍符合排程條件。
- `shouldNotifyFamily()` 為 true，也就是距上次打卡已超過家人通知等待時間。
- 家人 Email 與 Webhook URL 均存在。

寄送流程：

- 使用 `EmailContentBuilder` 建立 subject 與 body。
- 透過 `WebhookHelper.sendEmail()` POST 到 Webhook。
- 成功時：
  - 寫入執行紀錄。
  - 發送本機成功狀態通知。
  - Worker 回傳 success。
- 失敗時：
  - 寫入執行紀錄。
  - `runAttemptCount < 2` 時回傳 retry。
  - 超過重試條件後，發送本機失敗狀態通知並回傳 success。

### 4.6 重新開機與 App 更新

`BootReceiver` 監聽：

- `BOOT_COMPLETED`
- `LOCKED_BOOT_COMPLETED`
- `MY_PACKAGE_REPLACED`

收到後呼叫 `WorkSchedulerHelper.rescheduleAll()`，依目前設定重建照護提醒與家人通知排程。

## 5. 資料與狀態

主要偏好設定 key：

| Key | 說明 | 預設 |
| --- | --- | --- |
| `last_check_in_time` | 最後打卡時間，毫秒 | `0` |
| `alive_days` | 累積打卡天數 | `0` |
| `check_in_dates` | 已打卡日期集合，逗號分隔 `yyyy-MM-dd` | 空 |
| `notify_interval` | 打卡提醒週期，小時 | `12` |
| `family_notify_interval_float` | 家人通知等待時間，小時，可小數 | fallback 至整數設定 |
| `family_notify_interval` | 舊版家人通知等待時間，小時 | `28` |
| `family_email` | 家人 Email | 空 |
| `family_recipient_title` | 收件人稱謂 | 程式內預設字串 |
| `gas_webhook_url` | 自訂 GAS Webhook URL | 空，空值時使用內建 URL |
| `care_notification_on` | 照護提醒開關 | `true` |
| `quiet_hours_enabled` | 安靜時段開關 | `true` |
| `quiet_hours_start_minutes` | 安靜時段開始分鐘 | `1380`，即 23:00 |
| `quiet_hours_end_minutes` | 安靜時段結束分鐘 | `420`，即 07:00 |
| `execution_logs` | 執行紀錄，`||` 分隔 | 空 |
| `is_first_launch` | 是否首次啟動 | `true` |
| `user_name` | 使用者名稱 | 程式內預設字串 |

打卡日期使用裝置 locale 的 `SimpleDateFormat("yyyy-MM-dd")` 產生。連續打卡天數使用 `LocalDate.parse()` 解析 ISO local date 字串，從最新日期往前連續計算。

執行紀錄：

- 每筆格式為 `[MM/dd HH:mm:ss] message`。
- 新紀錄加在最前面。
- 最多保留 50 筆。

## 6. GAS Webhook 規格

### 6.1 App 送出 payload

App 以 `application/json; charset=utf-8` POST：

```json
{
  "to": "family@example.com",
  "subject": "Alive Please check-in",
  "body": "message text"
}
```

### 6.2 App 端成功判斷

- HTTP 非 2xx 視為失敗。
- HTTP 成功時：
  - 若 response body 是 JSON 且 `ok` 為 `false`，視為失敗。
  - 若 response body 不是 JSON，預設視為成功。
  - 若 JSON 內有 `error` 或 `message`，會帶回給呼叫端。

### 6.3 GAS 端 GET

`GET /exec` 回傳 healthcheck JSON：

```json
{
  "ok": true,
  "service": "alive-please-webhook",
  "message": "Webhook is running."
}
```

若 `LOG_HEALTHCHECKS` 開啟，會寫入 usage log。

### 6.4 GAS 端 POST

GAS 端會：

- 可選擇用 `?token=` 搭配 `WEBHOOK_TOKEN` 驗證。
- 驗證 body 必須為 JSON。
- 驗證 `to`、`subject`、`body` 必填。
- 驗證 `to` 為 Email 格式。
- 限制 subject 長度最大 200。
- 限制 body 長度最大 20000。
- 預設不允許 HTML；若 `ALLOW_HTML` 開啟，會同時設定 `htmlBody` 並產生純文字 fallback。
- 使用 `MailApp.sendEmail()` 寄送。
- 回傳 JSON 成功或失敗資訊。

### 6.5 Usage log

若 `USAGE_LOG_SHEET_ID` 有值，GAS 會寫入或建立 `USAGE_LOG_SHEET_NAME` 工作表，欄位如下：

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

事件類型包含：

- `healthcheck`
- `mail_sent`
- `mail_error`
- `test_mail_sent`
- `usage_log_failed`

## 7. 驗證與測試現況

目前測試涵蓋：

- 家人通知是否應排程：
  - 未打卡即使有 Email 也不排程。
  - 已打卡但無 Email 不排程。
  - 已打卡且有 Email 會排程。
- 安靜時段預設為開啟，23:00 到 07:00。
- 跨日安靜時段判斷。
- 同日安靜時段判斷。
- 安靜時段關閉時不調整觸發時間。
- 安靜時段內觸發會延後到結束時間。
- 開始與結束相同代表全天安靜，並延後到下一個相同結束時間。
- 倒數格式在超過一天時包含秒數與零值單位。

建議後續補測：

- `performCheckIn()` 同一天重複打卡不增加累積天數。
- 連續天數遇到缺口時中斷。
- 家人通知 worker 的成功、失敗、重試流程。
- Webhook response `ok=false` 與非 JSON body 的處理。
- 設定儲存時 Email、時間、小數等待時間的驗證。
- BootReceiver 重新排程行為。

## 8. 已知限制與待確認事項

- 內建 GAS Webhook URL 存在於 App 程式碼，是否應改為空值、建置變數或使用者必填，待確認。
- 家人通知會使用 Email 做逾時通知；目前沒有 SMS、通訊軟體、推播給家人等替代渠道。
- 打卡提醒是固定週期 WorkManager，未明確記錄「下一次打卡提醒」時間。
- 家人通知不套用安靜時段；目前只有照護提醒與打卡提醒會避開安靜時段。
- `alive_days` 以不同日期打卡數計算，不等同於從首次打卡到現在的日曆天數。
- 打卡日期依裝置當地時間產生；跨時區行為未另行定義。
