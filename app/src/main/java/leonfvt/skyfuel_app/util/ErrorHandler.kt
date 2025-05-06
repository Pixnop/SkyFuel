package leonfvt.skyfuel_app.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Classe utilitaire pour gérer les erreurs de manière cohérente dans l'application
 */
object ErrorHandler {
    private const val TAG = "SkyFuelError"
    
    /**
     * Types d'erreurs spécifiques à l'application
     */
    sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class DatabaseError(message: String, cause: Throwable? = null) : 
            AppError("Erreur de base de données: $message", cause)
            
        class NetworkError(message: String, cause: Throwable? = null) : 
            AppError("Erreur réseau: $message", cause)
            
        class ValidationError(message: String) : 
            AppError("Erreur de validation: $message")
            
        class ResourceNotFoundError(resourceType: String, resourceId: String) : 
            AppError("$resourceType avec ID $resourceId non trouvé")
            
        class GeneralError(message: String, cause: Throwable? = null) : 
            AppError(message, cause)
    }

    /**
     * Fonction pour retourner un message utilisateur à partir d'une exception
     */
    fun getUserMessage(exception: Throwable): String {
        return when (exception) {
            is AppError.DatabaseError -> "Problème d'accès aux données. Veuillez réessayer."
            is AppError.NetworkError -> "Problème de connexion. Vérifiez votre connexion internet."
            is AppError.ValidationError -> exception.message ?: "Données invalides."
            is AppError.ResourceNotFoundError -> exception.message ?: "Ressource non trouvée."
            is AppError.GeneralError -> exception.message ?: "Une erreur est survenue."
            else -> "Erreur inattendue: ${exception.message ?: "Inconnue"}"
        }
    }

    /**
     * Fonction pour journaliser une erreur
     */
    fun logError(exception: Throwable, additionalInfo: String = "") {
        val logMessage = if (additionalInfo.isNotEmpty()) {
            "$additionalInfo: ${exception.message}"
        } else {
            exception.message ?: "Erreur sans message"
        }
        
        Log.e(TAG, logMessage, exception)
    }

    /**
     * Crée un gestionnaire d'exception pour les coroutines
     */
    fun createCoroutineExceptionHandler(onError: (Throwable) -> Unit): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            logError(throwable, "Erreur non gérée dans une coroutine")
            onError(throwable)
        }
    }

    /**
     * Extension function pour CoroutineScope qui facilite le lancement 
     * de coroutines avec gestion d'erreurs
     */
    fun CoroutineScope.launchWithErrorHandling(
        context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) = this.launch(context + createCoroutineExceptionHandler(onError)) {
        try {
            block()
        } catch (e: Exception) {
            logError(e, "Erreur gérée dans launchWithErrorHandling")
            onError(e)
        }
    }
}