# 氢桌面增强版 (HydroDesktop Enhanced)

> 车机桌面启动器 + AI 助手 + 语音控制

## ✨ 功能特性

### 🏠 车机桌面
- 全屏沉浸式界面，适配车机横屏
- 大按钮设计，驾驶场景友好
- 实时时钟、日期、天气显示
- 一键启动导航、音乐、电话
- 支持开机自启为默认桌面

### 🤖 AI 助手
- 对话式 AI 聊天界面
- 支持多种大语言模型 API：
  - DeepSeek
  - OpenAI (ChatGPT)
  - 通义千问
  - 本地 Ollama
  - 其他 OpenAI 兼容接口
- 对话历史记忆
- 自定义系统提示词
- 语音输入支持

### 🎤 语音控制
- 持续语音监听（前台服务）
- 自定义唤醒词（默认：你好氢桌面）
- 语音指令解析：
  - "导航去XXX" → 启动地图导航
  - "播放音乐" → 控制音乐播放
  - "调大音量" → 调节系统音量
  - "打电话" → 拨打电话
  - "几点了" → 语音播报时间
  - "今天天气" → 天气查询
  - 其他问题 → 交给 AI 回答
- TTS 语音播报回复

### ⚙️ 设置中心
- AI API 地址/密钥/模型配置
- 语音唤醒词设置
- 屏幕亮度调节
- 媒体音量调节
- 开机自启开关

## 📦 技术栈

- **语言**: Java
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 34)
- **构建工具**: Gradle 8.2 + AGP 8.2.0
- **网络库**: OkHttp 4.12
- **JSON 解析**: Gson 2.10
- **UI**: Material Components + ConstraintLayout
- **语音识别**: Android SpeechRecognizer API
- **语音合成**: Android TextToSpeech API

## 🚀 使用方法

### 1. 导入 Android Studio
1. 解压 `HydroDesktop-Enhanced.zip`
2. 打开 Android Studio → `File` → `Open`
3. 选择解压后的项目文件夹
4. 等待 Gradle 同步完成

### 2. 配置 AI API
1. 运行应用后点击右下角「设置」
2. 在「AI 模型配置」区域填入：
   - **API 地址**: 你的 AI 服务地址（如 `https://api.deepseek.com/v1/chat/completions`）
   - **API 密钥**: 你的 API Key（可选，视服务商要求）
   - **模型名称**: 如 `deepseek-chat`、`gpt-3.5-turbo` 等
3. 点击「测试连接」验证配置

### 3. 设置语音控制
1. 在设置中开启「语音唤醒」
2. 可自定义唤醒词（默认：你好氢桌面）
3. 开启「语音播报」获取 TTS 回复

### 4. 设为默认桌面
1. 按 Home 键
2. 选择「氢桌面增强版」作为默认启动器

## 🔧 自定义开发

### 修改 AI 服务
编辑 `app/src/main/java/com/hydrodesktop/util/AIHelper.java`：
- 修改 `DEFAULT_API_URL` 更换默认 API
- 修改 `DEFAULT_SYSTEM_PROMPT` 自定义 AI 人格

### 添加语音指令
编辑 `app/src/main/java/com/hydrodesktop/voice/VoiceRecognitionService.java`：
- 在 `processVoiceCommand()` 方法中添加新的指令匹配

### 修改界面
- 主界面布局: `res/layout/activity_main.xml`
- AI 聊天界面: `res/layout/activity_ai_assistant.xml`
- 设置界面: `res/layout/activity_settings.xml`
- 主题颜色: `res/values/colors.xml`

## 📋 注意事项

1. **网络权限**: AI 功能需要网络连接
2. **麦克风权限**: 语音功能需要录音权限
3. **车机适配**: 建议在 1024×600 或更高分辨率的车机上使用
4. **API 费用**: 使用第三方 AI API 可能产生费用，请注意用量
5. **安全驾驶**: 语音控制优先，减少手动操作

## 📄 许可证

本项目基于原「氢桌面」概念进行二次开发，仅供学习和个人使用。
