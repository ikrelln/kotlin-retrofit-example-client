package http

import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import org.apache.logging.log4j.LogManager
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.lang.reflect.Proxy

enum class Service {
    Github
}

annotation class TargetService(val value: Service)

data class Endpoint(val method: String, val path: String, val service: Service?)

object CallReporter {
    private val logger = LogManager.getLogger()

    private fun <T, R: Call<T>> wrapHttpCall(call: R, endpoint: Endpoint? = null): R {
        val proxy = Proxy.newProxyInstance(call::class.java.classLoader, call.javaClass.interfaces) { _, method, args ->
            if (method.name == "execute") {
                val parentSpan = GlobalTracer.get().activeSpan()
                val span = GlobalTracer.get().buildSpan("http call ${endpoint?.service} ${endpoint?.method} ${endpoint?.path}")
                        .withTag("test.step.type", "HTTP_CALL")
                        .withTag(Tags.SPAN_KIND.key, Tags.SPAN_KIND_CLIENT)
                        .withTag(Tags.HTTP_METHOD.key, endpoint?.method)
                        .withTag(Tags.HTTP_URL.key, endpoint?.path)
                        .asChildOf(parentSpan).start()
                GlobalTracer.get().scopeManager().activate(span, true).use {
                    val result = if (args != null) method.invoke(call, *args) else method.invoke(call)
                    it.span().setTag(Tags.HTTP_STATUS.key, (result as? Response<*>)?.code())
                            .setTag(Tags.PEER_SERVICE.key, "${endpoint?.service}")

                    result
                }
            } else {
                if (args != null) method.invoke(call, *args) else method.invoke(call)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return proxy as R
    }

    fun <T> create(service: Class<T>, instance: T): T {
        val proxy = Proxy.newProxyInstance(service.classLoader, arrayOf<Class<*>>(service)) { _, method, args ->
            var result = if (args != null) method.invoke(instance, *args) else method.invoke(instance)
            if (result is Call<*>) {
                val serviceName = method.annotations
                        .filterIsInstance<TargetService>()
                        .firstOrNull()?.value
                val endpoint = method.annotations.mapNotNull {
                    when(it) {
                        is GET -> Endpoint("GET", it.value, serviceName)
                        is POST -> Endpoint("POST", it.value, serviceName)
                        is PUT -> Endpoint("PUT", it.value, serviceName)
                        is PATCH -> Endpoint("PATCH", it.value, serviceName)
                        is DELETE -> Endpoint("DELETE", it.value, serviceName)
                        is HEAD -> Endpoint("HEAD", it.value, serviceName)
                        is OPTIONS -> Endpoint("OPTIONS", it.value, serviceName)
                        else -> null
                    }
                }.firstOrNull()
                @Suppress("UNCHECKED_CAST")
                result = wrapHttpCall(result as Call<T>, endpoint)
            }

            result
        }

        @Suppress("UNCHECKED_CAST")
        return proxy as T
    }
}

