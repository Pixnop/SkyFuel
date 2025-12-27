package leonfvt.skyfuel_app.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Classe utilitaire pour gérer les erreurs de manière cohérente dans l'application.
 *
 * Utilise Timber pour le logging structuré.
 */
object ErrorHandler {

    /**
     * Types d'erreurs spécifiques à l'application.
     * Permet une gestion fine des différents types d'erreurs.
     */
    sealed class AppError(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause) {

        /** Erreur liée à la base de données (Room, SQLite) */
        class DatabaseError(
            message: String,
            cause: Throwable? = null
        ) : AppError("Erreur de base de données: $message", cause)

        /** Erreur réseau (API, connexion) */
        class NetworkError(
            message: String,
            cause: Throwable? = null
        ) : AppError("Erreur réseau: $message", cause)

        /** Erreur de validation des données d'entrée */
        class ValidationError(
            message: String
        ) : AppError("Erreur de validation: $message")

        /** Ressource non trouvée (batterie, historique, etc.) */
        class ResourceNotFoundError(
            resourceType: String,
            resourceId: String
        ) : AppError("$resourceType avec ID $resourceId non trouvé")

        /** Erreur générique */
        class GeneralError(
            message: String,
            cause: Throwable? = null
        ) : AppError(message, cause)
    }

    /**
     * Retourne un message utilisateur lisible à partir d'une exception.
     * Utilisé pour afficher des messages dans l'UI.
     */
    fun getUserMessage(exception: Throwable): String {
        return when (exception) {
            is AppError.DatabaseError -> "Problème d'accès aux données. Veuillez réessayer."
            is AppError.NetworkError -> "Problème de connexion. Vérifiez votre connexion internet."
            is AppError.ValidationError -> exception.message
            is AppError.ResourceNotFoundError -> exception.message
            is AppError.GeneralError -> exception.message
            else -> "Erreur inattendue: ${exception.message ?: "Inconnue"}"
        }
    }

    /**
     * Journalise une erreur avec Timber.
     *
     * @param exception L'exception à logger
     * @param additionalInfo Information contextuelle supplémentaire
     */
    fun logError(exception: Throwable, additionalInfo: String = "") {
        if (additionalInfo.isNotEmpty()) {
            Timber.e(exception, additionalInfo)
        } else {
            Timber.e(exception)
        }
    }

    /**
     * Journalise un warning avec Timber.
     */
    fun logWarning(message: String, exception: Throwable? = null) {
        if (exception != null) {
            Timber.w(exception, message)
        } else {
            Timber.w(message)
        }
    }

    /**
     * Journalise une info avec Timber.
     */
    fun logInfo(message: String) {
        Timber.i(message)
    }

    /**
     * Journalise un message de debug avec Timber.
     */
    fun logDebug(message: String) {
        Timber.d(message)
    }

    /**
     * Crée un gestionnaire d'exception pour les coroutines.
     */
    fun createCoroutineExceptionHandler(onError: (Throwable) -> Unit): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            logError(throwable, "Erreur non gérée dans une coroutine")
            onError(throwable)
        }
    }

    /**
     * Extension function pour CoroutineScope qui facilite le lancement
     * de coroutines avec gestion d'erreurs automatique.
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