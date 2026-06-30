# Alive Please

Alive Please 是一個以 Android 製作的報平安 App。

它的核心目的很簡單：
讓使用者每天按一下「我還活著」完成打卡，並在太久沒有報平安時，自動通知指定的親友。

## 下載正式版

使用者可以直接從 Google Play 商店下載正式版 App：
[Alive Please on Google Play](https://play.google.com/store/apps/details?id=com.orenhui.aliveplease)

## 功能

- 每日報平安打卡
- 連續打卡天數與累積天數顯示
- 每 3 天一次的鼓勵祝福提示
- 可自訂打卡提醒間隔
- 可設定親友 Email 與稱呼
- 支援 GAS Webhook 寄信
- 太久未打卡時自動通知親友
- 支援開機後重新恢復提醒與通知排程
- 內建設定導覽與測試寄信流程

## 畫面展示

<img src="./screenshots/home.png" alt="Home" width="320" />

## 使用提醒

Alive Please 會在你的裝置本機保存打卡紀錄、提醒設定與親友 Email。
若你啟用親友通知，App 會在太久未打卡時，透過設定的寄信服務送出通知 Email。

請確認親友 Email 與通知內容是你願意提供給寄信服務處理的資訊。

## 專案狀態

這是一個公開的 Android 專案，主要用來展示 Alive Please 的功能、介面與實作方式。
詳細規格可參考 `docs/SPEC.md`。
