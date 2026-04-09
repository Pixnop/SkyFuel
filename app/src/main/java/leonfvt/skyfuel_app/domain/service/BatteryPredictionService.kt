package leonfvt.skyfuel_app.domain.service

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
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
 * Facteurs de stress détectés dans l'historique d'utilisation
 */
data class StressFactors(
    val fullChargeDays: Float,          // Jours cumulés passés à 100% charge
    val deepDischargeDays: Float,       // Jours cumulés passés déchargée
    val highFrequencyPeriods: Int,      // Nombre de périodes avec >2 cycles/jour
    val fullChargeStressPenalty: Float,  // % de pénalité capacité dû au stockage chargé
    val deepDischargeStressPenalty: Float, // % de pénalité dû à la décharge prolongée
    val highFrequencyPenalty: Float,    // % de pénalité dû à l'usage intensif
    val totalStressPenalty: Float       // Pénalité totale combinée en %
) {
    val hasWarnings: Boolean
        get() = fullChargeDays > 7 || deepDischargeDays > 3 || highFrequencyPeriods > 5
}

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
    val internalResistanceIncrease: Float,
    // Facteurs de stress
    val stressFactors: StressFactors
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
 *
 * Modèles scientifiques utilisés :
 * - Dégradation par cycles : Q_loss = B × N^z (Xu et al., Wang et al., Schmalstieg et al. 2014)
 *   Loi puissance calibrée par chimie, avec correction Arrhenius pour la température.
 *
 * - Vieillissement calendaire : Q_loss = A × SoC_factor × √t (Keil et al. 2016)
 *   Cinétique de croissance SEI (Solid Electrolyte Interphase), dépendant du SoC de stockage.
 *
 * - Facteur SoC stockage : SoC_factor = 1 + 2.13 × (SoC - 0.5) (Ecker et al. 2014)
 *   Stocker à 100% SoC accélère le vieillissement ×2.07 vs 50% SoC.
 *
 * - Décharge profonde : dissolution du cuivre sous 2.5V (Spotnitz 2003)
 *   Dommages irréversibles proportionnels au temps passé en décharge profonde.
 *
 * - Résistance interne : croissance SEI en √N (Safari & Delacourt 2011)
 *
 * Sources :
 * - Schmalstieg et al., J. Power Sources 257 (2014) 325-334
 * - Ecker et al., J. Power Sources 248 (2014) 839-851
 * - Safari & Delacourt, J. Electrochem. Soc. 158 (2011) A1123
 * - Spotnitz, J. Power Sources 113 (2003) 72-80
 * - Sauer & Wenzl, J. Power Sources 176 (2008) 534-546
 */
@Singleton
class BatteryPredictionService @Inject constructor() {

