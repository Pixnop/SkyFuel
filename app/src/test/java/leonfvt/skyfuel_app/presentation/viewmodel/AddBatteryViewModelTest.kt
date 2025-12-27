package leonfvt.skyfuel_app.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.domain.usecase.AddBatteryUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryEvent
import leonfvt.skyfuel_app.util.CoroutineTestRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import java.time.LocalDate

@ExperimentalCoroutinesApi
class AddBatteryViewModelTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var viewModel: AddBatteryViewModel
    private lateinit var addBatteryUseCase: AddBatteryUseCase

    @Before
    fun setUp() {
        addBatteryUseCase = mock(AddBatteryUseCase::class.java)
        viewModel = AddBatteryViewModel(addBatteryUseCase)
    }

    @Test
    fun `initial state is valid`() = coroutineRule.runTest {
        advanceUntilIdle()
        val state = viewModel.state.value

        // Vérifier l'état initial
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        assertEquals("", state.brand)
        assertEquals("", state.model)
        assertEquals("", state.serialNumber)
        assertEquals(BatteryType.LIPO, state.batteryType)
        assertEquals("", state.cells)
        assertEquals("", state.capacity)
        assertEquals(LocalDate.now(), state.purchaseDate)
        assertEquals("", state.notes)
    }

    @Test
    fun `updateBrand updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateBrand("DJI"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("DJI", state.brand)
    }

    @Test
    fun `updateModel updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateModel("Mavic 3"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Mavic 3", state.model)
    }

    @Test
    fun `updateSerialNumber updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateSerialNumber("SN123456"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("SN123456", state.serialNumber)
    }

    @Test
    fun `updateBatteryType updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateBatteryType(BatteryType.LI_ION))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(BatteryType.LI_ION, state.batteryType)
    }

    @Test
    fun `updateCells updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateCells("4"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("4", state.cells)
    }

    @Test
    fun `updateCapacity updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateCapacity("5000"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("5000", state.capacity)
    }

    @Test
    fun `submit validates input and shows error on invalid input`() = coroutineRule.runTest {
        // When - champs obligatoires manquants
        viewModel.onEvent(AddBatteryEvent.SubmitBattery)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNotNull(state.errorMessage)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `submit with valid input updates state correctly`() = coroutineRule.runTest {
        // When - remplir tous les champs requis
        viewModel.onEvent(AddBatteryEvent.UpdateBrand("DJI"))
        viewModel.onEvent(AddBatteryEvent.UpdateModel("Mavic 3"))
        viewModel.onEvent(AddBatteryEvent.UpdateSerialNumber("SN123456"))
        viewModel.onEvent(AddBatteryEvent.UpdateBatteryType(BatteryType.LIPO))
        viewModel.onEvent(AddBatteryEvent.UpdateCells("4"))
        viewModel.onEvent(AddBatteryEvent.UpdateCapacity("5000"))
        viewModel.onEvent(AddBatteryEvent.UpdateNotes("Test notes"))
        advanceUntilIdle()

        // Then - verify state is correct before submit
        val state = viewModel.state.value
        assertEquals("DJI", state.brand)
        assertEquals("Mavic 3", state.model)
        assertEquals("SN123456", state.serialNumber)
        assertEquals(BatteryType.LIPO, state.batteryType)
        assertEquals("4", state.cells)
        assertEquals("5000", state.capacity)
        assertEquals("Test notes", state.notes)
    }
}
