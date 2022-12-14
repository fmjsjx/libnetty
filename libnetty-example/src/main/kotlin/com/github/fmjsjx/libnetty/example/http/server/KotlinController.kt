package com.github.fmjsjx.libnetty.example.http.server

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

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

}