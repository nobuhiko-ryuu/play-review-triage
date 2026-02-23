package app.playreviewtriage.domain.triage

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedTriageEngineV1Test {

    private lateinit var engine: RuleBasedTriageEngineV1

    @Before
    fun setUp() {
        engine = RuleBasedTriageEngineV1()
    }

    @Test
    fun `クラッシュキーワードを含むテキストはHIGHかつCRASHタグになる`() {
        val result = engine.evaluate("アプリがクラッシュして使えません", rating = 1)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.CRASH))
    }

    @Test
    fun `英語クラッシュキーワードを含むテキストはHIGHかつCRASHタグになる`() {
        val result = engine.evaluate("The app keeps crashing every time I open it", rating = 2)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.CRASH))
    }

    @Test
    fun `課金キーワードを含むテキストはHIGHかつBILLINGタグになる`() {
        val result = engine.evaluate("課金したのに機能が使えません", rating = 1)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.BILLING))
    }

    @Test
    fun `英語課金キーワードを含むテキストはHIGHかつBILLINGタグになる`() {
        val result = engine.evaluate("I was charged twice for the same subscription", rating = 1)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.BILLING))
    }

    @Test
    fun `UI改善キーワードを含むテキストはMIDかつUIタグになる`() {
        val result = engine.evaluate("使いにくいので改善してほしいです", rating = 3)
        assertEquals(Importance.MID, result.importance)
        assertTrue(result.tags.contains(ReasonTag.UI))
    }

    @Test
    fun `英語UI改善キーワードを含むテキストはMIDかつUIタグになる`() {
        val result = engine.evaluate("Please improve the design of the settings screen", rating = 3)
        assertEquals(Importance.MID, result.importance)
        assertTrue(result.tags.contains(ReasonTag.UI))
    }

    @Test
    fun `3文字以下のテキストはLOWかつNOISEタグになる`() {
        val result = engine.evaluate("うーん", rating = 1)
        assertEquals(Importance.LOW, result.importance)
        assertTrue(result.tags.contains(ReasonTag.NOISE))
    }

    @Test
    fun `空文字に近い1文字テキストはLOWかつNOISEタグになる`() {
        val result = engine.evaluate("a", rating = 1)
        assertEquals(Importance.LOW, result.importance)
        assertTrue(result.tags.contains(ReasonTag.NOISE))
    }

    @Test
    fun `クラッシュと課金両方のキーワードを含む場合はHIGHかつCRASHとBILLINGの両タグになる`() {
        val result = engine.evaluate("アプリがクラッシュして課金が取られたまま返ってこない", rating = 1)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.CRASH))
        assertTrue(result.tags.contains(ReasonTag.BILLING))
        assertEquals(2, result.tags.size)
    }

    @Test
    fun `クラッシュキーワードはratingが高くてもHIGHになる`() {
        val result = engine.evaluate("最高のアプリだけど最近クラッシュするようになった", rating = 5)
        assertEquals(Importance.HIGH, result.importance)
        assertTrue(result.tags.contains(ReasonTag.CRASH))
    }

    @Test
    fun `rating2以下でキーワードなしはMIDかつOTHERタグになる`() {
        val result = engine.evaluate("あまり良くないと思います", rating = 2)
        assertEquals(Importance.MID, result.importance)
        assertTrue(result.tags.contains(ReasonTag.OTHER))
    }

    @Test
    fun `rating3以上でキーワードなしはLOWかつOTHERタグになる`() {
        val result = engine.evaluate("まあまあ使えるアプリです", rating = 4)
        assertEquals(Importance.LOW, result.importance)
        assertTrue(result.tags.contains(ReasonTag.OTHER))
    }
}
