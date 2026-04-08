package leonfvt.skyfuel_app.domain.service

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Point de données pour les courbes de prédiction
 */
data class PredictionPoint(
    val date: LocalDate,
    val healthPercent: Float,
    val isPredicted: Boolean = false
)

/**
 * Résultat complet de prédiction pour une batterie
 */
data class BatteryPrediction(
    val battery: Battery,
    val estimatedEndOfLife: LocalDate,
    val remainingDays: Long,
    val remainingCycles: Int,
    val cyclesPerMonth: Float,
    val healthCurve: List<PredictionPoint>,
    val confidencePercent: Int
)

/**
 * Données de tendance de tension
 */
data class VoltageTrend(
    val date: LocalDate,
    val voltage: Float
)

/**
 * Score de santé de la flotte
 */
data class FleetHealthScore(
    val overallScore: Int,
    val excellentCount: Int,
    val goodCount: Int,
    val fairCount: Int,
    val poorCount: Int,
    val criticalCount: Int,
    val trend: FleetTrend,
    val nextReplacementDate: LocalDate?,
    val nextReplacementBattery: Battery?
)

enum class FleetTrend { IMPROVING, STABLE, DECLINING }

/**
 * Activité par jour de la semaine pour la heatmap
 */
data class WeeklyActivity(
    val dayOfWeek: Int, // 1=Lun, 7=Dim
    val hourOfDay: Int, // 0-23
    val count: Int
)

/**
 * Service de prédiction de durée de vie des batteries.
 * Utilise les données historiques pour estimer la fin de vie et projeter la santé.
 */
@Singleton
class BatteryPredictionService @Inject constructor() {

    /**
     * Nombre de cycles maximum estimé par type de batterie
     */
    private fun getMaxCycles(type: BatteryType): Int = when (type) {
        BatteryType.LIPO -> 400
        BatteryType.LI_ION -> 650
        BatteryType.NIMH -> 1000
        BatteryType.LIFE -> 2000
        BatteryType.OTHER -> 500
    }

    /**
     * Génère la prédiction complète pour une batterie
     */
    fun predictBatteryLifespan(battery: Battery, history: List<BatteryHistory>): BatteryPrediction {
        val maxCycles = getMaxCycles(battery.type)
        val currentHealth = battery.getHealthPercentage()
        val ageInDays = battery.getAgeInDays().coerceAtLeast(1)

        // Calcul du rythme de cycles
        val cyclesPerDay = battery.cycleCount.toFloat() / ageInDays
        val cyclesPerMonth = cyclesPerDay * 30f

        // Cycles restants avant 0% santé
        val remainingCycles = (maxCycles - battery.cycleCount).coerceAtLeast(0)

        // Jours restants estimés
        val remainingDays = if (cyclesPerDay > 0) {
            (remainingCycles / cyclesPerDay).toLong().coerceAtLeast(0)
        } else {
            // Si pas de cycles, estimer par dégradation liée à l'âge
            val ageImpactPerYear = when (battery.type) {
                BatteryType.LIPO -> 10f
                BatteryType.LI_ION -> 7f
                BatteryType.NIMH -> 5f
                BatteryType.LIFE -> 4f
                BatteryType.OTHER -> 8f
            }
            val remainingFromAge = (currentHealth / ageImpactPerYear * 365f).toLong()
            remainingFromAge.coerceAtLeast(30)
        }

        val estimatedEndOfLife = LocalDate.now().plusDays(remainingDays)

        // Courbe de santé historique + projection
        val healthCurve = generateHealthCurve(battery, cyclesPerDay, maxCycles)

        // Confiance basée sur la quantité de données
        val confidencePercent = calculateConfidence(battery, history)

        return BatteryPrediction(
            battery = battery,
            estimatedEndOfLife = estimatedEndOfLife,
            remainingDays = remainingDays,
            remainingCycles = remainingCycles,
            cyclesPerMonth = cyclesPerMonth,
            healthCurve = healthCurve,
            confidencePercent = confidencePercent
        )
    }

    /**
     * Génère la courbe de santé passée + projection future
     */
    private fun generateHealthCurve(
        battery: Battery,
        cyclesPerDay: Float,
        maxCycles: Int
    ): List<PredictionPoint> {
        val points = mutableListOf<PredictionPoint>()
        val purchaseDate = battery.purchaseDate
        val today = LocalDate.now()
        val ageInDays = ChronoUnit.DAYS.between(purchaseDate, today).toInt()

        // Points historiques (1 point par mois depuis l'achat)
        val monthsOfHistory = (ageInDays / 30).coerceAtLeast(1)
        for (i in 0..monthsOfHistory) {
            val date = purchaseDate.plusDays((i * 30).toLong())
            if (date.isAfter(today)) break

            val daysElapsed = ChronoUnit.DAYS.between(purchaseDate, date).toFloat()
            val estimatedCycles = (cyclesPerDay * daysElapsed).toInt()
            val health = simulateHealth(battery.type, estimatedCycles, daysElapsed)

            points.add(PredictionPoint(date, health, isPredicted = false))
        }

        // Point actuel
        points.add(PredictionPoint(today, battery.getHealthPercentage().toFloat(), isPredicted = false))

        // Points de projection future (jusqu'à 0% ou 2 ans max)
        val futureMonths = 24
        for (i in 1..futureMonths) {
            val futureDate = today.plusMonths(i.toLong())
            val totalDays = ChronoUnit.DAYS.between(purchaseDate, futureDate).toFloat()
            val estimatedCycles = (cyclesPerDay * totalDays).toInt()
            val health = simulateHealth(battery.type, estimatedCycles, totalDays)

            if (health <= 0f) {
                points.add(PredictionPoint(futureDate, 0f, isPredicted = true))
                break
            }
            points.add(PredictionPoint(futureDate, health, isPredicted = true))
        }

        return points
    }

