with open(r'C:\Users\seewell\AndroidStudioProjects\AutoWallpaperChanger\app\src\main\java\com\autowallpaper\changer\presentation\screens\library\LibraryScreen.kt', 'r', 'utf-8') as f:
    content = f.read()

old_text = '    var debugInfo by remember { mutableStateOf<String?>("Debug: 等待載入...") }\n    var previewOnlineImage by remember { mutableStateOf<GalleryApiService.GalleryImage?>(null) }\n\n    // 載入線上圖庫分類\n    LaunchedEffect(Unit) {'

new_text = '    var debugInfo by remember { mutableStateOf<String?>(null) }\n    var previewOnlineImage by remember { mutableStateOf<GalleryApiService.GalleryImage?>(null) }\n\n    // 資料夾選擇器（用於自動新增下載資料夾）\n    val autoWallpaperFolderPicker = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.OpenDocumentTree()\n    ) { uri: Uri? ->\n        uri?.let {\n            viewModel.addFolder(it)\n            viewModel.reloadImages()\n        }\n    }\n\n    // 載入線上圖庫分類\n    LaunchedEffect(Unit) {'

if old_text in content:
    content = content.replace(old_text, new_text)
    with open(r'C:\Users\seewell\AndroidStudioProjects\AutoWallpaperChanger\app\src\main\java\com\autowallpaper\changer\presentation\screens\library\LibraryScreen.kt', 'w', 'utf-8') as f:
        f.write(content)
    print('Done')
else:
    print('Text not found - checking encoding...')
    # Try to find similar text
    idx = content.find('var debugInfo')
    if idx >= 0:
        print(repr(content[idx:idx+200]))
