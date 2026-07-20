package com.idiombloom.app.data

data class Idiom(
    val id: String,
    val text: String,
    val pinyin: String,
    val tone: IdiomTone,
    val meaning: String,
    val note: String,
    val dailyExample: String,
    val literaryExample: String,
    val literarySource: String,
    val formalExample: String,
    val formalSource: String,
    val tags: List<String>,
)

enum class IdiomTone(val jsonValue: String, val title: String) {
    Positive("positive", "褒义"),
    Neutral("neutral", "中性"),
    Negative("negative", "贬义"),
    Unmarked("unmarked", "常用");

    companion object {
        fun fromJson(value: String): IdiomTone =
            entries.firstOrNull { it.jsonValue == value } ?: Unmarked
    }
}