    companion object {
        // ── Constantes physiques ──
        const val R = 8.314       // Constante des gaz (J/(mol·K))
        const val T_REF = 298.15  // Température de référence 25°C (K)

        // ── Seuils opérationnels ──
        const val FULL_CHARGE_STRESS_HOURS = 24L
        const val DEEP_DISCHARGE_STRESS_HOURS = 48L
        const val HIGH_FREQUENCY_THRESHOLD = 2
        const val MAX_STRESS_PENALTY = 40f          // plafond réaliste basé sur la littérature

        const val CONFIDENCE_BASE = 30
        const val PROJECTION_FUTURE_MONTHS = 24
        const val EOL_THRESHOLD = 0.80f             // fin de vie = 80% de la capacité nominale (convention industrie)

        // ── Paramètres de dégradation par cycles ──
        // Calibrés pour que Q_loss = 20% au nombre de cycles typique de fin de vie à 25°C/1C
        // Formule : Q_loss_cycle = B_eff × N^z
        //
        //   LiPo  : ~20% loss à 300 cycles (z=0.55) → B = 0.20 / 300^0.55 = 0.00817
        //   Li-Ion: ~20% loss à 500 cycles (z=0.50) → B = 0.20 / 500^0.50 = 0.00894
        //   LiFe  : ~20% loss à 2000 cycles (z=0.50) → B = 0.20 / 2000^0.50 = 0.00447
        //   NiMH  : ~20% loss à 500 cycles (z=0.80) → B = 0.20 / 500^0.80 = 0.00135
        private data class CycleParams(val b: Double, val z: Double, val maxCycles: Int)
        private val CYCLE_PARAMS = mapOf(
            BatteryType.LIPO    to CycleParams(0.00817, 0.55, 400),
            BatteryType.LI_ION  to CycleParams(0.00894, 0.50, 650),
            BatteryType.NIMH    to CycleParams(0.00135, 0.80, 1000),
            BatteryType.LIFE    to CycleParams(0.00447, 0.50, 2000),
            BatteryType.OTHER   to CycleParams(0.00800, 0.55, 500)
        )

        // ── Paramètres de vieillissement calendaire ──
        // Perte annuelle à 25°C, 50% SoC, en fraction (√année)
        // Basé sur Ecker et al. 2014, Keil et al. 2016
        private val CALENDAR_LOSS_RATE = mapOf(
            BatteryType.LIPO    to 0.08,  // ~8% par √année
            BatteryType.LI_ION  to 0.05,  // ~5% par √année
            BatteryType.NIMH    to 0.06,  // ~6% (auto-décharge + dégradation)
            BatteryType.LIFE    to 0.025, // ~2.5% (très stable)
            BatteryType.OTHER   to 0.06
        )

        // ── Énergie d'activation Arrhenius (J/mol) ──
        // Pour correction température : factor = exp(Ea/R × (1/T_ref - 1/T))
        private val ACTIVATION_ENERGY = mapOf(
            BatteryType.LIPO    to 31500.0,  // Schmalstieg et al.
            BatteryType.LI_ION  to 31700.0,
            BatteryType.NIMH    to 25000.0,
            BatteryType.LIFE    to 22400.0,  // Safari & Delacourt
            BatteryType.OTHER   to 30000.0
        )

        // ── Augmentation max de résistance interne à fin de vie (%) ──
        // Safari & Delacourt 2011 : R_increase ~ √N relationship
        private val MAX_RESISTANCE_INCREASE = mapOf(
            BatteryType.LIPO    to 80.0,
            BatteryType.LI_ION  to 60.0,
            BatteryType.NIMH    to 40.0,
            BatteryType.LIFE    to 25.0,
            BatteryType.OTHER   to 70.0
        )

        // ── Facteur SoC pour vieillissement calendaire ──
        // Ecker et al. 2014 : linéaire en SoC, centré sur 50%
        // SoC_factor(1.0) = 2.065 (100% SoC → ×2 vs stockage idéal)
        // SoC_factor(0.5) = 1.0   (stockage idéal)
        // SoC_factor(0.0) = max(0.5, ...) (basse SoC = moins de stress SEI mais Cu dissolution)
        const val SOC_FACTOR_SLOPE = 2.13  // Ecker et al.

        // ── Pénalité décharge profonde ──
        // Dissolution du cuivre sous ~2.5V (Spotnitz 2003)
        // Dommage irréversible : ~0.12% de capacité perdue par jour de décharge profonde
        private val DEEP_DISCHARGE_RATE_PER_DAY = mapOf(
            BatteryType.LIPO    to 0.15,  // Très sensible, gonflement possible
            BatteryType.LI_ION  to 0.12,  // Cu dissolution (Spotnitz)
            BatteryType.NIMH    to 0.03,  // Peu sensible, pas de Cu
            BatteryType.LIFE    to 0.02,  // Très tolérant
            BatteryType.OTHER   to 0.10
        )

        // ── Facteur C-rate ──
        // Ning/Popov : chaque 1C au-delà de 1C ajoute ~20% de dégradation
        // Pour drones : C-rate typique = 2-8C en décharge
        const val C_RATE_FACTOR_SLOPE = 0.20  // +20% dégradation par C au-dessus de 1C
    }

    private fun getMaxCycles(type: BatteryType): Int = CYCLE_PARAMS[type]?.maxCycles ?: 500

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

        // Facteurs de stress depuis l'historique
        val stressFactors = analyzeStressFactors(battery, history)

        // Capacité réelle estimée (inclut les pénalités de stress)
        val baseRetention = estimateCapacityRetention(battery.type, battery.cycleCount, battery.getAgeInDays())
        val capacityRetention = (baseRetention - stressFactors.totalStressPenalty).coerceIn(0f, 100f)
        val estimatedCapacityMah = (battery.capacity * capacityRetention / 100f).toInt()
        val capacityCurve = generateCapacityCurve(battery, cyclesPerDay, maxCycles, stressFactors.totalStressPenalty)

        // Résistance interne (stress augmente aussi la résistance)
        val irIncrease = estimateResistanceIncrease(battery.type, battery.cycleCount) +
                stressFactors.totalStressPenalty * 0.5f // le stress augmente aussi la RI

