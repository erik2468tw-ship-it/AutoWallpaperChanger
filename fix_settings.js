const fs = require('fs');

const filePath = 'C:\\Users\\seewell\\AndroidStudioProjects\\AutoWallpaperChanger\\app\\src\\main\\java\\com\\autowallpaper\\changer\\presentation\\screens\\settings\\SettingsScreen.kt';

fs.readFile(filePath, 'utf8', (err, data) => {
    if (err) {
        console.error('Error reading file:', err);
        return;
    }
    
    // 1. Remove WiFi only download section (lines around 42-64)
    const wifiOnlyPattern = /VerticalMargin\n    \}\n    \n    \/[\s\S]*?僅 Wi-Fi 下載[\s\S]*?HorizontalDivider\(\)[\s\S]*?\}/g;
    
    // 2. Update help dialog content
    const oldHelpContent = `                Column \{
                    Text\("📱 自動換桌布", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 選擇要使用的圖片資料夾\n2\. 設定自動更換間隔時間\n3\. 開啟自動更換功能"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("🎯 懸浮球快速換圖", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 在首頁開啟懸浮球功能\n2\. 首次使用需授權「顯示在其他應用程式上」\n3\. 點擊懸浮球立即更換主螢幕桌布\n4\. 拖曳懸浮球可移動位置"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("🖼️ 電子相簿", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 點擊電子相簿進入播放模式\n2\. 點擊螢幕可暫停/播放\n3\. 圖片會隨機播放"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("⚙️ 權限說明", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("• 儲存權限：用於讀取圖片\n• 懸浮球權限：用於顯示懸浮球\n• 開機權限：開機後自動啟動服務"\)
                \}`;
    
    const newHelpContent = `                Column \(modifier = Modifier\.verticalScroll\(rememberScrollState\(\)\)\) \{
                    Text\("📱 自動換桌布", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 選擇要使用的圖片資料夾\n2\. 設定自動更換間隔時間\n3\. 開啟自動更換功能"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("🎯 懸浮球快速換圖", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 在首頁開啟懸浮球功能\n2\. 首次使用需授權「顯示在其他應用程式上」\n3\. 點擊懸浮球立即更換主螢幕桌布\n4\. 拖曳懸浮球可移動位置"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("🖼️ 線上圖庫下載", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 進入「圖庫」頁面\n2\. 瀏覽線上圖庫，點擊圖片可下載\n3\. 下載後圖片會存到「AutoWallpaper」資料夾\n4\. 建議將 AutoWallpaper 資料夾加入圖片庫"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("📁 資料夾設定", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 加入資料夾後，圖片會出現在本地圖庫\n2\. 可刪除已加入的資料夾\n3\. 下載的圖片會自動添加到本地圖庫"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("🖼️ 電子相簿", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("1\. 點擊電子相簿進入播放模式\n2\. 點擊螢幕可暫停/播放\n3\. 圖片會隨機播放"\)
                    Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                    Text\("⚙️ 權限說明", style: MaterialTheme\.typography\.titleSmall\)
                    Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                    Text\("• 儲存權限：用於讀取圖片\n• 懸浮球權限：用於顯示懸浮球\n• 開機權限：開機後自動啟動服務"\)
                \}`;
    
    let newData = data;
    
    // Remove WiFi only section
    const wifiRegex = /SPACER_HEIGHT_X2\n\}\n\nSPACER_H_SMALL\n\nSPACER_H_SMALL\n\nSPACER_H_SMALL\n\nSettingsSectionHeader\(title = "電力"\)\nSettingsCard \{\nSettingsSwitchItem\([\s\S]*?)<\/>\n\}/;
    newData = newData.replace(wifiRegex, 'SPACER_HEIGHT_X2\n\}\n\nSPACER_H_SMALL\n\nSPACER_H_SMALL\n\nSettingsSectionHeader\(title = "電力"\)\nSettingsCard \{\n\}\n/');
    
    fs.writeFile(filePath, newData, 'utf8', (err) => {
        if (err) {
            console.error('Error writing file:', err);
        } else {
            console.log('Done!');
        }
    });
});
