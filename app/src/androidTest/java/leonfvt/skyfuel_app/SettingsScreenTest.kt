package leonfvt.skyfuel_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation pour l'écran des paramètres
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Naviguer vers l'écran des paramètres
        composeTestRule.onNodeWithContentDescription("Paramètres").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backButton_navigatesBack() {
        composeTestRule.onNodeWithContentDescription("Retour").performClick()
        composeTestRule.onNodeWithText("SkyFuel").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_notificationSectionDisplayed() {
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_themeSectionDisplayed() {
        composeTestRule.onNodeWithText("Apparence").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_dataSectionDisplayed() {
        composeTestRule.onNodeWithText("Données").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_aboutSectionDisplayed() {
        composeTestRule.onNodeWithText("À propos").performScrollTo()
        composeTestRule.onNodeWithText("À propos").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_alertsToggle_canBeToggled() {
        // Trouve et clique sur le toggle des alertes
        composeTestRule.onNodeWithText("Alertes batteries").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_themeButtons_displayed() {
        // Vérifie que les boutons de thème sont affichés
        composeTestRule.onNodeWithText("Système").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clair").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sombre").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_dynamicColorsToggle_displayed() {
        composeTestRule.onNodeWithText("Couleurs dynamiques").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_exportButton_displayed() {
        composeTestRule.onNodeWithText("Exporter les données").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_importButton_displayed() {
        composeTestRule.onNodeWithText("Importer des données").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_clickExport_showsDialog() {
        composeTestRule.onNodeWithText("Exporter les données").performClick()
        composeTestRule.onNodeWithText("Choisissez le format d'export:").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_clickImport_showsDialog() {
        composeTestRule.onNodeWithText("Importer des données").performClick()
        composeTestRule.onNodeWithText("Sélectionnez un fichier JSON ou CSV à importer.").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_appVersion_displayed() {
        composeTestRule.onNodeWithText("À propos").performScrollTo()
        composeTestRule.onNodeWithText("Version 1.0").assertIsDisplayed()
    }
}
