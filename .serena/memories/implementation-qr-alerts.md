# Implémentation QR Scanner et Alertes - Décembre 2025

## Améliorations QR Scanner

### Fichiers modifiés
- `util/ScanFeedback.kt` - Nouveau: feedback haptic + sonore
- `presentation/component/QrCodeScanner.kt` - Amélioré: cooldown, animation, feedback

### Fonctionnalités ajoutées
1. **Feedback haptic**: Vibration pattern de succès (50-50-50ms)
2. **Son**: ToneGenerator avec TONE_PROP_ACK
3. **Cooldown**: 2 secondes entre les scans (configurable)
4. **Animation de succès**: Scale + fade avec checkmark vert
5. **Protection anti-doublons**: Vérification du dernier code scanné

## Système d'Alertes

### Nouveaux fichiers
- `domain/model/BatteryAlert.kt` - Modèle d'alerte
- `domain/service/AlertService.kt` - Logique de détection des alertes
- `domain/usecase/GetBatteryAlertsUseCase.kt` - UseCase pour récupérer les alertes
- `presentation/component/AlertBanner.kt` - UI des alertes (banner, cards, section)
- `worker/BatteryAlertWorker.kt` - Worker pour notifications push
- `util/NotificationHelper.kt` - Helper pour notifications Android

### Types d'alertes
1. **NEEDS_CHARGING**: Batterie déchargée >7 jours
2. **LOW_HEALTH**: Santé <20%
3. **MAINTENANCE_DUE**: Maintenance tous les 90 jours ou 50 cycles
4. **HIGH_CYCLE_COUNT**: Cycles >80% du max recommandé par type

### Priorités
- CRITICAL: Rouge foncé, action urgente
- HIGH: Rouge, important
- MEDIUM: Orange, attention
- LOW: Bleu, information

### Notifications Push
- Vérification quotidienne via WorkManager
- Notifications groupées avec résumé
- Intégration Hilt via @HiltWorker

## Fichiers modifiés existants
- `build.gradle.kts` - Ajout WorkManager
- `libs.versions.toml` - Dépendances WorkManager
- `SkyFuelApplication.kt` - Init notifications + Worker scheduling
- `presentation/viewmodel/HomeViewModel.kt` - Intégration alertes
- `presentation/viewmodel/state/BatteryListState.kt` - Ajout champ alerts
- `presentation/screen/HomeScreen.kt` - Affichage AlertsSection
