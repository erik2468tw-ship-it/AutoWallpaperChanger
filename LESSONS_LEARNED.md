# AutoWallpaperChanger 開發錯誤記錄

## 踩過的坑

### 1. 圖片格式問題（2026-04-10）
**錯誤：** 使用 TIFF 格式圖片作為 Android 資源
**症狀：** 圖片無法顯示，沒有任何錯誤
**原因：** Android 不支援 TIFF 格式
**解決：** 轉換成 PNG 或 JPG 格式
**預防：** 
- Android 支援的圖片格式：PNG、JPG、WebP、GIF、BMP
- 放在 `res/drawable/` 而非 `res/raw/`（PNG/JPG 可直接引用）

---

### 2. Activity 佈局錯誤（2026-04-10）
**錯誤：** SlideshowActivity 使用 `activity_splash.xml` 而非專屬佈局
**症狀：** 電子相簿顯示的是開機畫面內容
**原因：** 複製程式碼時忘記修改 `setContentView(R.layout.xxx)`
**解決：** 建立專屬的 `activity_slideshow.xml`
**預防：**
- 每個 Activity 有自己的佈局檔案
- 建立新 Activity 時立即建立對應的佈局

---

### 3. 忘記在 Hilt AndroidEntryPoint（2026-04-09）
**錯誤：** 在 Activity/Fragment 使用 `@Inject` 但忘記加 `@AndroidEntryPoint`
**症狀：** 執行時錯誤：`EntryPoint not found`
**解決：** 在 class 前加 `@AndroidEntryPoint`
**預防：** 使用 `@Inject` 的地方一定要有 `@AndroidEntryPoint`

---

### 4. Compose UI 放在 XML 佈局（2026-04-09）
**錯誤：** 建立 Compose Activity 後仍使用 `setContentView(R.layout.xxx)`
**症狀：** 只顯示 XML 內容，Compose UI 不會出現
**解決：** 使用 `setContent { ... }` 而不是 `setContentView()`
**預防：**
- Compose Activity：使用 `setContent { }`
- XML Activity：使用 `setContentView(R.layout.xxx)`
- 不要混用

---

### 5. SAF URI 儲存與讀取（2026-04-09）
**錯誤：** 直接儲存 Uri 字串到 DataStore，讀取時無法使用
**原因：** SAF URI 有时效性，且格式可能變化
**解決：** 
- 儲存：`uri.toString()`
- 讀取：`Uri.parse(string)`
- 使用時確保有 `ContentResolver.takePersistableUriPermission()`
**預防：** 每次使用 SAF URI 前都要處理可能的無效情況

---

### 6. WorkManager 初始化忘記 HiltWorkerFactory（2026-04-08）
**錯誤：** 沒有注入 HiltWorkerFactory 導致 Worker 無法使用 Hilt
**症狀：** Worker 中的 @Inject 依賴都會是 null
**解決：** 在 Configuration.Provider 中正確設定 workerFactory
**正確範例：**
```kotlin
@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

---

### 7. Handler.postDelayed 時間單位（2026-04-10）
**錯誤：** 把分鐘當秒鐘用，設 30 分鐘結果 30 秒就結束
**原因：** `Handler.postDelayed` 的第二個參數是毫秒
**解決：** `30 * 60 * 1000L` 才是 30 分鐘
**預防：** 單位要搞清楚：1秒 = 1000毫秒

---

### 8. DataStore 預設值與類型（2026-04-09）
**錯誤：** 讀取 Int 類型和 String 類型 key 時搞混
**解決：** 清楚區分 `intPreferencesKey`、`stringPreferencesKey`、`booleanPreferencesKey`
**預防：** 統一管理所有 DataStore Key，集中在一個 object 或 companion object

---

### 9. Kotlin 不可變動集合（2026-04-09）
**錯誤：** 直接修改 `Flow<List<String>>` 的內容
**語法：** 使用 `.toMutableList()` 或 `.toMutableSet()` 轉換
**預防：** Kotlin 的 List/Set/Map 預設是唯讀的，需要可變版本要明確轉換

---

### 10. 忘記加入 Permission 請求（2026-04-08）
**錯誤：** 使用 READ_EXTERNAL_STORAGE 但忘記在 AndroidManifest 宣告
**解決：** 
- Manifest：`<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>`
- 程式碼：使用 ActivityResultContracts 請求
**預防：** 先確認需要哪些權限，再實作請求流程

---

## 開發原則

1. **每個功能立即測試** - 不要一次寫太多功能
2. **命名一致** - 避免 activity_splash、activity_slideshow 混用
3. **單位確認** - 時間（毫秒/秒/分）、大小（KB/MB/GB）
4. **格式支援** - Android 資源格式要確認支援清單
5. **依賴注入** - @Inject 一定要配合 @AndroidEntryPoint
6. **Compose vs XML** - 確認 UI 框架，不要混用
7. **錯誤日誌** - 發生錯誤時先看 Logcat 輸出

---

## 專案結構建議

```
app/src/main/
├── java/com/autowallpaper/changer/
│   ├── data/
│   │   ├── local/          # ImageScanner, FileUtils
│   │   ├── preferences/    # DataStore
│   │   └── repository/     # Repository 實作
│   ├── di/                 # Hilt Module
│   ├── domain/
│   │   ├── model/          # Data class
│   │   └── usecase/        # UseCase
│   ├── presentation/
│   │   ├── components/      # 共用 Compose 元件
│   │   ├── screens/        # 各頁面 Activity/Composable
│   │   │   ├── home/
│   │   │   ├── schedule/
│   │   │   ├── slideshow/
│   │   │   └── splash/
│   │   └── theme/          # Compose Theme
│   └── service/            # WorkManager, Service
└── res/
    ├── drawable/            # 圖片資源（PNG/JPG）
    ├── layout/             # XML 佈局
    ├── raw/                # 音訊檔案
    └── values/             # 字串、顏色
```
