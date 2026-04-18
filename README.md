# Auto Wallpaper - 自動換桌布

Android 自動更換桌布 App，定時更換手機桌布並支援線上圖庫下載。

## 版本資訊

- **當前版本：** v1.3.0
- **version_code：** 34
- **更新日期：** 2026-04-19

## 功能特色

### 🖼️ 自動換桌布
- 定时自动更换手机桌布
- 支援設定更換間隔（30分鐘～24小時）
- 可設定更換時間區間

### 📱 本地圖庫
- 支援選擇本地資料夾作為桌布來源
- 圖片預覽功能
- 可直接將圖片設為主螢幕或鎖屏

### 🌐 線上圖庫
- 四層分類：風格類 / 場景類 / 主題類 / 各國風景
- 雙層下拉選單操作
- 下載圖片到本地 AutoWallpaper 資料夾

### 📊 圖庫分類

| 大分類 | 小分類 |
|--------|--------|
| **風格類** | fantasy, sci-fi, dark, cute, vintage, minimalist |
| **場景類** | landscape, cityscape, sunset, ocean, mountain, forest, starry |
| **主題類** | portrait, couple, animal, food, car, phone |
| **各國風景** | taiwan, japan, korea, thailand, usa, canada, switzerland, newzealand, iceland, norway, australia, italy, greece, china, mongolia, singapore, malaysia, indonesia, philippines, myanmar, cambodia, laos, brunei |

### ⚡ 其他功能
- 快速更換按鈕（一鍵換桌布）
- 統計數據顯示（可用圖片數、已選資料夾）
- 自動排程管理

## 版本更新機制

1. **版本檢查：** APP 啟動時自動檢查更新
2. **安全機制：** 如果被阻擋，每次重開 APP 都會重新確認線上版本
3. **避免鎖死：** 如果後台設定錯誤（退版），使用者仍可正常使用

## API 設定

- **API URL：** http://203.222.24.35:3000/
- **版本檢查：** `/api/version/check?app=autowallpaper&version_code=34`

## 編譯 APK

```bash
# 在 AndroidStudioProjects/AutoWallpaperChanger 目錄執行
./gradlew assembleDebug

# APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 技術栈

- **語言：** Kotlin
- **UI：** Jetpack Compose
- **架構：** MVVM + Hilt
- **背景任務：** WorkManager

## 專案結構

```
AutoWallpaperChanger/
├── app/src/main/java/com/autowallpaper/changer/
│   ├── MainActivity.kt
│   ├── presentation/
│   │   ├── screens/
│   │   │   ├── home/        # 首頁
│   │   │   ├── library/     # 圖庫
│   │   │   └── settings/    # 設定
│   │   └── components/      # 共用元件
│   ├── service/             # API 和 Worker
│   ├── data/                # 資料和 Preferences
│   └── domain/              # 商業邏輯
└── app/src/main/res/       # 資源檔案
```

## GitHub

- **Repository：** https://github.com/erik2468tw-ship-it/AutoWallpaperChanger
- **後端：** https://github.com/erik2468tw-ship-it/wallpaper-backend