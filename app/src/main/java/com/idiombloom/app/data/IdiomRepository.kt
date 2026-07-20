package com.idiombloom.app.data

import android.content.Context
import org.json.JSONArray

object IdiomRepository {
    fun loadBundled(context: Context): List<Idiom> {
        val raw = context.assets.open("idioms.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parse(raw)
    }

    fun parse(raw: String): List<Idiom> {
        val array = JSONArray(raw)
        require(array.length() > 0) { "The dictionary must contain at least one idiom." }
        val ids = mutableSetOf<String>()

        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.getString("id").trim()
                require(id.isNotEmpty() && ids.add(id)) { "Invalid or duplicate idiom id: $id" }
                val text = item.getString("text").trim()
                val pinyin = item.getString("pinyin").trim()
                val meaning = item.getString("meaning").trim()
                val legacyExample = normalizedExample(item.optString("example"))
                val legacyContextExample = normalizedExample(item.optString("contextExample"))
                val legacyWrittenExample = normalizedExample(item.optString("writtenExample"))
                val dailyExample = normalizedExample(item.optString("dailyExample"))
                    .ifBlank {
                        legacyExample.ifBlank { dailyFallback(text) }
                    }
                val literaryExample = normalizedExample(item.optString("literaryExample"))
                    .ifBlank { legacyWrittenExample }
                    .ifBlank { legacyContextExample.ifBlank { legacyExample } }
                    .ifBlank { literaryFallback(text) }
                val literarySource = normalizedExample(item.optString("literarySource"))
                    .ifBlank { "词库暂未收录可靠出处" }
                val formalExample = normalizedExample(item.optString("formalExample"))
                    .ifBlank { formalFallback(text) }
                val formalSource = normalizedExample(item.optString("formalSource"))
                    .ifBlank { "词库暂未收录可靠出处" }
                require(text.isNotEmpty() && pinyin.isNotEmpty() && meaning.isNotEmpty()) {
                    "Required idiom fields cannot be empty: $id"
                }
                val tagArray = item.optJSONArray("tags") ?: JSONArray()
                val tags = buildList(tagArray.length()) {
                    for (tagIndex in 0 until tagArray.length()) {
                        add(tagArray.getString(tagIndex))
                    }
                }
                add(
                    Idiom(
                        id = id,
                        text = text,
                        pinyin = pinyin,
                        tone = IdiomTone.fromJson(item.optString("tone", "neutral")),
                        meaning = meaning,
                        note = item.optString("note"),
                        dailyExample = dailyExample,
                        literaryExample = literaryExample,
                        literarySource = literarySource,
                        formalExample = formalExample,
                        formalSource = formalSource,
                        tags = tags,
                    )
                )
            }
        }
    }

    private fun normalizedExample(value: String): String = value.trim()
        .takeUnless { it in setOf("无", "无。", "暂无", "暂无。") }
        .orEmpty()

    private fun dailyFallback(idiom: String): String =
        "“$idiom”的可靠日常语境例句暂未收录，请等待词库更新。"

    private fun literaryFallback(idiom: String): String =
        "“$idiom”的可核验文学例句暂未收录。"

    private fun formalFallback(idiom: String): String =
        "“$idiom”的可核验新闻、杂志或报刊例句暂未收录。"
}
