# IdiomBloom — Android 成语学习 App

IdiomBloom 是一款使用 Kotlin 和 Jetpack Compose 开发的 Android 成语学习应用，支持间隔重复、遗忘曲线复习、易错成语、中文语音朗读、离线词库及在线更新。

IdiomBloom is an open-source Chinese idiom learning app for Android, featuring spaced repetition, offline dictionaries, pronunciation, learning statistics, and vocabulary updates.

## 系统要求

- 最低版本：Android 8.0，API 26
- 目标版本：Android 16，API 36
- 开发工具：Android Studio、JDK 17、Android SDK 36

## 已实现功能

- 成语、拼音、感情色彩、释义、易错提示和两类例句
- 系统中文语音朗读
- “记错了 / 我会了”学习反馈
- 记错的成语间隔数题后在本轮重新出现
- 按 1 天、3 天及动态间隔安排后续复习
- 今日进度、连续学习天数、待复习和已学习统计
- 成语词库搜索
- 高频常用及八类易错专题筛选
- 收藏和收藏列表
- 启动时自动检查在线词库，支持手动检查与关闭自动更新
- 下载前校验版本、SHA-256、JSON 格式和条目数，失败时保留原词库
- 在线词库更新后界面立即刷新，无需重启 App
- 手机和平板自适应宽度
- 本地持久化及完全离线使用

## 运行

1. 在 Android Studio 中打开本目录。
2. 等待 Gradle 同步完成；首次同步会下载构建依赖。
3. 安装 Android SDK 36，并选择 API 26 以上的模拟器或设备。
4. 运行 `app` 配置。

也可以在 Windows 终端运行：

```text
gradlew.bat assembleDebug
```

生成的调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 使用 JSON 扩充成语词库

词库文件为：

```text
app/src/main/assets/idioms.json
```

文件最外层是 JSON 数组，每个成语使用如下格式：

```json
{
  "id": "shuo-guo-lei-lei",
  "text": "硕果累累",
  "pinyin": "shuò guǒ léi léi",
  "tone": "positive",
  "meaning": "本指果实繁多，常比喻取得了很多显著成果。",
  "note": "“累累”在这里读 léi léi。",
  "example": "经过几年的潜心研究，团队硕果累累。",
  "contextExample": "成果展呈现了同学们一年来硕果累累的创作实践。",
  "tags": ["成果", "褒义", "易错读音"]
}
```

字段说明：

- `id`：稳定且唯一的英文标识。发布后不要随意修改，否则原有学习记录无法对应。
- `text`：成语正文。
- `pinyin`：带声调的拼音。
- `tone`：`positive`、`neutral` 或 `negative`。
- `meaning`：释义。
- `note`：易错字、易错读音或使用提示，可以为空字符串。
- `example`：普通例句。
- `contextExample`：语境例句，可以为空字符串。
- `tags`：用于搜索和分类的标签数组。

添加记录时，在前一条记录结尾加入逗号，再追加新的 JSON 对象。只要字段名称和 JSON 格式正确，App 下次构建时会自动载入，不需要修改 Kotlin 代码。

## 配置自动在线更新

自动更新需要一个可公开访问的 HTTPS 静态文件地址。工程已包含手机端更新逻辑和一套可部署示例，位于：

```text
server-example/
```

首次发布 App 前，在工程根目录的 `gradle.properties` 填写远程清单地址：

```properties
IDIOM_DICTIONARY_MANIFEST_URL=https://abrikk.github.io/idiom-dictionary/manifest.json
```

然后重新构建 APK 或 AAB。地址会写入安装包，普通手机用户无需配置：打开或回到 App 时会自动检查，每 12 小时最多一次；也可以进入“我的”页面立即检查更新。

如果构建时没有预置地址，可在 App 的“我的 → 更新来源”中填写 `manifest.json` 的 HTTPS 地址用于测试。正式版建议预置，避免让普通用户处理技术地址。

在线发布流程：

1. 准备一份完整的新词库 JSON，而不是只包含新增成语。
2. 计算词库文件的 SHA-256。
3. 更新 `manifest.json` 中的版本、日期、下载地址、校验值和条目数。
4. 先上传词库 JSON，最后上传 `manifest.json`。

完整字段和发布步骤见 `server-example/README.md`。

## 数据说明

内置及在线 v4 词库共含 5003 条，其中4950条来自常用度筛选、1000条标记为高频常用，168条标记为易错成语。易错专题包括易读错、易写错、望文生义、褒贬误用、对象误用、谦敬误用和近义辨析。学习状态使用 Android `SharedPreferences` 保存；在线词库写入 App 私有目录。数据来源及许可见 `THIRD_PARTY_NOTICES.md`。正式商用前仍建议逐条复核内容和例句授权。
