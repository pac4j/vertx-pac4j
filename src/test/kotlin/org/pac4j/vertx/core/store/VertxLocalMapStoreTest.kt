package org.pac4j.vertx.core.store

import io.vertx.core.Vertx
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

/**
 *
 */
class VertxLocalMapStoreTest {

    companion object {
        const val ABSENT_KEY = "absentKey"
        const val PRESENT_KEY = "presentKey"
        const val INITIAL_VALUE = "initialValue"
        const val NEW_VALUE = "newValue"
    }

    val store = VertxLocalMapStore<String, String>(Vertx.vertx())

    @Before
    fun clearStore() {
        with(store) {
            set(PRESENT_KEY, INITIAL_VALUE)
            remove(ABSENT_KEY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetAbsent() {
        assertThat(store.get(ABSENT_KEY), `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPresent() {
        assertThat(store.get(PRESENT_KEY), `is`(INITIAL_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testSetPresent() {
        store.set(PRESENT_KEY, NEW_VALUE)
        assertThat(store.get(PRESENT_KEY), `is`(NEW_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testSetAbsent() {
        store.set(ABSENT_KEY, NEW_VALUE)
        assertThat(store.get(ABSENT_KEY), `is`(NEW_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveAbsent() {
        store.remove(ABSENT_KEY)
        assertThat(store.get(ABSENT_KEY), `is`(nullValue()))
    }
    @Test
    @Throws(Exception::class)
    fun testRemovePresent() {
        store.remove(PRESENT_KEY)
        assertThat(store.get(PRESENT_KEY), `is`(nullValue()))
    }

}