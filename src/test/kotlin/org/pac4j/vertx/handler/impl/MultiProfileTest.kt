package org.pac4j.vertx.handler.impl

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import org.hamcrest.Matcher
import org.hamcrest.core.Is
import java.util.function.Consumer

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
interface MultiProfileTest {

    companion object {
        private val LOG = LoggerFactory.getLogger(MultiProfileTest::class.java)
    }

    /**
     * Requires assertThat to be exposed by test implementing interface so we can use it. We may move that into its
     * own interface later if we also create another test interface which uses it, but for now it's reasonable to keep it
     * here.
     */
    fun <T>assertThat(actual: T, matcher: Matcher<T>);

    fun validateProfilesInBody(body: Buffer, validations: List<Pair<String, Consumer<JsonObject>>>) {

        LOG.info("Validating profiles in " + body)

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