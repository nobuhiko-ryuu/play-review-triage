package app.playreviewtriage.domain.triage

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag

interface TriageEngine {
    fun evaluate(text: String, rating: Int): TriageResult
}
