package leonfvt.skyfuel_app.domain.model

import leonfvt.skyfuel_app.util.ErrorHandler

/**
 * Représente le résultat d'une opération qui peut réussir ou échouer.
 *
 * Inspiré du pattern Result/Either de la programmation fonctionnelle.
 * Permet une gestion explicite des erreurs sans utiliser les exceptions.
 *
 * @param T Le type de données en cas de succès
 */
sealed class Result<out T> {

    /**
     * Représente une opération réussie avec une valeur.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Représente une opération échouée avec une erreur.
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Une erreur est survenue"
    ) : Result<Nothing>() {

        /** Message lisible pour l'utilisateur */
        val userMessage: String
            get() = ErrorHandler.getUserMessage(exception)
    }

    /**
     * Représente une opération en cours de chargement.
     */
    data object Loading : Result<Nothing>()

    // =========================================================================
    // Propriétés utilitaires
    // =========================================================================

    /** Retourne true si le résultat est un succès */
    val isSuccess: Boolean get() = this is Success

    /** Retourne true si le résultat est une erreur */
    val isError: Boolean get() = this is Error

    /** Retourne true si le résultat est en chargement */
    val isLoading: Boolean get() = this is Loading

    // =========================================================================
    // Fonctions de transformation
    // =========================================================================

    /**
     * Retourne la valeur si succès, null sinon.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Retourne la valeur si succès, la valeur par défaut sinon.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Retourne l'exception si erreur, null sinon.
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }

    /**
     * Transforme la valeur en cas de succès.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Transforme la valeur en cas de succès, avec possibilité d'erreur.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Exécute une action en cas de succès.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Exécute une action en cas d'erreur.
     */
    inline fun onError(action: (Throwable, String) -> Unit): Result<T> {
        if (this is Error) action(exception, message)
        return this
    }

    /**
     * Exécute une action en cas de chargement.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * Gère les trois cas possibles.
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (Throwable, String) -> R,
        onLoading: () -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception, message)
        is Loading -> onLoading()
    }

    companion object {
        /**
         * Crée un Result.Success à partir d'une valeur.
         */
        fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Crée un Result.Error à partir d'une exception.
         */
        fun error(exception: Throwable): Result<Nothing> = Error(exception)

        /**
         * Crée un Result.Error à partir d'un message.
         */
        fun error(message: String): Result<Nothing> =
            Error(ErrorHandler.AppError.GeneralError(message))

        /**
         * Crée un Result.Loading.
         */
        fun loading(): Result<Nothing> = Loading

        /**
         * Exécute un bloc et encapsule le résultat dans un Result.
         * Capture les exceptions et les transforme en Result.Error.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                ErrorHandler.logError(e)
                Error(e)
            }
        }

        /**
         * Version suspend de runCatching pour les coroutines.
         */
        suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                ErrorHandler.logError(e)
                Error(e)
            }
        }
    }
}

/**
 * Extension pour convertir un Result en Flow qui émet une seule valeur.
 */
fun <T> Result<T>.asFlow(): kotlinx.coroutines.flow.Flow<Result<T>> =
    kotlinx.coroutines.flow.flowOf(this)
