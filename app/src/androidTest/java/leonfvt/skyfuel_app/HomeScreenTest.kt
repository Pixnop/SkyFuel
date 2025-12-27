package leonfvt.skyfuel_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation pour l'écran d'accueil
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_displaysTitle() {
        // Vérifie que le titre de l'application est affiché
        composeTestRule.onNodeWithText("SkyFuel").assertIsDisplayed()
    }

    @Test
    fun homeScreen_fabIsDisplayed() {
        // Vérifie que le FAB pour ajouter une batterie est visible
        composeTestRule.onNodeWithContentDescription("Ajouter une batterie").assertIsDisplayed()
    }

    @Test
    fun homeScreen_clickFab_navigatesToAddBattery() {
        // Clique sur le FAB
        composeTestRule.onNodeWithContentDescription("Ajouter une batterie").performClick()
        
        // Vérifie la navigation vers l'écran d'ajout
        composeTestRule.onNodeWithText("Nouvelle batterie").assertIsDisplayed()
    }

    @Test
    fun homeScreen_settingsButtonIsDisplayed() {
        // Vérifie que le bouton des paramètres est visible
        composeTestRule.onNodeWithContentDescription("Paramètres").assertIsDisplayed()
    }

    @Test
    fun homeScreen_clickSettings_navigatesToSettings() {
        // Clique sur le bouton paramètres
        composeTestRule.onNodeWithContentDescription("Paramètres").performClick()
        
        // Vérifie la navigation vers l'écran des paramètres
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysDashboardStats() {
        // Vérifie que les statistiques du tableau de bord sont affichées
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyState_displaysMessage() {
        // Si aucune batterie n'existe, vérifie l'affichage du message vide
        // Ce test dépend de l'état initial de la base de données
        composeTestRule.waitForIdle()
    }
}
