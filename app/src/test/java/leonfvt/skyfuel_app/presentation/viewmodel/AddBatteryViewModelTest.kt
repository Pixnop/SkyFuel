package leonfvt.skyfuel_app.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import leonfvt.skyfuel_app.domain.model.BatteryType
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
        val state = viewModel.state.first()
        
        // Vérifier l'état initial
        assertFalse(state.isSubmitting)
        assertNull(state.error)
        assertEquals("", state.brand)
        assertEquals("", state.model)
        assertEquals("", state.serialNumber)
        assertEquals(BatteryType.LIPO, state.type)
        assertEquals(0, state.cells)
        assertEquals(0, state.capacity)
        assertEquals(LocalDate.now(), state.purchaseDate)
        assertEquals("", state.notes)
    }

    @Test
    fun `updateBrand updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateBrand("DJI"))
        
        // Then
        val state = viewModel.state.first()
        assertEquals("DJI", state.brand)
    }

    @Test
    fun `updateModel updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateModel("Mavic 3"))
        
        // Then
        val state = viewModel.state.first()
        assertEquals("Mavic 3", state.brand)
    }

    @Test
    fun `updateSerialNumber updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateSerialNumber("SN123456"))
        
        // Then
        val state = viewModel.state.first()
        assertEquals("SN123456", state.serialNumber)
    }

    @Test
    fun `updateType updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateType(BatteryType.LI_ION))
        
        // Then
        val state = viewModel.state.first()
        assertEquals(BatteryType.LI_ION, state.type)
    }

    @Test
    fun `updateCells updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateCells("4"))
        
        // Then
        val state = viewModel.state.first()
        assertEquals(4, state.cells)
    }

    @Test
    fun `updateCapacity updates state correctly`() = coroutineRule.runTest {
        // When
        viewModel.onEvent(AddBatteryEvent.UpdateCapacity("5000"))
        
        // Then
        val state = viewModel.state.first()
        assertEquals(5000, state.capacity)
    }

    @Test
    fun `submit validates input and shows error on invalid input`() = coroutineRule.runTest {
        // When - champs obligatoires manquants
        viewModel.onEvent(AddBatteryEvent.Submit)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.first()
        assertNotNull(state.error)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `submit with valid input calls use case and navigates back`() = coroutineRule.runTest {
        // Given
        `when`(addBatteryUseCase.invoke(
            brand = "DJI",
            model = "Mavic 3", 
            serialNumber = "SN123456",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(),
            notes = "Test notes"
        )).thenReturn(1L)

        // When - remplir tous les champs requis
        viewModel.onEvent(AddBatteryEvent.UpdateBrand("DJI"))
        viewModel.onEvent(AddBatteryEvent.UpdateModel("Mavic 3"))
        viewModel.onEvent(AddBatteryEvent.UpdateSerialNumber("SN123456"))
        viewModel.onEvent(AddBatteryEvent.UpdateType(BatteryType.LIPO))
        viewModel.onEvent(AddBatteryEvent.UpdateCells("4"))
        viewModel.onEvent(AddBatteryEvent.UpdateCapacity("5000"))
        viewModel.onEvent(AddBatteryEvent.UpdateNotes("Test notes"))
        
        // Submit
        viewModel.onEvent(AddBatteryEvent.Submit)
        advanceUntilIdle()
        
        // Then
        verify(addBatteryUseCase).invoke(
            brand = "DJI",
            model = "Mavic 3", 
            serialNumber = "SN123456",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(),
            notes = "Test notes"
        )
        
        assertEquals("back", viewModel.navigationEvent.first())
    }
}