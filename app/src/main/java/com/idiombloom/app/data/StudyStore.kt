package com.idiombloom.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ReviewOutcome { Forgot, Fuzzy, Remembered }

private const val MASTERED_REPETITIONS = 5
private const val MASTERED_INTERVAL_DAYS = 30

private val genericRelationTags = setOf(
    "常用成语",
    "高频常用",
    "易错成语",
    "易读错",
    "易写错",
    "望文生义",
    "褒贬误用",
    "对象误用",
    "谦敬误用",
    "近义辨析",
)

private val commonMeaningBigrams = setOf(
    "比喻", "形容", "表示", "用来", "常用", "多指", "泛指", "指人", "指事",
    "一种", "事情", "事物", "样子", "意思", "不能", "没有", "的人", "极其",
)

private val oppositeConcepts = listOf(
    "成功" to "失败", "前进" to "后退", "得到" to "失去", "勤奋" to "懒惰",
    "勇敢" to "胆怯", "团结" to "分裂", "真诚" to "虚伪", "赞扬" to "批评",
    "富有" to "贫穷", "强大" to "弱小", "安全" to "危险", "开始" to "结束",
    "赞成" to "反对", "快乐" to "悲伤", "光明" to "黑暗", "进步" to "退步",
    "节俭" to "浪费", "冷静" to "慌乱", "谦虚" to "骄傲", "有序" to "混乱",
)

data class ReviewState(
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.30,
    val lapseCount: Int = 0,
    val nextReviewAtMillis: Long = 0L,
    val lastReviewAtMillis: Long? = null,
    val firstLearnedAtMillis: Long? = null,
)

data class DailyStudyRecord(
    val dayKey: String,
    val learnedCount: Int,
    val reviewedCount: Int,
)

data class StudySnapshot(
    val records: Map<String, ReviewState> = emptyMap(),
    val favoriteIds: Set<String> = emptySet(),
    val completedIdsByDay: Map<String, Set<String>> = emptyMap(),
    val learnedIdsByDay: Map<String, Set<String>> = emptyMap(),
    val reviewedIdsByDay: Map<String, Set<String>> = emptyMap(),
    val dailyNewGoal: Int = 20,
    val autoReviewEnabled: Boolean = true,
    val dailyReviewGoal: Int = 20,
    val streak: Int = 0,
    val lastStudyDayKey: String? = null,
)

class StudyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "idiom_bloom_study",
        Context.MODE_PRIVATE,
    )
    private val zoneId: ZoneId = ZoneId.systemDefault()

    var state by mutableStateOf(loadSnapshot())
        private set

    val todayCompletedCount: Int
        get() = todayLearnedCount + todayReviewedCount

    val todayLearnedCount: Int
        get() = todayLearnedIds.size

    val todayReviewedCount: Int
        get() = todayReviewedIds.size

    val todayLearnedIds: Set<String>
        get() = state.learnedIdsByDay[dayKey(System.currentTimeMillis())].orEmpty()

    val todayReviewedIds: Set<String>
        get() = state.reviewedIdsByDay[dayKey(System.currentTimeMillis())].orEmpty()

    val allReviewedIds: Set<String>
        get() = state.reviewedIdsByDay.values.flatten().toSet()

    val allCompletedIds: Set<String>
        get() = state.completedIdsByDay.values.flatten().toSet()

    val totalReviewCount: Int
        get() = state.reviewedIdsByDay.values.sumOf { it.size }

    val displayedStreak: Int
        get() {
            val last = state.lastStudyDayKey ?: return 0
            val today = LocalDate.now(zoneId)
            return if (last == today.toString() || last == today.minusDays(1).toString()) {
                state.streak
            } else {
                0
            }
        }

    fun isFavorite(idiom: Idiom): Boolean = idiom.id in state.favoriteIds

    fun learnedCount(idioms: List<Idiom>): Int = idioms.count { idiom ->
        state.records[idiom.id]?.firstLearnedAtMillis != null
    }

    fun learnedIds(): Set<String> = state.records
        .filterValues { it.firstLearnedAtMillis != null }
        .keys

    fun masteredCount(idioms: List<Idiom>): Int = idioms.count { idiom ->
        state.records[idiom.id]?.isMastered() == true
    }

    fun masteredIds(): Set<String> = state.records
        .filterValues { it.isMastered() }
        .keys

    fun isReviewItem(idiom: Idiom): Boolean =
        state.records[idiom.id]?.firstLearnedAtMillis != null

    fun dueIds(
        idioms: List<Idiom>,
        now: Long = System.currentTimeMillis(),
    ): Set<String> = idioms.asSequence()
        .filter { idiom ->
            state.records[idiom.id]?.let { review ->
                review.firstLearnedAtMillis != null && review.nextReviewAtMillis <= now
            } == true
        }
        .map { it.id }
        .toSet()

    fun dueCount(idioms: List<Idiom>, now: Long = System.currentTimeMillis()): Int =
        idioms.count { idiom ->
            state.records[idiom.id]?.let { review ->
                review.firstLearnedAtMillis != null && review.nextReviewAtMillis <= now
            } == true
        }

    fun todayReviewPlanCount(
        idioms: List<Idiom>,
        now: Long = System.currentTimeMillis(),
    ): Int {
        val reviewed = todayReviewedIds.size
        val remainingCandidates = reviewCandidates(idioms, now)
        return if (state.autoReviewEnabled) {
            reviewed + remainingCandidates.count { idiom ->
                state.records[idiom.id]?.nextReviewAtMillis?.let { it <= now } == true
            }
        } else {
            minOf(state.dailyReviewGoal, reviewed + remainingCandidates.size)
        }
    }

    fun todayRemainingReviewCount(
        idioms: List<Idiom>,
        now: Long = System.currentTimeMillis(),
    ): Int = (todayReviewPlanCount(idioms, now) - todayReviewedCount).coerceAtLeast(0)

    fun makeDailyStudyQueue(
        idioms: List<Idiom>,
        now: Long = System.currentTimeMillis(),
    ): List<Idiom> {
        val remainingNewCount = (state.dailyNewGoal - todayLearnedCount).coerceAtLeast(0)
        val newCandidates = idioms.filter { idiom ->
            state.records[idiom.id]?.firstLearnedAtMillis == null
        }
        val newItems = makeSmartDailyOrder(
            candidates = newCandidates,
            slotCount = remainingNewCount,
            day = dayKey(now),
        ).take(remainingNewCount)

        val candidates = reviewCandidates(idioms, now)
        val reviewItems = if (state.autoReviewEnabled) {
            candidates.filter { idiom ->
                state.records[idiom.id]?.nextReviewAtMillis?.let { it <= now } == true
            }
        } else {
            candidates.take((state.dailyReviewGoal - todayReviewedCount).coerceAtLeast(0))
        }
        return interleaveStudyItems(reviewItems, newItems)
    }

    fun recentHistory(days: Int = 7, now: Long = System.currentTimeMillis()): List<DailyStudyRecord> {
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return (0 until days).map { offset ->
            val key = today.minusDays(offset.toLong()).toString()
            DailyStudyRecord(
                dayKey = key,
                learnedCount = state.learnedIdsByDay[key]?.size ?: 0,
                reviewedCount = state.reviewedIdsByDay[key]?.size ?: 0,
            )
        }
    }

    fun setDailyNewGoal(value: Int) {
        update(state.copy(dailyNewGoal = value.coerceIn(5, 100)))
    }

    fun setAutoReviewEnabled(enabled: Boolean) {
        update(state.copy(autoReviewEnabled = enabled))
    }

    fun setDailyReviewGoal(value: Int) {
        update(state.copy(dailyReviewGoal = value.coerceIn(5, 100)))
    }

    fun toggleFavorite(idiom: Idiom) {
        val favorites = state.favoriteIds.toMutableSet().apply {
            if (!add(idiom.id)) remove(idiom.id)
        }
        update(state.copy(favoriteIds = favorites))
    }

    fun review(
        idiom: Idiom,
        outcome: ReviewOutcome,
        now: Long = System.currentTimeMillis(),
    ) {
        val old = state.records[idiom.id] ?: ReviewState()
        var snapshot = state
        val wasLearned = old.firstLearnedAtMillis != null || old.repetitions > 0

        val reviewed = when (outcome) {
            ReviewOutcome.Forgot -> old.copy(
                repetitions = 0,
                intervalDays = 0,
                easeFactor = max(1.30, old.easeFactor - 0.20),
                lapseCount = old.lapseCount + 1,
                nextReviewAtMillis = now + 10 * 60 * 1000L,
                lastReviewAtMillis = now,
            )

            ReviewOutcome.Fuzzy -> {
                val nextReview = Instant.ofEpochMilli(now)
                    .atZone(zoneId)
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

                if (wasLearned) snapshot = markReviewed(snapshot, idiom.id, now)
                snapshot = markCompleted(snapshot, idiom.id, now)
                if (!wasLearned) {
                    snapshot = markLearned(snapshot, idiom.id, now)
                }
                old.copy(
                    repetitions = max(1, old.repetitions),
                    intervalDays = 1,
                    easeFactor = max(1.30, old.easeFactor - 0.08),
                    nextReviewAtMillis = nextReview,
                    lastReviewAtMillis = now,
                    firstLearnedAtMillis = old.firstLearnedAtMillis ?: now,
                )
            }

            ReviewOutcome.Remembered -> {
                val repetitions = old.repetitions + 1
                val interval = when (repetitions) {
                    1 -> 1
                    2 -> 3
                    else -> max(4, (max(1, old.intervalDays) * old.easeFactor).roundToInt())
                }
                val nextReview = Instant.ofEpochMilli(now)
                    .atZone(zoneId)
                    .toLocalDate()
                    .plusDays(interval.toLong())
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

                if (wasLearned) snapshot = markReviewed(snapshot, idiom.id, now)
                snapshot = markCompleted(snapshot, idiom.id, now)
                if (!wasLearned) {
                    snapshot = markLearned(snapshot, idiom.id, now)
                }
                old.copy(
                    repetitions = repetitions,
                    intervalDays = interval,
                    easeFactor = min(2.80, old.easeFactor + 0.06),
                    nextReviewAtMillis = nextReview,
                    lastReviewAtMillis = now,
                    firstLearnedAtMillis = old.firstLearnedAtMillis ?: now,
                )
            }
        }

        update(snapshot.copy(records = snapshot.records + (idiom.id to reviewed)))
    }

    fun resetProgress() {
        update(
            state.copy(
                records = emptyMap(),
                completedIdsByDay = emptyMap(),
                learnedIdsByDay = emptyMap(),
                reviewedIdsByDay = emptyMap(),
                streak = 0,
                lastStudyDayKey = null,
            )
        )
    }

    private fun markCompleted(
        snapshot: StudySnapshot,
        idiomId: String,
        now: Long,
    ): StudySnapshot {
        val today = dayKey(now)
        val completedToday = snapshot.completedIdsByDay[today].orEmpty().toMutableSet()
        if (!completedToday.add(idiomId)) return snapshot

        val completed = snapshot.completedIdsByDay.toMutableMap().apply {
            this[today] = completedToday
        }
        if (snapshot.lastStudyDayKey == today) {
            return snapshot.copy(completedIdsByDay = completed)
        }

        val yesterday = Instant.ofEpochMilli(now)
            .atZone(zoneId)
            .toLocalDate()
            .minusDays(1)
            .toString()
        return snapshot.copy(
            completedIdsByDay = completed,
            streak = if (snapshot.lastStudyDayKey == yesterday) snapshot.streak + 1 else 1,
            lastStudyDayKey = today,
        )
    }

    private fun markLearned(snapshot: StudySnapshot, idiomId: String, now: Long): StudySnapshot {
        val today = dayKey(now)
        val values = snapshot.learnedIdsByDay[today].orEmpty().toMutableSet()
        if (!values.add(idiomId)) return snapshot
        return snapshot.copy(
            learnedIdsByDay = snapshot.learnedIdsByDay + (today to values),
        )
    }

    private fun markReviewed(snapshot: StudySnapshot, idiomId: String, now: Long): StudySnapshot {
        val today = dayKey(now)
        val values = snapshot.reviewedIdsByDay[today].orEmpty().toMutableSet()
        if (!values.add(idiomId)) return snapshot
        return snapshot.copy(
            reviewedIdsByDay = snapshot.reviewedIdsByDay + (today to values),
        )
    }

    private fun update(snapshot: StudySnapshot) {
        state = snapshot
        preferences.edit().putString("snapshot", snapshot.toJson().toString()).apply()
    }

    private fun loadSnapshot(): StudySnapshot {
        val raw = preferences.getString("snapshot", null) ?: return StudySnapshot()
        return runCatching { JSONObject(raw).toSnapshot() }.getOrDefault(StudySnapshot())
    }

    private fun dayKey(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toString()

    private fun reviewCandidates(idioms: List<Idiom>, now: Long): List<Idiom> {
        val reviewedToday = todayReviewedIds
        return idioms.asSequence()
            .filter { idiom ->
                val review = state.records[idiom.id]
                review?.firstLearnedAtMillis != null && idiom.id !in reviewedToday
            }
            .sortedWith(
                compareBy<Idiom> { idiom ->
                    val dueAt = state.records[idiom.id]?.nextReviewAtMillis ?: Long.MAX_VALUE
                    if (dueAt <= now) 0 else 1
                }.thenBy { idiom ->
                    state.records[idiom.id]?.nextReviewAtMillis ?: Long.MAX_VALUE
                }.thenBy { idiom ->
                    stableScore(dayKey(now), idiom.id)
                }
            )
            .toList()
    }

    private fun interleaveStudyItems(
        reviewItems: List<Idiom>,
        newItems: List<Idiom>,
    ): List<Idiom> = buildList(reviewItems.size + newItems.size) {
        var reviewIndex = 0
        var newIndex = 0
        while (reviewIndex < reviewItems.size || newIndex < newItems.size) {
            if (reviewIndex < reviewItems.size) add(reviewItems[reviewIndex++])
            repeat(2) {
                if (newIndex < newItems.size) add(newItems[newIndex++])
            }
        }
    }

    private fun makeSmartDailyOrder(
        candidates: List<Idiom>,
        slotCount: Int,
        day: String,
    ): List<Idiom> {
        if (candidates.isEmpty()) return emptyList()
        val randomized = candidates.sortedBy { stableScore(day, it.id) }
        if (slotCount < 2 || randomized.size < 2) return randomized

        val targetGroupSize = minOf(
            2 + (stableScore(day, "smart-group-size") % 3L).toInt(),
            slotCount,
            randomized.size,
        )
        val searchPool = randomized.take(minOf(1200, randomized.size))
        val tokenCache = searchPool.associate { idiom ->
            idiom.id to meaningBigrams(idiom.meaning + idiom.note)
        }

        var bestGroup = emptyList<Idiom>()
        var bestGroupScore = 0
        randomized.take(minOf(16, randomized.size)).forEach { anchor ->
            val anchorTokens = tokenCache[anchor.id].orEmpty()
            val related = searchPool.asSequence()
                .filter { it.id != anchor.id }
                .map { candidate ->
                    candidate to relationScore(
                        anchor = anchor,
                        candidate = candidate,
                        anchorTokens = anchorTokens,
                        candidateTokens = tokenCache[candidate.id].orEmpty(),
                    )
                }
                .filter { it.second >= 6 }
                .sortedWith(
                    compareByDescending<Pair<Idiom, Int>> { it.second }
                        .thenBy { stableScore(day, it.first.id) }
                )
                .take(targetGroupSize - 1)
                .toList()

            if (related.isNotEmpty()) {
                val score = related.sumOf { it.second }
                if (score > bestGroupScore) {
                    bestGroupScore = score
                    bestGroup = listOf(anchor) + related.map { it.first }
                }
            }
        }

        if (bestGroup.size < 2) return randomized
        val groupIds = bestGroup.mapTo(mutableSetOf()) { it.id }
        val remaining = randomized.filter { it.id !in groupIds }
        val latestInsertion = (slotCount - bestGroup.size).coerceAtLeast(0)
        val insertion = if (latestInsertion == 0) {
            0
        } else {
            (stableScore(day, "smart-group-position") % (latestInsertion + 1L)).toInt()
        }
        return remaining.take(insertion) + bestGroup + remaining.drop(insertion)
    }

    private fun relationScore(
        anchor: Idiom,
        candidate: Idiom,
        anchorTokens: Set<String>,
        candidateTokens: Set<String>,
    ): Int {
        val anchorTags = anchor.tags.filterNot { it in genericRelationTags }.toSet()
        val candidateTags = candidate.tags.filterNot { it in genericRelationTags }.toSet()
        val sharedTags = anchorTags.intersect(candidateTags).size
        val sharedMeaning = anchorTokens.intersect(candidateTokens).size
        val anchorText = anchor.text
        val candidateText = candidate.text
        val isOpposite = oppositeConcepts.any { (left, right) ->
            (left in anchorText && right in candidateText) ||
                (right in anchorText && left in candidateText)
        }
        return sharedTags * 6 + minOf(sharedMeaning, 5) * 2 + if (isOpposite) 10 else 0
    }

    private fun meaningBigrams(value: String): Set<String> {
        val characters = value.filter { it.code in 0x4E00..0x9FFF }
        if (characters.length < 2) return emptySet()
        return characters.windowed(2)
            .filterNot { it in commonMeaningBigrams }
            .toSet()
    }

    private fun stableScore(day: String, value: String): Long {
        var hash = 1125899906842597L
        "$day|$value".forEach { character ->
            hash = hash * 31L + character.code
        }
        return hash and Long.MAX_VALUE
    }
}

