package leonfvt.skyfuel_app.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * État de la synchronisation Firebase
 */
data class SyncState(
    val isEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null,
    val pendingChanges: Int = 0,
    val userEmail: String? = null
)

/**
 * Résultat d'une opération de synchronisation
 */
sealed class SyncResult {
    data class Success(val uploadedCount: Int, val downloadedCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    data object NotAuthenticated : SyncResult()
    data object Disabled : SyncResult()
}

/**
 * Service de synchronisation Firebase
 */
@Singleton
class FirebaseSyncService @Inject constructor() {
    
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: Flow<SyncState> = _syncState.asStateFlow()
    
    init {
        // Observer l'état d'authentification
        auth.addAuthStateListener { firebaseAuth ->
            _syncState.value = _syncState.value.copy(
                isAuthenticated = firebaseAuth.currentUser != null,
                userEmail = firebaseAuth.currentUser?.email
            )
        }
    }
    
    /**
     * Vérifie si Firebase est configuré
     */
    fun isFirebaseConfigured(): Boolean = true
    
    /**
     * Active/désactive la synchronisation
     */
    suspend fun setSyncEnabled(enabled: Boolean) {
        _syncState.value = _syncState.value.copy(isEnabled = enabled)
    }
    
    /**
     * Authentifie l'utilisateur avec un compte anonyme
     */
    suspend fun signInAnonymously(): Boolean {
        return try {
            val result = auth.signInAnonymously().await()
            _syncState.value = _syncState.value.copy(
                isAuthenticated = result.user != null,
                error = null
            )
            result.user != null
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                error = "Erreur de connexion: ${e.message}"
            )
            false
        }
    }
    
    /**
     * Authentifie l'utilisateur avec Google
     */
    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            _syncState.value = _syncState.value.copy(
                isAuthenticated = result.user != null,
                userEmail = result.user?.email,
                error = null
            )
            result.user != null
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                error = "Erreur de connexion Google: ${e.message}"
            )
            false
        }
    }
    
    /**
     * Déconnecte l'utilisateur
     */
    suspend fun signOut() {
        auth.signOut()
        _syncState.value = _syncState.value.copy(
            isAuthenticated = false,
            userEmail = null,
            isEnabled = false
        )
    }
    
    /**
     * Synchronise les batteries locales avec Firebase
     */
    suspend fun syncBatteries(localBatteries: List<Battery>): SyncResult {
        val userId = auth.currentUser?.uid ?: return SyncResult.NotAuthenticated
        
        if (!_syncState.value.isEnabled) {
            return SyncResult.Disabled
        }
        
        _syncState.value = _syncState.value.copy(isSyncing = true, error = null)
        
        return try {
            val batteriesRef = firestore
                .collection("users")
                .document(userId)
                .collection("batteries")
            
            // Upload des batteries locales
            var uploadedCount = 0
            localBatteries.forEach { battery ->
                batteriesRef.document(battery.id.toString()).set(battery.toMap()).await()
                uploadedCount++
            }
            
            // Download des batteries distantes (pour info)
            val snapshot = batteriesRef.get().await()
            val downloadedCount = snapshot.documents.size
            
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                error = null
            )
            
            SyncResult.Success(uploadedCount, downloadedCount)
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = "Erreur de synchronisation: ${e.message}"
            )
            SyncResult.Error(e.message ?: "Erreur de synchronisation")
        }
    }
    
    /**
     * Télécharge les batteries depuis Firebase
     */
    suspend fun downloadBatteries(): List<Battery>? {
        val userId = auth.currentUser?.uid ?: return null
        
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("batteries")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Battery(
                        id = doc.getLong("id") ?: 0L,
                        brand = doc.getString("brand") ?: "",
                        model = doc.getString("model") ?: "",
                        serialNumber = doc.getString("serialNumber") ?: "",
                        type = BatteryType.valueOf(doc.getString("type") ?: "LIPO"),
                        cells = doc.getLong("cells")?.toInt() ?: 3,
                        capacity = doc.getLong("capacity")?.toInt() ?: 1000,
                        purchaseDate = LocalDate.parse(doc.getString("purchaseDate") ?: LocalDate.now().toString()),
                        status = BatteryStatus.valueOf(doc.getString("status") ?: "CHARGED"),
                        cycleCount = doc.getLong("cycleCount")?.toInt() ?: 0,
                        notes = doc.getString("notes") ?: "",
                        lastUseDate = doc.getString("lastUseDate")?.let { LocalDate.parse(it) },
                        lastChargeDate = doc.getString("lastChargeDate")?.let { LocalDate.parse(it) },
                        qrCodeId = doc.getString("qrCodeId") ?: "",
                        photoPath = doc.getString("photoPath")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                error = "Erreur de téléchargement: ${e.message}"
            )
            null
        }
    }
    
    /**
     * Force une synchronisation complète
     */
    suspend fun forceFullSync(localBatteries: List<Battery>): SyncResult {
        return syncBatteries(localBatteries)
    }
    
    /**
     * Efface les données distantes
     */
    suspend fun clearRemoteData(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val batteriesRef = firestore
                .collection("users")
                .document(userId)
                .collection("batteries")
            
            val snapshot = batteriesRef.get().await()
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                error = "Erreur de suppression: ${e.message}"
            )
            false
        }
    }
    
    /**
     * Efface l'erreur courante
     */
    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
    
    /**
     * Convertit une Battery en Map pour Firestore
     */
    private fun Battery.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "brand" to brand,
        "model" to model,
        "serialNumber" to serialNumber,
        "type" to type.name,
        "cells" to cells,
        "capacity" to capacity,
        "purchaseDate" to purchaseDate.toString(),
        "status" to status.name,
        "cycleCount" to cycleCount,
        "notes" to notes,
        "lastUseDate" to lastUseDate?.toString(),
        "lastChargeDate" to lastChargeDate?.toString(),
        "qrCodeId" to qrCodeId,
        "photoPath" to photoPath
    )
}
