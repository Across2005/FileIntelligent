package com.crossk.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset

/**
 * P0-3 (CR-3): encoding detection for FileParserEngine.
 *
 * Verifies the decodeWithEncodingDetection helper handles:
 * - UTF-8 with BOM (BOM must be stripped)
 * - UTF-8 without BOM (default case)
 * - GBK (Chinese Windows legacy files; no BOM)
 *
 * Uses Robolectric so the engine can be instantiated with an Application
 * context. Fixtures are written to a temp directory in @Before; the
 * decode helper is tested directly so we don't depend on the engine's
 * content-resolver plumbing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileParserEngineEncodingTest {

    private lateinit var engine: FileParserEngine
    private lateinit var dir: File

    @Before fun setUp() {
        engine = FileParserEngine(ApplicationProvider.getApplicationContext<Context>())
        dir = File("build/test-encodings").apply { mkdirs() }
        File(dir, "utf8-bom.txt").writeBytes(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                "你好，世界\nHello".toByteArray(Charsets.UTF_8),
        )
        File(dir, "utf8-no-bom.txt").writeBytes(
            "你好，世界\nHello".toByteArray(Charsets.UTF_8),
        )
        File(dir, "gbk.txt").writeBytes(
            "你好，世界\nHello".toByteArray(Charset.forName("GBK")),
        )
    }

    private fun uri(name: String): Uri = Uri.fromFile(File(dir, name))

    @Test
    fun utf8BomIsStripped(): Unit {
        val decoded = decodeWithEncodingDetection(
            File(dir, "utf8-bom.txt").inputStream(),
        )
        assertThat(decoded).startsWith("你好")
        assertThat(decoded).doesNotContain("\uFEFF")
    }

    @Test
    fun utf8NoBomDecodes(): Unit {
        val decoded = decodeWithEncodingDetection(
            File(dir, "utf8-no-bom.txt").inputStream(),
        )
        assertThat(decoded).startsWith("你好")
        assertThat(decoded).doesNotContain("\uFFFD")
    }

    @Test
    fun gbkFallsBackFromUtf8(): Unit {
        // GBK file has no BOM and bytes that look like garbage to UTF-8 decoder.
        // Our fallback should detect this and decode as GB18030 (superset of GBK).
        val decoded = decodeWithEncodingDetection(
            File(dir, "gbk.txt").inputStream(),
        )
        assertThat(decoded).startsWith("你好")
        assertThat(decoded).doesNotContain("\uFFFD")
    }

    @Test
    fun utf8PureAsciiDoesNotFallBack(): Unit {
        val ascii = "Hello, world!\nLine 2."
        val decoded = decodeWithEncodingDetection(
            ByteArrayInputStream(ascii.toByteArray(Charsets.UTF_8)),
        )
        assertThat(decoded).isEqualTo(ascii)
    }

    @Test
    fun emptyStreamReturnsEmpty(): Unit {
        val decoded = decodeWithEncodingDetection(ByteArrayInputStream(ByteArray(0)))
        assertThat(decoded).isEqualTo("")
    }
}
