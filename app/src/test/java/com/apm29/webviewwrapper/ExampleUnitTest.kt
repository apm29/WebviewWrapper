package com.apm29.webviewwrapper

import org.junit.Test

import org.junit.Assert.*
import java.net.URI
import java.net.URL

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val base = "http://www.baidu.com:9999/proxy/xxxxxx/"
        val oldUrl = URL("http://atcc-workshoptest.ciih.net/#/quality-analyse/pareto")
        val uri = "$base${oldUrl.path.substring(1)}${if (oldUrl.ref != null) "#" else ""}${oldUrl.ref ?: ""}"
        println(uri)
    }
}