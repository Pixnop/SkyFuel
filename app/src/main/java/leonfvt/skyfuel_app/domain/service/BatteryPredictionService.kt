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
 * Point de données pour la courbe de capacité
 */
data class CapacityPoint(
    val cycleNumber: Int,
    val capacityMah: Int,
    val capacityPercent: Float,
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
    val confidencePercent: Int,
    // Capacité réelle
    val estimatedCapacityMah: Int,
    val capacityRetentionPercent: Float,
    val capacityCurve: List<CapacityPoint>,
    // Résistance interne estimée
    val internalResistanceIncrease: Float // % d'augmentation vs neuf
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

        // Capacité réelle estimée
        val capacityRetention = estimateCapacityRetention(battery.type, battery.cycleCount, battery.getAgeInDays())
        val estimatedCapacityMah = (battery.capacity * capacityRetention / 100f).toInt()
        val capacityCurve = generateCapacityCurve(battery, cyclesPerDay, maxCycles)

        // Résistance interne
        val irIncrease = estimateResistanceIncrease(battery.type, battery.cycleCount)

        return BatteryPrediction(
            battery = battery,
            estimatedEndOfLife = estimatedEndOfLife,
            remainingDays = remainingDays,
            remainingCycles = remainingCycles,
            cyclesPerMonth = cyclesPerMonth,
            healthCurve = healthCurve,
            confidencePercent = confidencePercent,
            estimatedCapacityMah = estimatedCapacityMah,
            capacityRetentionPercent = capacityRetention,
            capacityCurve = capacityCurve,
            internalResistanceIncrease = irIncrease
        )
    }

    /**
     * Estime le % de capacité restante basé sur le type, les cycles et l'âge.
     *
     * Utilise un modèle NON-LINÉAIRE réaliste :
     * - Phase 1 (0 à ~70% des cycles max) : dégradation lente, quasi-linéaire
     * - Phase 2 (70-100% des cycles max) : dégradation accélérée ("knee point")
     *
     * Formule : retention = 100 * exp(-k * (cycles/maxCycles)^n)
     * où k et n varient par type de batterie pour modéliser le "coude"
     *
     * + vieillissement calendaire (non-linéaire aussi, sqrt du temps)
     */
    private fun estimateCapacityRetention(type: BatteryType, cycles: Int, ageDays: Long): Float {
        val maxCycles = getMaxCycles(type)
        val cycleRatio = cycles.toFloat() / maxCycles

        // Paramètres de la courbe de dégradation par type
        // k = intensité de la dégradation, n = exposant (>1 = accélération en fin de vie)
        val (k, n) = when (type) {
            BatteryType.LIPO -> 0.7f to 2.2f    // Coude marqué, décroche vite après 70%
            BatteryType.LI_ION -> 0.5f to 1.8f  // Plus progressif
            BatteryType.NIMH -> 0.3f to 1.5f     // Dégradation douce
            BatteryType.LIFE -> 0.2f to 1.3f     // Très stable, presque linéaire
            BatteryType.OTHER -> 0.5f to 2.0f
        }

        // Dégradation par cycles (exponentielle avec accélération)
        val cycleDegradation = kotlin.math.exp(-k * Math.pow(cycleRatio.toDouble(), n.toDouble())).toFloat()

        // Vieillissement calendaire : sqrt du temps (ralentit avec l'âge)
        // Perte de ~5-15% sur les 3 premières années selon le type
        val calendarLossRate = when (type) {
            BatteryType.LIPO -> 0.08f   // ~8% par sqrt(année)
            BatteryType.LI_ION -> 0.05f
            BatteryType.NIMH -> 0.03f
            BatteryType.LIFE -> 0.02f
            BatteryType.OTHER -> 0.06f
        }
        val yearsElapsed = ageDays / 365f
        val calendarRetention = 1f - calendarLossRate * kotlin.math.sqrt(yearsElapsed.toDouble()).toFloat()

        return (cycleDegradation * calendarRetention * 100f).coerceIn(0f, 100f)
    }

    /**
     * Génère la courbe de capacité en mAh au fil des cycles (passé + projection)
     */
    private fun generateCapacityCurve(
        battery: Battery,
        cyclesPerDay: Float,
        maxCycles: Int
    ): List<CapacityPoint> {
        val points = mutableListOf<CapacityPoint>()
        val nominalCapacity = battery.capacity

        // Points passés : un point tous les 10% des cycles actuels
        val step = (battery.cycleCount / 15).coerceAtLeast(1)
        for (c in 0..battery.cycleCount step step) {
            val daysAtCycle = if (cyclesPerDay > 0) (c / cyclesPerDay).toLong() else 0L
            val retention = estimateCapacityRetention(battery.type, c, daysAtCycle)
            points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = false))
        }
        // Point actuel exact
        val currentRetention = estimateCapacityRetention(battery.type, battery.cycleCount, battery.getAgeInDays())
        points.add(CapacityPoint(battery.cycleCount, (nominalCapacity * currentRetention / 100f).toInt(), currentRetention, isPredicted = false))

        // Points futurs
        val futureStep = ((maxCycles - battery.cycleCount) / 10).coerceAtLeast(1)
        for (c in (battery.cycleCount + futureStep)..maxCycles step futureStep) {
            val daysAtCycle = if (cyclesPerDay > 0) (c / cyclesPerDay).toLong() else battery.getAgeInDays() + 365
            val retention = estimateCapacityRetention(battery.type, c, daysAtCycle)
            if (retention < 50f) { // On arrête quand la batterie est inutilisable (<50%)
                points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = true))
                break
            }
            points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = true))
        }

        return points
    }

    /**
     * Estime l'augmentation de résistance interne en % vs neuf.
     * La résistance augmente de façon quadratique avec les cycles
     * (accélération en fin de vie, comme la capacité).
     */
    private fun estimateResistanceIncrease(type: BatteryType, cycles: Int): Float {
        val maxCycles = getMaxCycles(type)
        val ratio = cycles.toFloat() / maxCycles

        // Augmentation quadratique : lente au début, rapide en fin de vie
        val maxIncrease = when (type) {
            BatteryType.LIPO -> 80f    // +80% à fin de vie
            BatteryType.LI_ION -> 60f
            BatteryType.NIMH -> 40f
            BatteryType.LIFE -> 25f
            BatteryType.OTHER -> 70f
        }
        return (maxIncrease * ratio * ratio).coerceAtMost(200f)
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
     * Simule la santé à un moment donné (utilise le même modèle non-linéaire)
     */
    private fun simulateHealth(type: BatteryType, cycles: Int, daysElapsed: Float): Float {
        return estimateCapacityRetention(type, cycles, daysElapsed.toLong())
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
