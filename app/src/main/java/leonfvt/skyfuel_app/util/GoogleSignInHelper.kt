package leonfvt.skyfuel_app.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import timber.log.Timber

/**
 * Helper class for Google Sign-In using Credential Manager
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Initiates Google Sign-In flow
     * @param webClientId The Web Client ID from Firebase Console
     * @return The ID token if successful, null otherwise
     */
    suspend fun signIn(webClientId: String): GoogleSignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context as android.app.Activity
            )

            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            Timber.e(e, "Google Sign-In failed")
            GoogleSignInResult.Error("Connexion échouée: ${e.localizedMessage}")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during Google Sign-In")
            GoogleSignInResult.Error("Erreur inattendue: ${e.localizedMessage}")
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): GoogleSignInResult {
        val credential = result.credential

        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        GoogleSignInResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        Timber.e(e, "Failed to parse Google ID token")
                        GoogleSignInResult.Error("Erreur de parsing du token")
                    }
                } else {
                    GoogleSignInResult.Error("Type de credential non supporté")
                }
            }
            else -> GoogleSignInResult.Error("Credential non reconnu")
        }
    }
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String, val email: String?) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
