# Guide de Configuration Firebase pour SkyFuel

Ce guide vous accompagne étape par étape pour configurer Firebase dans l'application SkyFuel.

## Prérequis

- Un compte Google
- Android Studio installé
- Le projet SkyFuel cloné et fonctionnel

---

## Étape 1 : Créer un projet Firebase

1. Rendez-vous sur [Firebase Console](https://console.firebase.google.com/)

2. Cliquez sur **"Ajouter un projet"** (ou "Create a project")

3. **Nom du projet** : Entrez `SkyFuel` (ou un nom de votre choix)

4. **Google Analytics** : 
   - Vous pouvez l'activer ou le désactiver selon vos besoins
   - Si activé, sélectionnez ou créez un compte Analytics

5. Cliquez sur **"Créer le projet"** et attendez la création

---

## Étape 2 : Ajouter une application Android

1. Dans la console Firebase, cliquez sur l'icône **Android** pour ajouter une app

2. **Nom du package Android** : 
   ```
   leonfvt.skyfuel_app
   ```
   ⚠️ Ce nom doit correspondre EXACTEMENT à celui dans `app/build.gradle.kts`

3. **Surnom de l'application** (optionnel) : `SkyFuel`

4. **Certificat de signature SHA-1** (optionnel pour commencer) :
   - Pour l'obtenir, exécutez dans le terminal :
   ```bash
   cd /home/fievetl/StudioProjects/SkyFuel
   ./gradlew signingReport
   ```
   - Copiez la valeur SHA-1 du variant `debug`

5. Cliquez sur **"Enregistrer l'application"**

---

## Étape 3 : Télécharger google-services.json

1. Firebase vous propose de télécharger `google-services.json`

2. **Téléchargez** ce fichier

3. **Placez-le** dans le dossier `app/` de votre projet :
   ```
   /home/fievetl/StudioProjects/SkyFuel/app/google-services.json
   ```

4. Cliquez sur **"Suivant"** dans la console Firebase

---

## Étape 4 : Configurer les fichiers Gradle

### 4.1 Fichier `build.gradle.kts` (racine du projet)

Ouvrez `/home/fievetl/StudioProjects/SkyFuel/build.gradle.kts` et ajoutez le plugin Google Services :

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.google.services) apply false  // ← Ajouter cette ligne
}
```

### 4.2 Fichier `app/build.gradle.kts`

Ouvrez `/home/fievetl/StudioProjects/SkyFuel/app/build.gradle.kts` :

**a) Ajoutez le plugin en haut du fichier :**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kapt)
    alias(libs.plugins.google.services)  // ← Ajouter cette ligne
}
```

**b) Ajoutez les dépendances Firebase dans le bloc `dependencies` :**

```kotlin
dependencies {
    // ... autres dépendances existantes ...
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
}
```

---

## Étape 5 : Activer Firestore dans Firebase

1. Dans la console Firebase, allez dans **"Build" → "Firestore Database"**

2. Cliquez sur **"Créer une base de données"**

3. **Mode de démarrage** :
   - Sélectionnez **"Mode test"** pour commencer (permet lecture/écriture sans authentification pendant 30 jours)
   - Vous pourrez sécuriser plus tard

4. **Emplacement** : Choisissez `eur3 (europe-west)` ou le plus proche de vos utilisateurs

5. Cliquez sur **"Activer"**

---

## Étape 6 : Activer l'authentification (optionnel mais recommandé)

1. Dans la console Firebase, allez dans **"Build" → "Authentication"**

2. Cliquez sur **"Commencer"**

3. Dans l'onglet **"Sign-in method"**, activez :
   - **Anonyme** : Pour une utilisation simple sans compte
   - **Google** : Pour la connexion avec compte Google (recommandé)

### Pour activer Google Sign-In :

1. Cliquez sur **"Google"**
2. Activez le bouton
3. Ajoutez un **email d'assistance** (votre email)
4. Cliquez sur **"Enregistrer"**

---

