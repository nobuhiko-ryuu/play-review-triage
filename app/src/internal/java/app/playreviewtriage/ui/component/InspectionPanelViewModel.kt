package app.playreviewtriage.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.data.fake.FakeScenario
import app.playreviewtriage.data.fake.InternalTestStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionPanelViewModel @Inject constructor(
    private val store: InternalTestStore,
) : ViewModel() {

    val scenario = store.scenario.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FakeScenario.SUCCESS,
    )

    fun setScenario(scenario: FakeScenario) {
        viewModelScope.launch { store.setScenario(scenario) }
    }
}
