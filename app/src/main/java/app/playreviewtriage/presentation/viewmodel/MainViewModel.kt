package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.domain.usecase.LoadStartupRouteUseCase
import app.playreviewtriage.domain.usecase.StartupRoute
import app.playreviewtriage.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadStartupRouteUseCase: LoadStartupRouteUseCase,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val route = loadStartupRouteUseCase.invoke()
            _startDestination.value = when (route) {
                StartupRoute.SignIn -> NavRoutes.SignIn.route
                StartupRoute.Setup -> NavRoutes.Setup.route
                StartupRoute.Today -> NavRoutes.Today.route
            }
        }
    }
}
