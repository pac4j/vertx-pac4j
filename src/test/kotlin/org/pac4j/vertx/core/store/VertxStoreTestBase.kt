package org.pac4j.vertx.core.store

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.pac4j.core.store.Store

/**
 *
 */
abstract class VertxStoreTestBase {

    companion object {
        const val ABSENT_KEY = "absentKey"
        const val PRESENT_KEY = "presentKey"
        const val INITIAL_VALUE = "initialValue"
        const val NEW_VALUE = "newValue"
    }

    abstract protected val store: Store<String, String>

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
        MatcherAssert.assertThat(store.get(ABSENT_KEY), CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPresent() {
        MatcherAssert.assertThat(store.get(PRESENT_KEY), CoreMatchers.`is`(INITIAL_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testSetPresent() {
        store.set(PRESENT_KEY, NEW_VALUE)
        MatcherAssert.assertThat(store.get(PRESENT_KEY), CoreMatchers.`is`(NEW_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testSetAbsent() {
        store.set(ABSENT_KEY, NEW_VALUE)
        MatcherAssert.assertThat(store.get(ABSENT_KEY), CoreMatchers.`is`(NEW_VALUE))
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveAbsent() {
        store.remove(ABSENT_KEY)
        MatcherAssert.assertThat(store.get(ABSENT_KEY), CoreMatchers.`is`(CoreMatchers.nullValue()))
    }
    @Test
    @Throws(Exception::class)
    fun testRemovePresent() {
        store.remove(PRESENT_KEY)
        MatcherAssert.assertThat(store.get(PRESENT_KEY), CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

}