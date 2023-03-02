package example.armeria.grpc

import kotlinx.coroutines.CoroutineExceptionHandler
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.*

class CoroutineExceptionRule(
    private val exceptions: MutableList<Throwable> = Collections.synchronizedList(mutableListOf<Throwable>())
) : TestWatcher(),
    CoroutineExceptionHandler by (CoroutineExceptionHandler { _, exception ->
        exceptions.add(exception)
    }) {

    override fun starting(description: Description) {
        exceptions.clear()
    }

    override fun finished(description: Description) {
        exceptions.forEach { throw AssertionError(it) }
    }
}