    /**
     * Simule la santé à un moment donné
     */
    private fun simulateHealth(type: BatteryType, cycles: Int, daysElapsed: Float): Float {
        val cycleImpact = when (type) {
            BatteryType.LIPO -> cycles * 0.25f
            BatteryType.LI_ION -> cycles * 0.15f
            BatteryType.NIMH -> cycles * 0.1f
            BatteryType.LIFE -> cycles * 0.05f
            BatteryType.OTHER -> cycles * 0.2f
        }
        val ageImpact = when (type) {
            BatteryType.LIPO -> (daysElapsed / 365f) * 10f
            BatteryType.LI_ION -> (daysElapsed / 365f) * 7f
            BatteryType.NIMH -> (daysElapsed / 365f) * 5f
            BatteryType.LIFE -> (daysElapsed / 365f) * 4f
            BatteryType.OTHER -> (daysElapsed / 365f) * 8f
        }
        return (100f - cycleImpact - ageImpact).coerceIn(0f, 100f)
    }

    /**
     * Confiance de la prédiction basée sur la quantité de données
     */
    private fun calculateConfidence(battery: Battery, history: List<BatteryHistory>): Int {
        var confidence = 30 // Base

        // Plus on a de cycles, plus c'est fiable
        if (battery.cycleCount >= 10) confidence += 15
        if (battery.cycleCount >= 50) confidence += 15

        // Plus on a d'historique, mieux c'est
        if (history.size >= 10) confidence += 10
        if (history.size >= 50) confidence += 10

        // L'âge aide aussi
        if (battery.getAgeInDays() >= 30) confidence += 10
        if (battery.getAgeInDays() >= 180) confidence += 10

        return confidence.coerceIn(0, 100)
    }

    /**
     * Extrait les tendances de tension depuis l'historique
     */
    fun getVoltageTrends(history: List<BatteryHistory>): List<VoltageTrend> {
        return history
            .filter { it.eventType == BatteryEventType.VOLTAGE_READING && it.voltage != null }
            .sortedBy { it.timestamp }
            .map { VoltageTrend(it.timestamp.toLocalDate(), it.voltage!!) }
    }

    /**
     * Calcule le score de santé global de la flotte
     */
    fun calculateFleetHealth(
        batteries: List<Battery>,
        predictions: List<BatteryPrediction>
    ): FleetHealthScore {
        if (batteries.isEmpty()) {
            return FleetHealthScore(
                overallScore = 100, excellentCount = 0, goodCount = 0,
                fairCount = 0, poorCount = 0, criticalCount = 0,
                trend = FleetTrend.STABLE, nextReplacementDate = null,
                nextReplacementBattery = null
            )
        }

        val healths = batteries.map { it.getHealthPercentage() }
        val overallScore = healths.average().toInt()

        val excellentCount = healths.count { it > 80 }
        val goodCount = healths.count { it in 61..80 }
        val fairCount = healths.count { it in 41..60 }
        val poorCount = healths.count { it in 21..40 }
        val criticalCount = healths.count { it <= 20 }

        // Tendance basée sur l'âge moyen vs cycles
        val avgCycleRate = batteries.map {
            it.cycleCount.toFloat() / it.getAgeInDays().coerceAtLeast(1)
        }.average()

        val trend = when {
            avgCycleRate > 0.5 -> FleetTrend.DECLINING
            avgCycleRate < 0.1 -> FleetTrend.IMPROVING
            else -> FleetTrend.STABLE
        }

        // Prochaine batterie à remplacer
        val nextReplacement = predictions
            .filter { it.remainingDays > 0 }
            .minByOrNull { it.remainingDays }

        return FleetHealthScore(
            overallScore = overallScore,
            excellentCount = excellentCount,
            goodCount = goodCount,
            fairCount = fairCount,
            poorCount = poorCount,
            criticalCount = criticalCount,
            trend = trend,
            nextReplacementDate = nextReplacement?.estimatedEndOfLife,
            nextReplacementBattery = nextReplacement?.battery
        )
    }

    /**
     * Génère les données de heatmap d'activité depuis l'historique
     */
    fun generateActivityHeatmap(history: List<BatteryHistory>): List<WeeklyActivity> {
        val activityMap = mutableMapOf<Pair<Int, Int>, Int>()

        history.forEach { event ->
            val dayOfWeek = event.timestamp.dayOfWeek.value // 1=Lun, 7=Dim
            val hour = event.timestamp.hour
            val key = dayOfWeek to hour
            activityMap[key] = (activityMap[key] ?: 0) + 1
        }

        return activityMap.map { (key, count) ->
            WeeklyActivity(dayOfWeek = key.first, hourOfDay = key.second, count = count)
        }
    }
}
