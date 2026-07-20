# 在线词库发布示例

本目录包含可直接发布的 v4 在线词库：

- `idioms-v4.json`：完整词库，共 5003 条；在线词库每次发布的是完整文件，不是增量补丁。
- `manifest.json`：App 首先读取的版本清单。

## 首次部署

1. 将 `idioms-v4.json` 上传到支持 HTTPS 的静态文件服务。
2. 把 `manifest.json` 的 `dictionaryUrl` 改为该文件的公开 HTTPS 地址。
3. 如果修改过词库，重新计算 SHA-256，并同步更新 `sha256` 与 `entryCount`。
4. 上传 `manifest.json`，复制它的公开 HTTPS 地址。
5. 在工程根目录 `gradle.properties` 中设置：

   ```properties
   IDIOM_DICTIONARY_MANIFEST_URL=https://abrikk.github.io/idiom-dictionary/manifest.json
   ```

6. 重新构建并发布 App。此后手机用户不需要手动导入词库。

macOS、Linux 可使用下面的命令计算校验值：

```text
sha256sum idioms-v4.json
```

Windows PowerShell 可使用：

```text
Get-FileHash .\idioms-v4.json -Algorithm SHA256
```

## 后续发布词库

1. 复制上一版完整词库，添加或修订成语，保持已有 `id` 不变。
2. 将 `version` 增加 1，更新日期、条目数和 SHA-256。
3. 先上传新的词库 JSON，确认其 URL 可访问。
4. 最后上传新的 `manifest.json`，避免用户拿到清单后却下载不到词库。

App 打开或回到前台时每 12 小时最多自动检查一次，也可在“我的 → 立即检查更新”中手动检查。只有版本号更高时才下载；校验或解析失败会保留手机中原有词库。

## manifest 字段

- `schemaVersion`：清单格式版本，当前固定为 `1`。
- `version`：词库版本，必须为正整数且每次发布递增。
- `updatedAt`：面向用户显示的更新时间。
- `dictionaryUrl`：完整词库 JSON 的 HTTPS 地址。
- `sha256`：词库文件的 SHA-256，小写十六进制字符串。
- `entryCount`：完整词库的条目数。
- `minAppVersion`：可读取此词库的最低 Android `versionCode`。
