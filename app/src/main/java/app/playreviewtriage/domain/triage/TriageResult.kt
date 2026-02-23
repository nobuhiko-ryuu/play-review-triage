package app.playreviewtriage.domain.triage

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag

data class TriageResult(
    val importance: Importance,
    val tags: Set<ReasonTag>,
)
