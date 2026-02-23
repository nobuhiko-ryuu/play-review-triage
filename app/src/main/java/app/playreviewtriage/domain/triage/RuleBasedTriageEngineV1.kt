package app.playreviewtriage.domain.triage

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleBasedTriageEngineV1 @Inject constructor() : TriageEngine {

    override fun evaluate(text: String, rating: Int): TriageResult {
        val lower = text.lowercase()

        // LOW（NOISE）: テキストが3文字以下
        if (text.length <= 3) {
            return TriageResult(importance = Importance.LOW, tags = setOf(ReasonTag.NOISE))
        }

        val tags = mutableSetOf<ReasonTag>()

        // HIGH（CRASH）判定
        val isCrash = CRASH_KEYWORDS.any { lower.contains(it) }
        if (isCrash) tags.add(ReasonTag.CRASH)

        // HIGH（BILLING）判定
        val isBilling = BILLING_KEYWORDS.any { lower.contains(it) }
        if (isBilling) tags.add(ReasonTag.BILLING)

        if (tags.isNotEmpty()) {
            return TriageResult(importance = Importance.HIGH, tags = tags)
        }

        // MID（UI）判定
        val isUi = UI_KEYWORDS.any { lower.contains(it) }
        if (isUi) {
            return TriageResult(importance = Importance.MID, tags = setOf(ReasonTag.UI))
        }

        // デフォルト判定
        return if (rating <= 2) {
            TriageResult(importance = Importance.MID, tags = setOf(ReasonTag.OTHER))
        } else {
            TriageResult(importance = Importance.LOW, tags = setOf(ReasonTag.OTHER))
        }
    }

    companion object {
        private val CRASH_KEYWORDS = listOf(
            // 日本語
            "クラッシュ",
            "落ちる",
            "起動できない",
            "立ち上がらない",
            "起動しない",
            "動かない",
            "開かない",
            "フリーズ",
            // 英語
            "crash",
            "anr",
            "freeze",
            "not opening",
            "won't open",
            "won't start",
            "force close",
        )

        private val BILLING_KEYWORDS = listOf(
            // 日本語
            "課金",
            "購入",
            "支払い",
            "返金",
            "サブスク",
            "決済",
            // 英語
            "billing",
            "purchase",
            "payment",
            "refund",
            "subscription",
            "charge",
        )

        private val UI_KEYWORDS = listOf(
            // 日本語
            "使いにくい",
            "見づらい",
            "追加して",
            "欲しい",
            "要望",
            "改善",
            "デザイン",
            "操作しづらい",
            // 英語
            "improve",
            "request",
            "wish",
            "ux",
            "ui",
            "design",
            "feature",
        )
    }
}
