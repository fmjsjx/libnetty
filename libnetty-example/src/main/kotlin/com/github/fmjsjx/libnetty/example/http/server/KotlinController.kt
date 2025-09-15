package com.github.fmjsjx.libnetty.example.http.server

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.fmjsjx.libcommon.json.toFastjson2Bytes
import com.github.fmjsjx.libnetty.example.http.server.TestController.SSE_EVENT_CLOSE
import com.github.fmjsjx.libnetty.example.http.server.TestController.SSE_EVENT_OPEN
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody
import com.github.fmjsjx.libnetty.http.server.annotation.QueryVar
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException
import com.github.fmjsjx.libnetty.http.server.sse.SseEventBuilder
import com.github.fmjsjx.libnetty.http.server.sse.SseEventStream
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import io.netty.util.AsciiString
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@HttpPath("/api/kotlin")
class KotlinController {

    private val logger = LoggerFactory.getLogger(KotlinController::class.java)!!

    @HttpGet("/jsons")
    @JsonBody
    suspend fun getJsons(query: QueryStringDecoder): Any {
        // GET /api/jsons
        logger.info("-- kotlin jsons --")
        delay(1)
        logger.info("-- delayed 1 millisecond --")
        val node = JsonNodeFactory.instance.objectNode()
        query.parameters().forEach { (key: String?, values: List<String?>) ->
            if (values.size == 1) {
                node.put(key, values[0])
            } else {
                node.putPOJO(key, values)
            }
        }
        if (node.isEmpty) {
            throw ManualHttpFailureException(
                HttpResponseStatus.BAD_REQUEST, "{\"code\":1,\"message\":\"Missing Query String\"}",
                HttpHeaderValues.APPLICATION_JSON, "Missing Query String"
            )
        } else {
            return node
        }
    }

    @HttpGet("/sse-event-stream")
    suspend fun HttpRequestContext.getSseEventStream(
        @QueryVar("len", required = false) len: Int?,
    ): SseEventStream {
        // GET /api/test/sse-event-stream
        println("-- test sse-event-stream --")
        println(channel())
        val messageSize = len ?: 100
        val eventLoop = eventLoop()
        val running = AtomicBoolean(false)
        val uuid = java.util.UUID.randomUUID().toString()
        return eventStreamBuilder().autoPing(Duration.ofSeconds(30)).onError { _, cause ->
            System.err.println("error occurs on SSE event stream")
            cause.printStackTrace()
            running.set(false)
        }.onActive { stream ->
            running.set(true)
            // event: open\n\n
            // data: {"session":"$uuid"}
            val sessionData = AsciiString(mapOf("session" to uuid).toFastjson2Bytes())
            stream.sendEvent(SseEventBuilder.create().event(SSE_EVENT_OPEN).data(sessionData))
            val writeStreamTask = object : Runnable {
                private var n: Int = 0
                override fun run() {
                    if (!running.get()) {
                        System.err.println("Abnormal interruption of event stream")
                        return
                    }
                    if (n++ < messageSize) {
                        val data = AsciiString(mapOf("line" to n).toFastjson2Bytes())
                        stream.sendEvent(SseEventBuilder.message(data))
                        eventLoop.schedule(this, 1000, TimeUnit.MILLISECONDS)
                    } else {
                        stream.sendEvent(SseEventBuilder.create().event(SSE_EVENT_CLOSE).data(sessionData))
                        stream.close()
                    }
                }
            }
            eventLoop.schedule(writeStreamTask, 1000, TimeUnit.MILLISECONDS)
        }.build()
    }

}