package leonfvt.skyfuel_app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import leonfvt.skyfuel_app.domain.model.Battery
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitaires pour le partage et l'enregistrement des QR codes
 */
object QrCodeUtils {
    
    /**
     * Partage le QR code d'une batterie via les apps de partage d'Android
     * @param context Contexte Android
     * @param bitmap Image du QR code à partager
     * @param battery Batterie associée au QR code
     * @param isShareMode Si true, indique que c'est un QR de partage complet (toutes les données)
     */
    fun shareQrCode(context: Context, bitmap: Bitmap, battery: Battery, isShareMode: Boolean = false) {
        try {
            // Créer un fichier temporaire pour le QR code
            val cachePath = File(context.cacheDir, "qr_codes")
            cachePath.mkdirs()
            
            // Nom de fichier avec marque et numéro de série
            val prefix = if (isShareMode) "share_" else "qrcode_"
            val fileName = "${prefix}${battery.brand}_${battery.serialNumber}.png"
                .replace(" ", "_")
                .replace("/", "_")
            val file = File(cachePath, fileName)
            
            // Écrire le bitmap dans le fichier
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            // Créer l'URI pour le fichier via FileProvider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Message de partage adapté au mode
            val shareText = if (isShareMode) {
                "Partage de batterie SkyFuel: ${battery.brand} ${battery.model}\n" +
                "S/N: ${battery.serialNumber}\n" +
                "Scannez ce QR code dans SkyFuel pour importer cette batterie."
            } else {
                "QR Code pour la batterie ${battery.brand} ${battery.model} (S/N: ${battery.serialNumber})"
            }
            
            val subject = if (isShareMode) {
                "Partage SkyFuel - ${battery.brand} ${battery.model}"
            } else {
                "QR Code - ${battery.brand} ${battery.model}"
            }
            
            // Créer l'intent de partage
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Lancer le dialogue de partage
            context.startActivity(Intent.createChooser(intent, "Partager le QR code"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Erreur lors du partage du QR code: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Enregistre le QR code d'une batterie dans la galerie ou les téléchargements
     * @param context Contexte Android
     * @param bitmap Image du QR code à enregistrer
     * @param battery Batterie associée au QR code
     */
    fun saveQrCodeToGallery(context: Context, bitmap: Bitmap, battery: Battery) {
        try {
            // Création d'un nom de fichier unique
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SkyFuel_QRCode_${battery.brand}_${battery.serialNumber}_$timestamp.png"
                .replace(" ", "_")
                .replace("/", "_")
            
            var fos: OutputStream? = null
            
            // Pour Android 10 (API 29) et plus récent, on utilise MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SkyFuel")
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                if (uri != null) {
                    fos = context.contentResolver.openOutputStream(uri)
                }
            } else {
                // Pour les versions antérieures, on utilise le stockage externe
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val skyFuelDir = File(imagesDir, "SkyFuel")
                
                if (!skyFuelDir.exists()) {
                    skyFuelDir.mkdirs()
                }
                
                val imageFile = File(skyFuelDir, fileName)
                fos = FileOutputStream(imageFile)
                
                // Rendre le fichier visible dans la galerie
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)))
            }
            
            // Écriture du bitmap dans le fichier
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            
            Toast.makeText(
                context,
                "QR code enregistré dans la galerie",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Erreur lors de l'enregistrement du QR code: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}