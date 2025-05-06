package leonfvt.skyfuel_app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.util.ErrorHandler.launchWithErrorHandling

/**
 * Extension functions pour faciliter l'exécution des use cases et simplifier
 * la gestion d'erreurs et des états de chargement dans les ViewModels
 */
object UseCaseExtensions {

    /**
     * Extension sur ViewModel pour exécuter un use case suspending
     */
    fun <T> ViewModel.executeUseCase(
        useCase: suspend () -> T,
        onStart: () -> Unit = {},
        onError: (Throwable) -> Unit,
        onSuccess: (T) -> Unit
    ) {
        viewModelScope.launchWithErrorHandling(
            onError = onError
        ) {
            onStart()
            val result = useCase()
            onSuccess(result)
        }
    }

    /**
     * Extension sur ViewModel pour exécuter un use case qui retourne un Flow
     */
    fun <T> ViewModel.executeFlowUseCase(
        useCase: () -> Flow<T>,
        onStart: () -> Unit = {},
        onError: (Throwable) -> Unit,
        onEach: (T) -> Unit
    ) {
        viewModelScope.launch {
            useCase()
                .onStart { onStart() }
                .catch { error -> onError(error) }
                .collect { result -> onEach(result) }
        }
    }

    /**
     * Extension sur Flow pour faciliter l'ajout du comportement onStart/onError
     */
    fun <T> Flow<T>.handleErrors(
        onStart: () -> Unit = {},
        onError: (Throwable) -> Unit
    ): Flow<T> {
        return this
            .onStart { onStart() }
            .catch { error -> 
                ErrorHandler.logError(error, "Erreur dans le flux de données")
                onError(error)
            }
    }
}