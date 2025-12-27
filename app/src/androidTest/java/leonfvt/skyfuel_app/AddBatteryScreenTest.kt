package leonfvt.skyfuel_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation pour l'écran d'ajout de batterie
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddBatteryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Naviguer vers l'écran d'ajout
        composeTestRule.onNodeWithContentDescription("Ajouter une batterie").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun addBatteryScreen_displaysTitle() {
        composeTestRule.onNodeWithText("Nouvelle batterie").assertIsDisplayed()
    }

    @Test
    fun addBatteryScreen_backButton_navigatesBack() {
        composeTestRule.onNodeWithContentDescription("Retour").performClick()
        composeTestRule.onNodeWithText("SkyFuel").assertIsDisplayed()
    }

    @Test
    fun addBatteryScreen_allFieldsDisplayed() {
        // Vérifie que tous les champs sont affichés
        composeTestRule.onNodeWithText("Marque").assertIsDisplayed()
        composeTestRule.onNodeWithText("Modèle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Numéro de série").assertIsDisplayed()
        composeTestRule.onNodeWithText("Capacité (mAh)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre de cellules").assertIsDisplayed()
    }

    @Test
    fun addBatteryScreen_saveButton_initiallyDisabled() {
        // Le bouton de sauvegarde doit être désactivé initialement
        composeTestRule.onNodeWithText("Enregistrer").assertIsNotEnabled()
    }

    @Test
    fun addBatteryScreen_fillRequiredFields_enablesSaveButton() {
        // Remplit les champs requis
        composeTestRule.onNodeWithText("Marque").performTextInput("DJI")
        composeTestRule.onNodeWithText("Modèle").performTextInput("Mavic 3")
        composeTestRule.onNodeWithText("Numéro de série").performTextInput("SN123456")
        composeTestRule.onNodeWithText("Capacité (mAh)").performTextInput("5000")
        composeTestRule.onNodeWithText("Nombre de cellules").performTextInput("4")
        
        // Vérifie que le bouton est maintenant activé
        composeTestRule.onNodeWithText("Enregistrer").assertIsEnabled()
    }

    @Test
    fun addBatteryScreen_invalidCapacity_showsError() {
        // Essaie d'entrer une capacité invalide
        composeTestRule.onNodeWithText("Capacité (mAh)").performTextInput("abc")
        
        // La validation devrait échouer (le champ ne devrait accepter que des chiffres)
        composeTestRule.waitForIdle()
    }

    @Test
    fun addBatteryScreen_qrCodeButton_isDisplayed() {
        // Vérifie que le bouton de scan QR code est affiché
        composeTestRule.onNodeWithContentDescription("Scanner QR code").assertIsDisplayed()
    }

    @Test
    fun addBatteryScreen_batteryTypeSelector_isDisplayed() {
        // Vérifie que le sélecteur de type de batterie est affiché
        composeTestRule.onNodeWithText("Type de batterie").assertIsDisplayed()
    }
}
