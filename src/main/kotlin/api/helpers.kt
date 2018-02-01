package api

import http.*
import io.opentracing.contrib.okhttp3.OkHttpClientSpanDecorator.STANDARD_TAGS
import io.opentracing.util.GlobalTracer
import okhttp3.OkHttpClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import io.opentracing.contrib.okhttp3.TracingInterceptor;


object ApiService {
    val logger: Logger = LogManager.getLogger()

    fun <T> create(service: Class<T>, baseUrl: String): T {
        val tracing = TracingInterceptor(
                GlobalTracer.get(), listOf(STANDARD_TAGS))

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(tracing)
        httpClient.addNetworkInterceptor(tracing)

        val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(httpClient.build())
                .build()

        var retrofitService = retrofit.create(service)
        retrofitService = CallReporter.create(service, retrofitService)
        return retrofitService
    }
}
