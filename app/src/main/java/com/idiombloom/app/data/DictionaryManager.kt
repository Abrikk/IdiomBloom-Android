package com.idiombloom.app.data

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.idiombloom.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class DictionarySource(val title: String) {
    Bundled("内置词库"),
    Online("在线词库"),
}

data class DictionaryInfo(
    val version: Int,
    val updatedAt: String,
    val entryCount: Int,
    val source: DictionarySource,
    val lastCheckedAtMillis: Long?,
)

sealed interface DictionaryUpdateStatus {
    data object Idle : DictionaryUpdateStatus
    data object Checking : DictionaryUpdateStatus
    data class Message(val text: String, val isError: Boolean = false) : DictionaryUpdateStatus
}

class DictionaryManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dictionaryDirectory = File(appContext.filesDir, "dictionary")
    private val activeDictionaryFile = File(dictionaryDirectory, "idioms.json")
    private val activeDictionary = AtomicFile(activeDictionaryFile)
    private val bundledMeta = loadBundledMeta()
    private val initialDictionary = loadInitialDictionary()
    private val defaultManifestUrl = BuildConfig.DICTIONARY_MANIFEST_URL.trim()

    var idioms by mutableStateOf(initialDictionary.idioms)
        private set

    var info by mutableStateOf(initialDictionary.info)
        private set

    var status by mutableStateOf<DictionaryUpdateStatus>(DictionaryUpdateStatus.Idle)
        private set

    var autoUpdateEnabled by mutableStateOf(preferences.getBoolean(KEY_AUTO_UPDATE, true))
        private set

    var manifestUrl by mutableStateOf(
        if (preferences.contains(KEY_CUSTOM_URL)) {
            preferences.getString(KEY_CUSTOM_URL, "").orEmpty()
        } else {
            defaultManifestUrl
        }
    )
        private set

    fun updateManifestUrl(value: String) {
        manifestUrl = value.trim()
        preferences.edit().putString(KEY_CUSTOM_URL, manifestUrl).apply()
        status = when {
            manifestUrl.isBlank() -> {
                DictionaryUpdateStatus.Message("尚未配置在线词库地址。", isError = true)
            }

            !manifestUrl.startsWith("https://", ignoreCase = true) -> {
                DictionaryUpdateStatus.Message("更新地址必须使用 HTTPS。", isError = true)
            }

            else -> DictionaryUpdateStatus.Message("更新地址已保存。")
        }
    }

    fun useDefaultManifestUrl() {
        preferences.edit().remove(KEY_CUSTOM_URL).apply()
        manifestUrl = defaultManifestUrl
        status = if (manifestUrl.isBlank()) {
            DictionaryUpdateStatus.Message("工程尚未设置默认更新地址。", isError = true)
        } else {
            DictionaryUpdateStatus.Message("已恢复默认更新地址。")
        }
    }

    fun updateAutoUpdateEnabled(enabled: Boolean) {
        autoUpdateEnabled = enabled
        preferences.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
    }

    suspend fun autoCheckIfNeeded() {
        if (!autoUpdateEnabled || manifestUrl.isBlank()) return
        val lastCheck = preferences.getLong(KEY_LAST_CHECK_AT, 0L)
        if (checkedRecently(lastCheck)) return
        checkForUpdates(manual = false)
    }

    suspend fun checkForUpdates(manual: Boolean = true) {
        if (status is DictionaryUpdateStatus.Checking) return
        val source = manifestUrl.trim()
        if (!source.startsWith("https://", ignoreCase = true)) {
            status = DictionaryUpdateStatus.Message(
                if (source.isBlank()) "尚未配置在线词库地址。" else "更新地址必须使用 HTTPS。",
                isError = true,
            )
            return
        }

        if (!manual) {
            val lastCheck = preferences.getLong(KEY_LAST_CHECK_AT, 0L)
            if (checkedRecently(lastCheck)) return
        }

        status = DictionaryUpdateStatus.Checking
        val checkedAt = System.currentTimeMillis()
        val currentVersion = info.version
        val outcome = withContext(Dispatchers.IO) {
            runCatching { performUpdateCheck(source, currentVersion) }
                .getOrElse { CheckOutcome.Failed(readableError(it)) }
        }

        preferences.edit().putLong(KEY_LAST_CHECK_AT, checkedAt).apply()
        info = info.copy(lastCheckedAtMillis = checkedAt)

        when (outcome) {
            is CheckOutcome.UpToDate -> {
                status = DictionaryUpdateStatus.Message("词库已是最新版本（v${outcome.version}）。")
            }

            is CheckOutcome.Updated -> {
                idioms = outcome.idioms
                info = DictionaryInfo(
                    version = outcome.manifest.version,
                    updatedAt = outcome.manifest.updatedAt,
                    entryCount = outcome.idioms.size,
                    source = DictionarySource.Online,
                    lastCheckedAtMillis = checkedAt,
                )
                status = DictionaryUpdateStatus.Message(
                    "词库已更新至 v${outcome.manifest.version}，共 ${outcome.idioms.size} 条。"
                )
            }

            is CheckOutcome.Failed -> {
                status = DictionaryUpdateStatus.Message(outcome.message, isError = true)
            }
        }
    }

    fun restoreBundledDictionary() {
        if (status is DictionaryUpdateStatus.Checking) return
        clearLocalDictionary()
        idioms = IdiomRepository.loadBundled(appContext)
        info = DictionaryInfo(
            version = bundledMeta.version,
            updatedAt = bundledMeta.updatedAt,
            entryCount = idioms.size,
            source = DictionarySource.Bundled,
            lastCheckedAtMillis = preferences.getLong(KEY_LAST_CHECK_AT, 0L).takeIf { it > 0 },
        )
        status = DictionaryUpdateStatus.Message("已恢复内置词库。")
    }

    private fun performUpdateCheck(manifestUrl: String, currentVersion: Int): CheckOutcome {
        val manifestBytes = downloadHttps(manifestUrl, MAX_MANIFEST_BYTES)
        val manifest = parseManifest(manifestBytes.toString(Charsets.UTF_8))
        if (manifest.minAppVersion > BuildConfig.VERSION_CODE) {
            return CheckOutcome.Failed("此词库需要更新版本的 App。")
        }
        if (manifest.version <= currentVersion) {
            return CheckOutcome.UpToDate(currentVersion)
        }

        val dictionaryBytes = downloadHttps(manifest.dictionaryUrl, MAX_DICTIONARY_BYTES)
        val actualSha256 = dictionaryBytes.sha256()
        if (!actualSha256.equals(manifest.sha256, ignoreCase = true)) {
            return CheckOutcome.Failed("词库完整性校验失败，已保留原词库。")
        }

        val parsed = try {
            IdiomRepository.parse(dictionaryBytes.toString(Charsets.UTF_8))
        } catch (_: Exception) {
            return CheckOutcome.Failed("下载的词库格式无效，已保留原词库。")
        }
        if (manifest.entryCount != null && manifest.entryCount != parsed.size) {
            return CheckOutcome.Failed("词库条目数量与发布信息不一致，已保留原词库。")
        }

        dictionaryDirectory.mkdirs()
        val output = activeDictionary.startWrite()
        try {
            output.write(dictionaryBytes)
            output.flush()
            activeDictionary.finishWrite(output)
        } catch (error: Exception) {
            activeDictionary.failWrite(output)
            throw error
        }

        val saved = preferences.edit()
            .putInt(KEY_LOCAL_VERSION, manifest.version)
            .putString(KEY_LOCAL_UPDATED_AT, manifest.updatedAt)
            .putInt(KEY_LOCAL_ENTRY_COUNT, parsed.size)
            .putString(KEY_LOCAL_SHA256, actualSha256)
            .commit()
        if (!saved) throw IOException("Unable to save dictionary metadata.")
        return CheckOutcome.Updated(manifest, parsed)
    }

    private fun parseManifest(raw: String): RemoteManifest {
        val json = JSONObject(raw)
        require(json.optInt("schemaVersion") == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported dictionary schema."
        }
        val version = json.getInt("version")
        val dictionaryUrl = json.getString("dictionaryUrl").trim()
        val sha256 = json.getString("sha256").trim().lowercase()
        require(version > 0) { "Invalid dictionary version." }
        require(dictionaryUrl.startsWith("https://", ignoreCase = true)) {
            "Dictionary URL must use HTTPS."
        }
        require(SHA256_PATTERN.matches(sha256)) { "Invalid SHA-256 value." }
        return RemoteManifest(
            version = version,
            updatedAt = json.optString("updatedAt", ""),
            dictionaryUrl = dictionaryUrl,
            sha256 = sha256,
            entryCount = json.optInt("entryCount", -1).takeIf { it >= 0 },
            minAppVersion = json.optInt("minAppVersion", 1),
        )
    }

    private fun loadInitialDictionary(): LoadedDictionary {
        val lastChecked = preferences.getLong(KEY_LAST_CHECK_AT, 0L).takeIf { it > 0 }
        if (activeDictionaryFile.isFile || preferences.contains(KEY_LOCAL_VERSION)) {
            val onlineIdioms = runCatching {
                val dictionaryBytes = activeDictionary.openRead().use { it.readBytes() }
                val expectedSha256 = preferences.getString(KEY_LOCAL_SHA256, "").orEmpty()
                require(expectedSha256.isBlank() || dictionaryBytes.sha256() == expectedSha256) {
                    "Dictionary checksum mismatch."
                }
                IdiomRepository.parse(dictionaryBytes.toString(Charsets.UTF_8))
            }.getOrNull()
            if (!onlineIdioms.isNullOrEmpty()) {
                return LoadedDictionary(
                    idioms = onlineIdioms,
                    info = DictionaryInfo(
                        version = preferences.getInt(KEY_LOCAL_VERSION, bundledMeta.version + 1),
                        updatedAt = preferences.getString(KEY_LOCAL_UPDATED_AT, "").orEmpty(),
                        entryCount = onlineIdioms.size,
                        source = DictionarySource.Online,
                        lastCheckedAtMillis = lastChecked,
                    ),
                )
            }
            clearLocalDictionary()
        }

        val bundledIdioms = IdiomRepository.loadBundled(appContext)
        return LoadedDictionary(
            idioms = bundledIdioms,
            info = DictionaryInfo(
                version = bundledMeta.version,
                updatedAt = bundledMeta.updatedAt,
                entryCount = bundledIdioms.size,
                source = DictionarySource.Bundled,
                lastCheckedAtMillis = lastChecked,
            ),
        )
    }

    private fun loadBundledMeta(): BundledMeta {
        val raw = appContext.assets.open("dictionary_meta.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val json = JSONObject(raw)
        return BundledMeta(
            version = json.optInt("version", 1),
            updatedAt = json.optString("updatedAt", ""),
        )
    }

    private fun downloadHttps(url: String, maxBytes: Int): ByteArray {
        val requestedUrl = URL(url)
        if (!requestedUrl.protocol.equals("https", ignoreCase = true)) {
            throw IOException("Only HTTPS is allowed.")
        }
        val connection = (requestedUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "IdiomBloom/${BuildConfig.VERSION_NAME}")
        }
        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) throw IOException("HTTP $statusCode")
            if (!connection.url.protocol.equals("https", ignoreCase = true)) {
                throw IOException("Insecure redirect is not allowed.")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > maxBytes) throw IOException("Download is too large.")
            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw IOException("Download is too large.")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readableError(error: Throwable): String = when (error) {
        is java.net.SocketTimeoutException -> "连接超时，已保留原词库。"
        is java.net.UnknownHostException -> "无法连接更新服务器，已保留原词库。"
        else -> "词库更新失败，已保留原词库。"
    }

    private fun checkedRecently(lastCheck: Long): Boolean {
        val elapsed = System.currentTimeMillis() - lastCheck
        return elapsed in 0 until AUTO_CHECK_INTERVAL_MILLIS
    }

    private fun clearLocalDictionary() {
        activeDictionary.delete()
        preferences.edit()
            .remove(KEY_LOCAL_VERSION)
            .remove(KEY_LOCAL_UPDATED_AT)
            .remove(KEY_LOCAL_ENTRY_COUNT)
            .remove(KEY_LOCAL_SHA256)
            .apply()
    }

    private data class BundledMeta(val version: Int, val updatedAt: String)

    private data class LoadedDictionary(val idioms: List<Idiom>, val info: DictionaryInfo)

    private data class RemoteManifest(
        val version: Int,
        val updatedAt: String,
        val dictionaryUrl: String,
        val sha256: String,
        val entryCount: Int?,
        val minAppVersion: Int,
    )

    private sealed interface CheckOutcome {
        data class UpToDate(val version: Int) : CheckOutcome
        data class Updated(val manifest: RemoteManifest, val idioms: List<Idiom>) : CheckOutcome
        data class Failed(val message: String) : CheckOutcome
    }

    private companion object {
        const val PREFERENCES_NAME = "idiom_bloom_dictionary"
        const val KEY_AUTO_UPDATE = "auto_update"
        const val KEY_CUSTOM_URL = "custom_manifest_url"
        const val KEY_LAST_CHECK_AT = "last_check_at"
        const val KEY_LOCAL_VERSION = "local_version"
        const val KEY_LOCAL_UPDATED_AT = "local_updated_at"
        const val KEY_LOCAL_ENTRY_COUNT = "local_entry_count"
        const val KEY_LOCAL_SHA256 = "local_sha256"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val MAX_MANIFEST_BYTES = 128 * 1024
        const val MAX_DICTIONARY_BYTES = 10 * 1024 * 1024
        const val AUTO_CHECK_INTERVAL_MILLIS = 12 * 60 * 60 * 1000L
        val SHA256_PATTERN = Regex("^[a-f0-9]{64}$")
    }
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
