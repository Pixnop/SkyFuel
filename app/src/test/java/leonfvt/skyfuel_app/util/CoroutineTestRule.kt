package leonfvt.skyfuel_app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Règle JUnit qui configure les tests de coroutines avec un dispatcher de test
 * Utilise la bibliothèque kotlinx-coroutines-test pour faciliter les tests des coroutines
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    val testScope = TestScope(testDispatcher)

    override fun starting(description: Description) {
        super.starting(description)
        // Remplace le dispatcher principal par le dispatcher de test
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        // Restaure le dispatcher principal
        Dispatchers.resetMain()
    }

    /**
     * Exécute un bloc de code en avançant le temps virtuel jusqu'à ce que toutes les coroutines
     * soient terminées
     */
    fun runTest(block: suspend TestScope.() -> Unit) = testScope.runTest { block() }
}