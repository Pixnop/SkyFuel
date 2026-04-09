package leonfvt.skyfuel_app.util

import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
import leonfvt.skyfuel_app.data.local.dao.CategoryDao
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import leonfvt.skyfuel_app.data.local.entity.BatteryCategoryCrossRef
import leonfvt.skyfuel_app.data.local.entity.BatteryEntity
import leonfvt.skyfuel_app.data.local.entity.BatteryHistoryEntity
import leonfvt.skyfuel_app.data.local.entity.CategoryEntity
import leonfvt.skyfuel_app.data.local.entity.ChargeReminderEntity
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.ReminderType
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeder de données de test pour peupler l'app avec des batteries variées,
 * de l'historique riche et des catégories. Activé uniquement en mode DEBUG.
 */
@Singleton
class TestDataSeeder @Inject constructor(
    private val batteryDao: BatteryDao,
    private val historyDao: BatteryHistoryDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ChargeReminderDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun seedIfEmpty() {
        val existing = batteryDao.getAllBatteries().first()
        if (existing.isNotEmpty()) {
            Timber.d("TestDataSeeder: Base déjà peuplée (${existing.size} batteries), skip")
            return
        }

        Timber.i("TestDataSeeder: Peuplement de la base avec des données de test...")

        // Skip l'onboarding
        userPreferencesRepository.setOnboardingCompleted(true)

        // === CATÉGORIES ===
        val catDroneRace = categoryDao.insertCategory(CategoryEntity(name = "Drone Racing", color = 0xFFE91E63L, icon = "star", description = "Batteries pour drones de course FPV"))
        val catInspection = categoryDao.insertCategory(CategoryEntity(name = "Inspection", color = 0xFF2196F3L, icon = "folder", description = "Batteries pour drones d'inspection"))
        val catVideo = categoryDao.insertCategory(CategoryEntity(name = "Vidéo", color = 0xFF4CAF50L, icon = "label", description = "Batteries pour prises de vue aériennes"))
        val catReserve = categoryDao.insertCategory(CategoryEntity(name = "Réserve", color = 0xFFFF9800L, icon = "folder", description = "Batteries de secours"))

        // === BATTERIES ===
        val today = LocalDate.now()

        val batteries = listOf(
            // LiPo racing - beaucoup de cycles, récente
            BatteryEntity(brand = "Tattu", model = "R-Line 1550mAh 6S", serialNumber = "TAT-R001", type = BatteryType.LIPO, cells = 6, capacity = 1550, purchaseDate = today.minusMonths(4), status = BatteryStatus.CHARGED, cycleCount = 85, notes = "Batterie principale racing", lastUseDate = today.minusDays(1), lastChargeDate = today, qrCodeId = "SF-001"),
            // LiPo racing - très usée
            BatteryEntity(brand = "Tattu", model = "R-Line 1300mAh 6S", serialNumber = "TAT-R002", type = BatteryType.LIPO, cells = 6, capacity = 1300, purchaseDate = today.minusMonths(14), status = BatteryStatus.DISCHARGED, cycleCount = 280, notes = "Puffing léger, à surveiller", lastUseDate = today.minusDays(3), lastChargeDate = today.minusDays(5), qrCodeId = "SF-002"),
            // LiPo vidéo - usage modéré
            BatteryEntity(brand = "DJI", model = "Mavic 3 Intelligent", serialNumber = "DJI-M3-001", type = BatteryType.LIPO, cells = 4, capacity = 5000, purchaseDate = today.minusMonths(8), status = BatteryStatus.STORAGE, cycleCount = 45, notes = "Mavic 3 Pro principal", lastUseDate = today.minusDays(14), lastChargeDate = today.minusDays(12), qrCodeId = "SF-003"),
            // LiPo vidéo - neuve
            BatteryEntity(brand = "DJI", model = "Mavic 3 Intelligent", serialNumber = "DJI-M3-002", type = BatteryType.LIPO, cells = 4, capacity = 5000, purchaseDate = today.minusWeeks(3), status = BatteryStatus.CHARGED, cycleCount = 5, notes = "Backup Mavic 3", lastUseDate = today.minusDays(2), lastChargeDate = today.minusDays(1), qrCodeId = "SF-004"),
            // Li-Ion inspection - ancienne mais peu utilisée
            BatteryEntity(brand = "Autel", model = "EVO II 7100mAh", serialNumber = "AUT-EVO-001", type = BatteryType.LI_ION, cells = 4, capacity = 7100, purchaseDate = today.minusYears(2), status = BatteryStatus.CHARGED, cycleCount = 120, notes = "Drone d'inspection thermique", lastUseDate = today.minusDays(7), lastChargeDate = today.minusDays(5), qrCodeId = "SF-005"),
            // LiPo racing - hors service
            BatteryEntity(brand = "CNHL", model = "MiniStar 1500mAh 4S", serialNumber = "CNHL-MS-001", type = BatteryType.LIPO, cells = 4, capacity = 1500, purchaseDate = today.minusYears(1).minusMonths(6), status = BatteryStatus.OUT_OF_SERVICE, cycleCount = 350, notes = "Gonflée, mise au rebut", lastUseDate = today.minusMonths(2), lastChargeDate = today.minusMonths(2), qrCodeId = "SF-006"),
            // NiMH - radio
            BatteryEntity(brand = "Eneloop", model = "Pro AA 2500mAh", serialNumber = "ENL-PRO-001", type = BatteryType.NIMH, cells = 8, capacity = 2500, purchaseDate = today.minusMonths(10), status = BatteryStatus.CHARGED, cycleCount = 30, notes = "Pour radiocommande FrSky", lastUseDate = today.minusDays(1), lastChargeDate = today, qrCodeId = "SF-007"),
            // LiFe - émetteur
            BatteryEntity(brand = "Spektrum", model = "LiFe 2S 6.6V", serialNumber = "SPK-LF-001", type = BatteryType.LIFE, cells = 2, capacity = 3200, purchaseDate = today.minusYears(1), status = BatteryStatus.CHARGED, cycleCount = 150, notes = "Émetteur DX9", lastUseDate = today.minusDays(3), lastChargeDate = today.minusDays(2), qrCodeId = "SF-008"),
            // LiPo - gros porteur
            BatteryEntity(brand = "Multistar", model = "High Cap 10000mAh 6S", serialNumber = "MS-HC-001", type = BatteryType.LIPO, cells = 6, capacity = 10000, purchaseDate = today.minusMonths(6), status = BatteryStatus.DISCHARGED, cycleCount = 60, notes = "Gros porteur mapping", lastUseDate = today.minusDays(10), lastChargeDate = today.minusDays(12), qrCodeId = "SF-009"),
            // LiPo - FPV freestyle
            BatteryEntity(brand = "GNB", model = "HV 1100mAh 6S", serialNumber = "GNB-HV-001", type = BatteryType.LIPO, cells = 6, capacity = 1100, purchaseDate = today.minusMonths(2), status = BatteryStatus.CHARGED, cycleCount = 25, notes = "Freestyle 5 pouces", lastUseDate = today, lastChargeDate = today, qrCodeId = "SF-010"),
            // Encore une DJI
            BatteryEntity(brand = "DJI", model = "Mini 4 Pro 3850mAh", serialNumber = "DJI-M4P-001", type = BatteryType.LIPO, cells = 3, capacity = 3850, purchaseDate = today.minusMonths(3), status = BatteryStatus.STORAGE, cycleCount = 15, notes = "Drone loisir", lastUseDate = today.minusWeeks(2), lastChargeDate = today.minusWeeks(2), qrCodeId = "SF-011"),
            // Très vieille batterie
            BatteryEntity(brand = "Turnigy", model = "Graphene 1300mAh 4S", serialNumber = "TGY-GR-001", type = BatteryType.LIPO, cells = 4, capacity = 1300, purchaseDate = today.minusYears(3), status = BatteryStatus.OUT_OF_SERVICE, cycleCount = 400, notes = "Recyclée - fin de vie", lastUseDate = today.minusMonths(6), lastChargeDate = today.minusMonths(6), qrCodeId = "SF-012")
        )

        val batteryIds = batteries.map { batteryDao.insertBattery(it) }

        // === CATÉGORIE-BATTERIE RELATIONS ===
        // Racing: Tattu R-Line, CNHL, GNB
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[0], catDroneRace))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[1], catDroneRace))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[5], catDroneRace))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[9], catDroneRace))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[11], catDroneRace))
        // Inspection: Autel
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[4], catInspection))
        // Vidéo: DJI Mavic 3, Mini 4
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[2], catVideo))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[3], catVideo))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[10], catVideo))
        // Réserve: DJI Mavic backup, Multistar
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[3], catReserve))
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryIds[8], catReserve))

        // === HISTORIQUE RICHE ===
        val now = LocalDateTime.now()

        // Générer de l'historique pour chaque batterie
        batteryIds.forEachIndexed { index, batteryId ->
            val battery = batteries[index]
            val historyEntries = mutableListOf<BatteryHistoryEntity>()

            // Événements de cycles répartis dans le temps
            val cycleCount = battery.cycleCount
            val ageInDays = java.time.temporal.ChronoUnit.DAYS.between(battery.purchaseDate, today).toInt().coerceAtLeast(1)

            for (cycle in 1..cycleCount.coerceAtMost(50)) { // Max 50 events par batterie
                val daysAgo = (ageInDays * (cycleCount - cycle).toFloat() / cycleCount).toLong()
                val timestamp = now.minusDays(daysAgo).minusHours((cycle % 12).toLong())

                // Cycle complet
                historyEntries.add(BatteryHistoryEntity(
                    batteryId = batteryId,
                    timestamp = timestamp,
                    eventType = BatteryEventType.CYCLE_COMPLETED,
                    cycleNumber = cycle,
                    notes = if (cycle % 10 == 0) "Cycle #$cycle" else ""
                ))

                // Changement de statut (charge/décharge) à chaque cycle
                historyEntries.add(BatteryHistoryEntity(
                    batteryId = batteryId,
                    timestamp = timestamp.minusMinutes(30),
                    eventType = BatteryEventType.STATUS_CHANGE,
                    previousStatus = BatteryStatus.DISCHARGED,
                    newStatus = BatteryStatus.CHARGED
                ))

                // Mesures de tension régulières
                if (cycle % 5 == 0) {
                    val baseVoltage = when (battery.type) {
                        BatteryType.LIPO -> 4.2f * battery.cells
                        BatteryType.LI_ION -> 4.2f * battery.cells
                        BatteryType.NIMH -> 1.4f * battery.cells
                        BatteryType.LIFE -> 3.6f * battery.cells
                        BatteryType.OTHER -> 3.7f * battery.cells
                    }
                    // Légère dégradation progressive
                    val degradation = (cycle.toFloat() / cycleCount) * 0.3f
                    historyEntries.add(BatteryHistoryEntity(
                        batteryId = batteryId,
                        timestamp = timestamp.plusMinutes(5),
                        eventType = BatteryEventType.VOLTAGE_READING,
                        voltage = baseVoltage - degradation,
                        notes = "Mesure post-charge"
                    ))
                }

                // Maintenance tous les 20 cycles
                if (cycle % 20 == 0) {
                    historyEntries.add(BatteryHistoryEntity(
                        batteryId = batteryId,
                        timestamp = timestamp.plusHours(1),
                        eventType = BatteryEventType.MAINTENANCE,
                        notes = "Vérification visuelle et équilibrage"
                    ))
                }
            }

            // Notes ajoutées
            if (battery.notes.isNotEmpty()) {
                historyEntries.add(BatteryHistoryEntity(
                    batteryId = batteryId,
                    timestamp = now.minusDays(1),
                    eventType = BatteryEventType.NOTE_ADDED,
                    notes = battery.notes
                ))
            }

            historyEntries.forEach { historyDao.insertHistoryEntry(it) }
        }

        // === RAPPELS ===
        // Rappel de charge pour la Tattu R-Line
        reminderDao.insertReminder(ChargeReminderEntity(
            batteryId = batteryIds[0],
            title = "Charger Tattu R-Line",
            hour = 8, minute = 0,
            daysOfWeekBits = 0b0011111, // Lun-Ven
            isEnabled = true,
            reminderType = ReminderType.CHARGE,
            notes = "Avant la session de vol"
        ))
        // Rappel stockage pour DJI Mavic 3
        reminderDao.insertReminder(ChargeReminderEntity(
            batteryId = batteryIds[2],
            title = "Vérifier stockage Mavic",
            hour = 18, minute = 30,
            daysOfWeekBits = 0b1000000, // Dim
            isEnabled = true,
            reminderType = ReminderType.STORAGE,
            notes = "Vérifier le niveau de stockage"
        ))
        // Rappel maintenance Autel
        reminderDao.insertReminder(ChargeReminderEntity(
            batteryId = batteryIds[4],
            title = "Maintenance Autel EVO",
            hour = 10, minute = 0,
            daysOfWeekBits = 0, // Tous les jours
            isEnabled = false,
            reminderType = ReminderType.MAINTENANCE,
            notes = "Inspection mensuelle"
        ))

        Timber.i("TestDataSeeder: ${batteryIds.size} batteries, 4 catégories, historique riche et rappels créés")
    }
}
