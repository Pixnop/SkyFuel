# Processus CI/CD pour SkyFuel

Ce document explique le fonctionnement du pipeline d'intégration continue et de déploiement continu (CI/CD) mis en place pour l'application SkyFuel.

## Vue d'ensemble

Le projet SkyFuel utilise GitHub Actions pour automatiser :
- La compilation de l'application
- L'exécution des tests
- Les vérifications de qualité de code
- La génération des APK de debug

## Workflows disponibles

### 1. Android CI (`android.yml`)

Ce workflow s'exécute automatiquement sur :
- Les pushes vers les branches `main` et `dev`
- Les pull requests vers les branches `main` et `dev`
- Manuellement depuis l'interface GitHub Actions

**Étapes du workflow :**
1. Checkout du code source
2. Configuration de l'environnement Java 17
3. Génération de l'APK de debug (en évitant d'exécuter les tests)
4. Publication de l'APK comme artefact de build

Note: Les tests unitaires sont temporairement désactivés dans le workflow de CI jusqu'à ce que les problèmes d'annotation dans les fichiers de test soient résolus.

Pour télécharger l'APK généré après l'exécution du workflow :
1. Aller dans l'onglet "Actions" sur GitHub
2. Sélectionner l'exécution du workflow
3. Descendre jusqu'à la section "Artifacts"
4. Télécharger "app-debug"

### 2. Code Quality Check (`code-quality.yml`)

Ce workflow vérifie la qualité du code et s'exécute sur les mêmes déclencheurs que le workflow principal.

**Étapes du workflow :**
1. Checkout du code source
2. Configuration de l'environnement Java 17
3. Exécution de l'outil Lint d'Android
4. Publication du rapport Lint comme artefact

## Déclenchement manuel des workflows

Pour exécuter manuellement un workflow :
1. Aller dans l'onglet "Actions" sur GitHub
2. Sélectionner le workflow à exécuter
3. Cliquer sur "Run workflow"
4. Choisir la branche sur laquelle exécuter le workflow
5. Cliquer sur le bouton "Run workflow" vert

## Badges de statut

Les badges de statut dans le README.md indiquent l'état actuel des workflows CI/CD :
- ![Android CI](https://github.com/leonfvt/SkyFuel/workflows/Android%20CI/badge.svg) - Indique si la dernière compilation a réussi
- ![Code Quality](https://github.com/leonfvt/SkyFuel/workflows/Code%20Quality%20Check/badge.svg) - Indique si les vérifications de qualité de code ont réussi

## Dépannage

Si un workflow échoue, vérifiez les points suivants :

1. **Erreurs de compilation**
   - Vérifiez les logs d'erreur dans l'exécution du workflow
   - Corrigez les erreurs de compilation en local avant de pousser les modifications

2. **Échec des tests**
   - Consultez les rapports de test pour identifier les tests qui échouent
   - Exécutez les tests en local avec `./gradlew test`

3. **Problèmes de lint**
   - Consultez le rapport lint généré
   - Corrigez les problèmes signalés ou ajustez les règles lint si nécessaire

## Extension future

Voici les améliorations prévues pour le pipeline CI/CD :

1. Déploiement automatique vers Firebase App Distribution pour les tests
2. Génération et publication automatique des APK signés pour les versions de production
3. Automatisation des tests UI avec screenshot comparison
4. Intégration d'outils d'analyse de code supplémentaires (Detekt, SonarQube)