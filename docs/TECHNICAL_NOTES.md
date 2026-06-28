# Alive Please 技術筆記

## 技術總覽

| 技術 | 用途 | 選擇原因 |
| --- | --- | --- |
| Kotlin 1.9、Android SDK 35 | Android App 開發 | 專案原生平台與既有語言；最低支援 API 24 |
| Jetpack Compose、Material 3 | UI | 沿用 Android 宣告式 UI 與 Material 元件 |
| Navigation Compose、HorizontalPager | 頁面導覽 | 使用 Compose 原生導覽與分頁元件 |
| ViewModel | UI 狀態 | 將狀態與操作移出 Composable |
| SharedPreferences | 本機資料 | 資料量與查詢需求小，不需要資料庫或雲端帳號 |
| WorkManager | 背景工作 | 處理可延後、需要網路或需要重試的工作 |
| AlarmManager | 到期喚醒 | 補足 WorkManager 不保證準時執行的限制 |
| OkHttp、Google Apps Script | 寄送 Email | 沒有自建後端，以簡單 Webhook 完成寄信 |
| JUnit、Robolectric、WorkManager Test、MockK | JVM 測試 | 不依賴實機驗證本機與背景邏輯 |

## Android 應用架構

App 採單一 `MainActivity` 與 Compose。Navigation Compose 管理路由，主要分頁使用 `HorizontalPager`；`MainViewModel` 與 `SettingsViewModel` 管理 UI 狀態，持久化集中在 `AppDataStore`。

`AppDataStore` 是專案封裝，底層實際使用 `SharedPreferences`，不是 Jetpack DataStore。首次導覽完成狀態曾因非同步寫入而可能被重複判定，因此改用同步 `commit()`；一般設定仍使用 `apply()`。

## SharedPreferences 與時間模型

資料量與查詢需求都很小，因此沒有引入資料庫。親友清單以 JSON 陣列保存，讀取時保留舊版單一收件人欄位的相容處理。

通知期限使用 Unix epoch 毫秒計算經過時間，不受時區切換影響；每日紀錄則依裝置當下時區格式化為 `yyyy-MM-dd`，日期歸屬會跟著裝置時區。安靜時段使用 `Calendar`，並另外處理跨午夜與起訖時間相同的情況。

## WorkManager 與 AlarmManager

WorkManager 適合保證工作最終有機會執行，但不保證在指定分鐘執行。最初只使用 WorkManager 時，Doze、省電限制或廠商背景管控可能讓通知延遲到 App 再次開啟。

目前由 AlarmManager 的 `setAndAllowWhileIdle()` 負責較接近期限的本機喚醒，再交由 WorkManager 執行需要網路的寄信。專案沒有要求精確鬧鐘權限，因此仍不承諾分鐘級準時。

重新開機、App 更新與 App 啟動時會重建排程。Receiver 與 Worker 執行前重新檢查最新狀態，避免舊排程直接執行。

到期提醒與寄信結果原本會形成兩則通知，後來改用相同通知 ID 更新內容，避免重複打擾。

## 親友通知與冪等處理

開啟 App 會重建排程，過去因此可能重複寄信。現在以每次打卡時間作為通知週期 ID，並記錄該週期已成功的收件人；部分成功時只重試尚未完成的項目。

只靠本機紀錄仍無法處理「信已寄出但網路回應遺失」，所以 App 會傳送穩定的 `requestId`。GAS 使用 Script Lock 避免同時處理相同請求，並以 Script Properties 保存 180 天內已寄送的 ID；重複請求回報成功但不再寄信。

Worker 在寄送前後確認週期是否仍是最新值，避免舊請求結果寫入新週期。

## Google Apps Script Webhook

專案沒有自建後端，因此以 OkHttp 呼叫 GAS，再由 `MailApp.sendEmail()` 寄信。這讓內建服務與自行部署共用同一個 Webhook 介面，不需要額外的後端 SDK。

GAS 在信任邊界驗證欄位長度與 Email 格式，並支援選用的 `WEBHOOK_TOKEN`。寄送紀錄可寫入 Google Sheet，供確認實際執行結果。

## Android 系統限制

Android 13 以上的通知權限可能在排程後被撤銷，因此 `NotificationHelper` 攔截 `SecurityException`，避免背景工作崩潰。

手機關機、長時間離線、強制停止 App 或系統省電限制都可能延後本機排程。寄信依賴裝置取得背景執行與網路時間；若要在手機關機時仍能寄信，期限與排程必須移到持續運作的雲端服務。

## 測試與建置

目前以 JUnit 與 Robolectric 驗證本機資料、時間判斷、排程接收器、Worker 與防重複寄送。實際省電策略仍需在不同廠牌手機測試。

Release 簽章資料由 `local.properties` 讀取，不寫入版本控制；四個簽章欄位都存在時才建立 release signing config。