private fun StudySnapshot.toJson(): JSONObject = JSONObject().apply {
    put("dailyNewGoal", dailyNewGoal)
    put("autoReviewEnabled", autoReviewEnabled)
    put("dailyReviewGoal", dailyReviewGoal)
    put("streak", streak)
    put("lastStudyDayKey", lastStudyDayKey ?: JSONObject.NULL)

    put("favoriteIds", JSONArray().apply {
        favoriteIds.forEach { put(it) }
    })

    put("records", JSONObject().apply {
        records.forEach { (id, review) ->
            put(id, JSONObject().apply {
                put("repetitions", review.repetitions)
                put("intervalDays", review.intervalDays)
                put("easeFactor", review.easeFactor)
                put("lapseCount", review.lapseCount)
                put("nextReviewAtMillis", review.nextReviewAtMillis)
                put("lastReviewAtMillis", review.lastReviewAtMillis ?: JSONObject.NULL)
                put("firstLearnedAtMillis", review.firstLearnedAtMillis ?: JSONObject.NULL)
            })
        }
    })

    put("completedIdsByDay", JSONObject().apply {
        completedIdsByDay.forEach { (day, ids) ->
            put(day, JSONArray().apply { ids.forEach { put(it) } })
        }
    })

    put("learnedIdsByDay", learnedIdsByDay.toJson())
    put("reviewedIdsByDay", reviewedIdsByDay.toJson())
}

