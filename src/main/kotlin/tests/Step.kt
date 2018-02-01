package tests

import io.opentracing.util.GlobalTracer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Step(private val description: String) {
    val logger: Logger = LogManager.getLogger()

    fun <T> focus(body: () -> T): T {
        logger.info("<" + ("-".repeat(38)) + ">")
        val result =step(StepType.Focus, body)
        logger.info("got <$result>")
        logger.info("<" + ("-".repeat(38)) + ">")
        return result
    }

    fun <T> setup(body: () -> T): T {
        val result =step(StepType.Setup, body)
        logger.info("got <$result>")
        return result
    }

    fun <T> verification(body: () -> T): T {
        return step(StepType.Verification, body)
    }

    enum class StepType {
        Setup,
        Focus,
        Verification,
    }

    private fun <T> step(type: StepType, body: () -> T): T {
        logger.info("Beginning $type part of the test: $description")
        val parentSpan = GlobalTracer.get().activeSpan()
        val span = GlobalTracer.get().buildSpan(description)
                .withTag("test.step.type", "$type").asChildOf(parentSpan).start()
        GlobalTracer.get().scopeManager().activate(span, true).use {
            return try {
                body()
            } catch (e: Throwable) {
                it.span().setTag("error", e.toString())
                throw e
            } finally {
                logger.info("Done with $type part of the test")
            }
        }
    }

}
