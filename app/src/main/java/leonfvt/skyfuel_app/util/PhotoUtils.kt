package leonfvt.skyfuel_app.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitaire pour la gestion des photos de batteries
 */
object PhotoUtils {
    
    private const val PHOTOS_DIR = "battery_photos"
    private const val FILE_PROVIDER_AUTHORITY = "leonfvt.skyfuel_app.fileprovider"
    
    /**
     * Crée un fichier temporaire pour stocker une photo
     */
    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "BATTERY_${timeStamp}_"
        
        val storageDir = File(context.filesDir, PHOTOS_DIR)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }
    
    /**
     * Obtient l'URI content:// pour un fichier photo (pour la caméra)
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        )
    }
    
    /**
     * Copie un fichier depuis une URI vers le stockage interne
     */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val destFile = createImageFile(context)
            
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Supprime une photo
     */
    fun deletePhoto(photoPath: String): Boolean {
        return try {
            File(photoPath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Vérifie si le fichier photo existe
     */
    fun photoExists(photoPath: String?): Boolean {
        if (photoPath.isNullOrBlank()) return false
        return File(photoPath).exists()
    }
    
    /**
     * Obtient le dossier des photos
     */
    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Nettoie les photos orphelines (photos sans batterie associée)
     */
    fun cleanOrphanPhotos(context: Context, validPhotoPaths: Set<String>) {
        val photosDir = getPhotosDirectory(context)
        photosDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in validPhotoPaths) {
                file.delete()
            }
        }
    }
}
