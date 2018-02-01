package tests

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.ThreadContext
import org.testng.ITestContext
import org.testng.ITestListener
import org.testng.ITestResult
import org.testng.annotations.BeforeClass
import org.testng.annotations.Listeners
import brave.Tracing
import brave.context.log4j2.ThreadContextCurrentTraceContext
import brave.opentracing.BraveTracer
import io.opentracing.util.GlobalTracer
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender


@Listeners(TestListener::class)
open class TestBase {

    @BeforeClass(alwaysRun = true)
    fun classSetUp() {
        ThreadContext.put("testSuite", this.javaClass.simpleName)

        val globalTracer = 1
        synchronized(globalTracer, {
            if (!GlobalTracer.isRegistered()) {
                val sender = OkHttpSender.create(
                        "http://localhost:7878/api/v1/spans")
                val reporter = AsyncReporter.builder(sender).build()
                val tracer = BraveTracer.create(Tracing.newBuilder()
                        .localServiceName("tests")
                        .currentTraceContext(ThreadContextCurrentTraceContext.create())
                        .spanReporter(reporter)
                        .build())

                if (!GlobalTracer.isRegistered()) {
                    GlobalTracer.register(tracer)
                }
            }
        })
    }

}

class TestListener: ITestListener {
    private val logger: Logger
        get() = LogManager.getLogger()

    override fun onTestSkipped(result: ITestResult) {
        printTestResults(result)
    }

    override fun onTestSuccess(result: ITestResult) {
        printTestResults(result)
    }

    private fun printStackTrace(t: Throwable) {
        t.toString().split(Regex("""\n\r|\n|\r""")).forEach { logger.error(it) }
        val trace = t.stackTrace
        for (traceElement in trace)
            logger.error("\tat " + traceElement)

        // Print suppressed exceptions, if any
        for (se in t.suppressed)
            printStackTrace(se)

        // Print cause, if any
        val ourCause = t.cause
        if (ourCause != null)
            printStackTrace(ourCause)
    }


    override fun onTestFailure(result: ITestResult) {
        printStackTrace(result.throwable)
        printTestResults(result)
    }

    private fun printTestResults(result: ITestResult) {
        val status = when(result.status) {
            ITestResult.SUCCESS -> "SUCCESS"
            ITestResult.FAILURE -> "FAILURE"
            ITestResult.SKIP -> "SKIP"
            else -> "UNKNOWN"
        }

        val span = GlobalTracer.get().activeSpan()
        span.setTag("test.result", status)
        span.finish()
        GlobalTracer.get().scopeManager().active().close()

        val parameters = result.parameters.joinToString(",") { it.toString() }
        val duration = result.endMillis - result.startMillis

        logger.info("")
        logger.info("~".repeat(40))
        logger.info("test: ${result.instanceName}.${result.name}($parameters)")
        logger.info("result: $status (${duration}ms)")
        logger.info("=".repeat(40))
        logger.info("")
    }

    override fun onTestFailedButWithinSuccessPercentage(result: ITestResult?) {
    }

    override fun onTestStart(result: ITestResult) {
        ThreadContext.put("testName", result.name)
        logger.info("")
        logger.info("=".repeat(40))
        logger.info("Starting test ${result.instanceName}.${result.name}")

        val name = "[A-Z\\d]".toRegex().replace(result.name, { " ${it.value}" })
        val span= GlobalTracer.get().buildSpan("${result.instanceName} - $name").withTag("type", "test").start()
        GlobalTracer.get().scopeManager().activate(span, false)
    }

    override fun onStart(context: ITestContext?) {
    }

    override fun onFinish(context: ITestContext?) {
    }
}