private fun Map<String, Set<String>>.toJson(): JSONObject = JSONObject().apply {
    this@toJson.forEach { (day, ids) ->
        put(day, JSONArray().apply { ids.forEach { put(it) } })
    }
}

private fun JSONObject.toSnapshot(): StudySnapshot {
    val records = mutableMapOf<String, ReviewState>()
    optJSONObject("records")?.let { objectValue ->
        val keys = objectValue.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val review = objectValue.getJSONObject(id)
            records[id] = ReviewState(
                repetitions = review.optInt("repetitions"),
                intervalDays = review.optInt("intervalDays"),
                easeFactor = review.optDouble("easeFactor", 2.30),
                lapseCount = review.optInt("lapseCount"),
                nextReviewAtMillis = review.optLong("nextReviewAtMillis"),
                lastReviewAtMillis = if (review.isNull("lastReviewAtMillis")) {
                    null
                } else {
                    review.optLong("lastReviewAtMillis")
                },
                firstLearnedAtMillis = if (review.has("firstLearnedAtMillis")) {
                    if (review.isNull("firstLearnedAtMillis")) null else review.optLong("firstLearnedAtMillis")
                } else {
                    review.optLong("lastReviewAtMillis").takeIf { review.optInt("repetitions") > 0 }
                },
            )
        }
    }

    val favorites = mutableSetOf<String>()
    optJSONArray("favoriteIds")?.let { values ->
        for (index in 0 until values.length()) favorites += values.getString(index)
    }

    val completed = readIdMap("completedIdsByDay")
    val learned = readIdMap("learnedIdsByDay")
    val reviewed = readIdMap("reviewedIdsByDay")

    return StudySnapshot(
        records = records,
        favoriteIds = favorites,
        completedIdsByDay = completed,
        learnedIdsByDay = learned,
        reviewedIdsByDay = reviewed,
        dailyNewGoal = if (has("dailyNewGoal")) {
            optInt("dailyNewGoal", 20)
        } else {
            optInt("dailyGoal", 20)
        }.coerceIn(5, 100),
        autoReviewEnabled = optBoolean("autoReviewEnabled", true),
        dailyReviewGoal = optInt("dailyReviewGoal", 20).coerceIn(5, 100),
        streak = optInt("streak"),
        lastStudyDayKey = if (isNull("lastStudyDayKey")) null else optString("lastStudyDayKey"),
    )
}

private fun ReviewState.isMastered(): Boolean =
    repetitions >= MASTERED_REPETITIONS && intervalDays >= MASTERED_INTERVAL_DAYS

private fun JSONObject.readIdMap(key: String): Map<String, Set<String>> {
    val result = mutableMapOf<String, Set<String>>()
    optJSONObject(key)?.let { objectValue ->
        val keys = objectValue.keys()
        while (keys.hasNext()) {
            val day = keys.next()
            val values = objectValue.getJSONArray(day)
            result[day] = buildSet {
                for (index in 0 until values.length()) add(values.getString(index))
            }
        }
    }
    return result
}