        // Ajuster les jours restants en tenant compte du stress
        val stressedRemainingDays = (remainingDays * (1f - stressFactors.totalStressPenalty / 200f)).toLong().coerceAtLeast(0)
        val stressedEndOfLife = LocalDate.now().plusDays(stressedRemainingDays)

        return BatteryPrediction(
            battery = battery,
            estimatedEndOfLife = stressedEndOfLife,
            remainingDays = stressedRemainingDays,
            remainingCycles = remainingCycles,
            cyclesPerMonth = cyclesPerMonth,
            healthCurve = healthCurve,
            confidencePercent = confidencePercent,
            estimatedCapacityMah = estimatedCapacityMah,
            capacityRetentionPercent = capacityRetention,
            capacityCurve = capacityCurve,
            internalResistanceIncrease = irIncrease,
            stressFactors = stressFactors
        )
    }

    /**
     * Analyse l'historique pour détecter les facteurs de stress :
     * - Stockage à pleine charge prolongé (>24h à CHARGED sans utilisation)
     * - Décharge profonde prolongée (>48h à DISCHARGED)
     * - Usage intensif (>2 cycles/jour sur des périodes)
     */
    private fun analyzeStressFactors(battery: Battery, history: List<BatteryHistory>): StressFactors {
        val statusChanges = history
            .filter { it.eventType == BatteryEventType.STATUS_CHANGE && it.newStatus != null }
            .sortedBy { it.timestamp }

        var fullChargeDays = 0f
        var deepDischargeDays = 0f
        var highFrequencyPeriods = 0

        if (statusChanges.size >= 2) {
            for (i in 0 until statusChanges.size - 1) {
                val current = statusChanges[i]
                val next = statusChanges[i + 1]
                val durationHours = java.time.Duration.between(current.timestamp, next.timestamp).toHours()

                when (current.newStatus) {
                    // Stockage à pleine charge : chaque heure au-delà de 24h compte
                    BatteryStatus.CHARGED -> {
                        if (durationHours > FULL_CHARGE_STRESS_HOURS) {
                            fullChargeDays += (durationHours - FULL_CHARGE_STRESS_HOURS) / 24f
                        }
                    }
                    // Décharge prolongée : chaque heure au-delà de 48h compte
                    BatteryStatus.DISCHARGED -> {
                        if (durationHours > DEEP_DISCHARGE_STRESS_HOURS) {
                            deepDischargeDays += (durationHours - DEEP_DISCHARGE_STRESS_HOURS) / 24f
                        }
                    }
                    else -> { /* STORAGE et OUT_OF_SERVICE sont OK */ }
                }
            }

            // Vérifier l'état actuel (la batterie est peut-être restée chargée/déchargée depuis le dernier event)
            statusChanges.lastOrNull()?.let { lastEvent ->
                val hoursSinceLastEvent = java.time.Duration.between(
                    lastEvent.timestamp,
                    java.time.LocalDateTime.now()
                ).toHours()

                when (lastEvent.newStatus) {
                    BatteryStatus.CHARGED -> {
                        if (hoursSinceLastEvent > FULL_CHARGE_STRESS_HOURS) {
                            fullChargeDays += (hoursSinceLastEvent - FULL_CHARGE_STRESS_HOURS) / 24f
                        }
                    }
                    BatteryStatus.DISCHARGED -> {
                        if (hoursSinceLastEvent > DEEP_DISCHARGE_STRESS_HOURS) {
                            deepDischargeDays += (hoursSinceLastEvent - DEEP_DISCHARGE_STRESS_HOURS) / 24f
                        }
                    }
                    else -> {}
                }
            }
        } else {
            // Pas assez d'historique — estimer depuis les champs de la batterie
            battery.lastChargeDate?.let { chargeDate ->
                if (battery.status == BatteryStatus.CHARGED) {
                    val daysSinceCharge = ChronoUnit.DAYS.between(chargeDate, LocalDate.now())
                    if (daysSinceCharge > 1) fullChargeDays += (daysSinceCharge - 1).toFloat()
                }
            }
            battery.lastUseDate?.let { useDate ->
                if (battery.status == BatteryStatus.DISCHARGED) {
                    val daysSinceUse = ChronoUnit.DAYS.between(useDate, LocalDate.now())
                    if (daysSinceUse > 2) deepDischargeDays += (daysSinceUse - 2).toFloat()
                }
            }
        }

        // Détection d'usage intensif (>2 cycles/jour)
        val cycleEvents = history
            .filter { it.eventType == BatteryEventType.CYCLE_COMPLETED }
            .sortedBy { it.timestamp }

        if (cycleEvents.size >= 3) {
            val cyclesByDay = cycleEvents.groupBy { it.timestamp.toLocalDate() }
            highFrequencyPeriods = cyclesByDay.count { (_, cycles) -> cycles.size > HIGH_FREQUENCY_THRESHOLD }
        }

        // ── Pénalités basées sur la littérature scientifique ──

        // Stockage à pleine charge : Ecker et al. 2014
        // Le facteur SoC à 100% est ~2.07× vs 50% (idéal)
        // Pénalité = jours × (SoC_factor - 1) × taux calendaire journalier
        val calendarRatePerDay = (CALENDAR_LOSS_RATE[battery.type] ?: 0.06) / kotlin.math.sqrt(365.0)
        val socFactorFull = 1.0 + SOC_FACTOR_SLOPE * (1.0 - 0.5) // = 2.065
        val fullChargePenaltyRate = ((socFactorFull - 1.0) * calendarRatePerDay * 100.0).toFloat() // %/jour excédentaire

        // Décharge profonde : Spotnitz 2003 — dissolution Cu
        val deepDischargePenaltyRate = (DEEP_DISCHARGE_RATE_PER_DAY[battery.type] ?: 0.10).toFloat()

        // Usage intensif (C-rate élevé) : Ning/Popov
        // Chaque jour à >2 cycles ≈ opération à C-rate élevé
        val highFrequencyPenaltyRate = (C_RATE_FACTOR_SLOPE * 0.5).toFloat() // 0.10% par période intensive

        val fullChargePenalty = fullChargeDays * fullChargePenaltyRate
        val deepDischargePenalty = deepDischargeDays * deepDischargePenaltyRate
        val highFrequencyPenalty = highFrequencyPeriods * highFrequencyPenaltyRate
        val totalPenalty = (fullChargePenalty + deepDischargePenalty + highFrequencyPenalty).coerceAtMost(MAX_STRESS_PENALTY)

        return StressFactors(
            fullChargeDays = fullChargeDays,
            deepDischargeDays = deepDischargeDays,
            highFrequencyPeriods = highFrequencyPeriods,
            fullChargeStressPenalty = fullChargePenalty,
            deepDischargeStressPenalty = deepDischargePenalty,
            highFrequencyPenalty = highFrequencyPenalty,
            totalStressPenalty = totalPenalty
        )
    }

    /**
     * Estime le % de capacité restante basé sur le type, les cycles et l'âge.
     *
     * Modèle combiné validé par la littérature :
     *
     * 1. Dégradation par cycles (Schmalstieg et al. 2014, Wang et al. 2014) :
     *    Q_loss_cycle = B × N^z
     *    Loi puissance, où z < 1 donne une dégradation décélérante (SEI stabilisation),
     *    z > 0.5 pour NiMH modélise la dégradation plus régulière.
     *    B est calibré pour 20% de perte au nombre de cycles typique de fin de vie.
     *
     * 2. Vieillissement calendaire (Keil et al. 2016, Ecker et al. 2014) :
     *    Q_loss_cal = A × √(t_années)
     *    Cinétique de croissance de la couche SEI, proportionnelle à √t.
     *    Dépend du SoC de stockage : stockage à 50% = idéal, 100% = ×2.07 plus rapide.
     *
     * 3. Combinaison additive (convention industrielle) :
     *    SoH = 1.0 - Q_loss_cycle - Q_loss_cal
     *
     * @param type Chimie de la batterie
     * @param cycles Nombre de cycles complets effectués
     * @param ageDays Âge en jours depuis l'achat
     * @param temperatureC Température ambiante moyenne (défaut 25°C)
     * @return Capacité restante en % (0-100)
     */
    private fun estimateCapacityRetention(
        type: BatteryType,
        cycles: Int,
        ageDays: Long,
        temperatureC: Float = 25f
    ): Float {
        val params = CYCLE_PARAMS[type] ?: CYCLE_PARAMS[BatteryType.OTHER]!!

        // ── Correction Arrhenius pour la température ──
        // factor = exp(Ea/R × (1/T_ref - 1/T))
        // À 25°C : factor = 1.0 (référence)
        // À 35°C : factor ≈ 1.8-2.0 (dégradation accélérée)
        val ea = ACTIVATION_ENERGY[type] ?: 30000.0
        val tKelvin = temperatureC + 273.15
        val arrheniusFactor = kotlin.math.exp(ea / R * (1.0 / T_REF - 1.0 / tKelvin))

        // ── Perte par cycles : Q_loss = B × N^z × Arrhenius ──
        val cycleLoss = if (cycles > 0) {
            params.b * Math.pow(cycles.toDouble(), params.z) * arrheniusFactor
        } else 0.0

        // ── Perte calendaire : Q_loss = A × √(t_années) × Arrhenius ──
        val calendarRate = CALENDAR_LOSS_RATE[type] ?: 0.06
        val yearsElapsed = ageDays / 365.0
        val calendarLoss = calendarRate * kotlin.math.sqrt(yearsElapsed) * arrheniusFactor

        // ── Combinaison additive (standard industriel) ──
        val totalLoss = cycleLoss + calendarLoss
        val retention = (1.0 - totalLoss) * 100.0

        return retention.toFloat().coerceIn(0f, 100f)
    }

    /**
     * Génère la courbe de capacité en mAh au fil des cycles (passé + projection)
     */
    private fun generateCapacityCurve(
        battery: Battery,
        cyclesPerDay: Float,
        maxCycles: Int,
        stressPenalty: Float = 0f
    ): List<CapacityPoint> {
        val points = mutableListOf<CapacityPoint>()
        val nominalCapacity = battery.capacity

        // Points passés : un point tous les ~7% des cycles actuels
        val step = (battery.cycleCount / 15).coerceAtLeast(1)
        for (c in 0..battery.cycleCount step step) {
            val daysAtCycle = if (cyclesPerDay > 0) (c / cyclesPerDay).toLong() else 0L
            // Le stress s'accumule proportionnellement au cycle actuel
            val stressAtCycle = stressPenalty * (c.toFloat() / battery.cycleCount.coerceAtLeast(1))
            val retention = (estimateCapacityRetention(battery.type, c, daysAtCycle) - stressAtCycle).coerceIn(0f, 100f)
            points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = false))
        }
        // Point actuel exact (avec stress complet)
        val currentRetention = (estimateCapacityRetention(battery.type, battery.cycleCount, battery.getAgeInDays()) - stressPenalty).coerceIn(0f, 100f)
        points.add(CapacityPoint(battery.cycleCount, (nominalCapacity * currentRetention / 100f).toInt(), currentRetention, isPredicted = false))

        // Points futurs (le stress continue de s'accumuler proportionnellement)
        val futureStep = ((maxCycles - battery.cycleCount) / 10).coerceAtLeast(1)
        for (c in (battery.cycleCount + futureStep)..maxCycles step futureStep) {
            val daysAtCycle = if (cyclesPerDay > 0) (c / cyclesPerDay).toLong() else battery.getAgeInDays() + 365
            val futureStress = stressPenalty * (c.toFloat() / battery.cycleCount.coerceAtLeast(1))
            val retention = (estimateCapacityRetention(battery.type, c, daysAtCycle) - futureStress.coerceAtMost(stressPenalty * 1.5f)).coerceIn(0f, 100f)
            if (retention < 50f) {
                points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = true))
                break
            }
            points.add(CapacityPoint(c, (nominalCapacity * retention / 100f).toInt(), retention, isPredicted = true))
        }

        return points
    }

    /**
     * Estime l'augmentation de résistance interne en % vs neuf.
     *
     * Modèle : Safari & Delacourt 2011 (J. Electrochem. Soc.)
     * La résistance interne augmente proportionnellement à √N (croissance SEI)
     * puis s'accélère en fin de vie (perte de lithium actif).
     *
     * R_increase = R_max × (N / N_max)^0.5 + composante quadratique en fin de vie
     */
    private fun estimateResistanceIncrease(type: BatteryType, cycles: Int): Float {
        val maxCycles = getMaxCycles(type)
        val ratio = cycles.toFloat() / maxCycles
        val maxIncrease = (MAX_RESISTANCE_INCREASE[type] ?: 70.0).toFloat()

        // √N pour la croissance SEI + terme quadratique pour l'accélération fin de vie
        val seiGrowth = kotlin.math.sqrt(ratio.toDouble()).toFloat()
        val endOfLifeAcceleration = ratio * ratio
        val increase = maxIncrease * (0.7f * seiGrowth + 0.3f * endOfLifeAcceleration)

        return increase.coerceAtMost(200f)
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
        val futureMonths = PROJECTION_FUTURE_MONTHS
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
        var confidence = CONFIDENCE_BASE

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