## Étape 7 : Mettre à jour le code de synchronisation

Maintenant que Firebase est configuré, mettez à jour `FirebaseSyncService.kt` :

```kotlin
// Fichier: app/src/main/java/leonfvt/skyfuel_app/data/sync/FirebaseSyncService.kt

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
                isAuthenticated = firebaseAuth.currentUser != null
            )
        }
    }
    
    fun isFirebaseConfigured(): Boolean = true
    
    suspend fun signInAnonymously(): Boolean {
        return try {
            val result = auth.signInAnonymously().await()
            result.user != null
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(error = e.message)
            false
        }
    }
    
    suspend fun signOut() {
        auth.signOut()
        _syncState.value = _syncState.value.copy(isAuthenticated = false)
    }
    
    suspend fun syncBatteries(localBatteries: List<Battery>): SyncResult {
        val userId = auth.currentUser?.uid ?: return SyncResult.NotAuthenticated
        
        if (!_syncState.value.isEnabled) {
            return SyncResult.Disabled
        }
        
        _syncState.value = _syncState.value.copy(isSyncing = true)
        
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
            
            // Download des batteries distantes
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
                error = e.message
            )
            SyncResult.Error(e.message ?: "Erreur de synchronisation")
        }
    }
    
    // Extension pour convertir Battery en Map pour Firestore
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
```

---

## Étape 8 : Synchroniser le projet

1. Dans Android Studio, cliquez sur **"Sync Now"** dans la barre qui apparaît

2. Ou utilisez **File → Sync Project with Gradle Files**

3. Attendez que la synchronisation se termine

---

## Étape 9 : Tester la configuration

1. Lancez l'application sur un émulateur ou appareil

2. Allez dans **Paramètres**

3. La section "Synchronisation Cloud" devrait maintenant être active

4. Testez la connexion anonyme

---

## Règles de sécurité Firestore (Production)

Une fois en production, mettez à jour les règles Firestore dans la console :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Les utilisateurs ne peuvent accéder qu'à leurs propres données
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## Structure des données Firestore

```
users/
  └── {userId}/
      └── batteries/
          └── {batteryId}/
              ├── id: number
              ├── brand: string
              ├── model: string
              ├── serialNumber: string
              ├── type: string (LIPO, LI_ION, etc.)
              ├── cells: number
              ├── capacity: number
              ├── purchaseDate: string (ISO date)
              ├── status: string (CHARGED, DISCHARGED, etc.)
              ├── cycleCount: number
              ├── notes: string
              ├── lastUseDate: string | null
              ├── lastChargeDate: string | null
              ├── qrCodeId: string
              └── photoPath: string | null
```

---

## Dépannage

### Erreur "google-services.json not found"
- Vérifiez que le fichier est bien dans `app/google-services.json`
- Le nom du package doit correspondre exactement

### Erreur "API key not valid"
- Vérifiez dans la console Firebase que l'app Android est bien enregistrée
- Régénérez `google-services.json` si nécessaire

### Erreur "Permission denied" sur Firestore
- Vérifiez que les règles Firestore autorisent l'accès
- En mode test, tout le monde peut lire/écrire pendant 30 jours

### L'authentification Google ne fonctionne pas
- Vérifiez que le SHA-1 est correctement configuré dans Firebase
- Exécutez `./gradlew signingReport` pour obtenir le SHA-1

---

## Ressources utiles

- [Documentation Firebase Android](https://firebase.google.com/docs/android/setup)
- [Guide Firestore](https://firebase.google.com/docs/firestore/quickstart)
- [Authentification Firebase](https://firebase.google.com/docs/auth/android/start)

---

## Prochaines étapes

1. ✅ Configuration de base Firebase
2. ⬜ Implémenter la synchronisation automatique en arrière-plan
3. ⬜ Ajouter la synchronisation des photos (Firebase Storage)
4. ⬜ Gérer les conflits de synchronisation
5. ⬜ Ajouter la synchronisation hors-ligne
