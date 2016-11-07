package org.pac4j.vertx.handler.impl

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import org.hamcrest.Matcher
import org.hamcrest.core.Is
import org.pac4j.vertx.Pac4jAuthHandlerIntegrationTestBase
import java.util.function.Consumer

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
interface MultiProfileTest {

    companion object {
        private val LOG = LoggerFactory.getLogger(MultiProfileTest::class.java)
    }

    fun validateProfilesInBody(test: Pac4jAuthHandlerIntegrationTestBase, body: Buffer, validations: List<Pair<String, Consumer<JsonObject>>>) {

        LOG.info("Validating profiles in " + body)

        fun <T>assertThat(actual: T, matcher: Matcher<T>) = test.assertThat(actual, matcher)

        fun validateJsonObjectChild(jsonObject: JsonObject?, key: String, childValidator: Consumer<JsonObject>) {
            assertThat<Any>(jsonObject!!.containsKey(key), Is.`is`(true))
            childValidator.accept(jsonObject.getJsonObject(key))
        }

        val bodyAsJson = JsonObject(body.toString())
        assertThat(bodyAsJson.size(), Is.`is`(validations.size))

        validations.forEach {
            validateJsonObjectChild(bodyAsJson, it.first, it.second)
        }

    }

}