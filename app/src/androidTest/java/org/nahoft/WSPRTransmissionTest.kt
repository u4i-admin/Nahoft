package org.nahoft

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoho.android.usbserial.driver.UsbSerialDriver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.operatorfoundation.ion.storage.NounType
import org.operatorfoundation.ion.storage.Word
import timber.log.Timber
import org.operatorfoundation.transmission.*

@RunWith(AndroidJUnit4::class)
class WSPRTransmissionTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testWSPRTransmission() = runBlocking {
        Timber.plant(Timber.DebugTree())

        Timber.d("========================================")
        Timber.d("WSPR Transmission Test Starting")
        Timber.d("========================================")

        val toneCount = 162
        //val toneCount = 2
        val on = Word.make(1, NounType.INTEGER.value)
        val off = Word.make(0, NounType.INTEGER.value)
        val toneA = Word.make(1014850000, NounType.INTEGER.value)
        val toneB = Word.make(1014860000, NounType.INTEGER.value)

        Timber.d("Created control words: ON, OFF, ToneA, ToneB")

        // Get USB connection
        Timber.d("Creating SerialConnectionFactory...")
        val factory = SerialConnectionFactory(context)

        Timber.d("Searching for USB devices...")
        val devices = factory.findAvailableDevices()

        if (devices.isEmpty()) {
            Timber.e("❌ No USB devices found!")
            assert(false) { "No USB devices found" }
            return@runBlocking
        }

        Timber.d("✓ Found ${devices.size} device(s)")
        val driver = devices.first()
        Timber.d("Selected device: ${driver.device.deviceName}")

        // Create connection with retry for permission
        Timber.d("Attempting to create connection (may require permission)...")
        val connection = createTestConnection(factory, driver)

        if (connection == null) {
            Timber.e("❌ Failed to create connection after retries")
            assert(false) { "Failed to create connection" }
            return@runBlocking
        }

        Timber.d("✓ Connection established successfully")
        Timber.d("========================================")
        Timber.d("Starting transmission sequence")
        Timber.d("========================================")

        // Turn transmitter on
        Timber.d("→ Sending transmitter ON command...")
        Word.to_conn(connection, on)
        Timber.d("✓ Transmitter ON")

        // Send tones
        for (i in 0 until toneCount) {
            val tone = if (i % 2 == 0) toneA else toneB
            val frequency = if (i % 2 == 0) 1014850000 else 1014860000

            Timber.d("→ Tone $i/$toneCount: ${if (i % 2 == 0) "A" else "B"} ($frequency Hz)")

            try {
                Word.to_conn(connection, tone)
                Timber.v("  ✓ Tone $i sent successfully")
            } catch (e: Exception) {
                Timber.e(e, "  ❌ Failed to send tone $i")
                throw e
            }

            delay(683)
        }

        Timber.d("========================================")
        Timber.d("All tones sent, shutting down")
        Timber.d("========================================")

        // Turn transmitter off
        Timber.d("→ Sending transmitter OFF command...")
        Word.to_conn(connection, off)
        Timber.d("✓ Transmitter OFF")

        connection.close()
        Timber.d("✓ Connection closed")
        Timber.d("========================================")
        Timber.d("Test completed successfully!")
        Timber.d("========================================")
    }

    private suspend fun createTestConnection(
        factory: SerialConnectionFactory,
        driver: UsbSerialDriver
    ): SerialConnection? {
        return try {
            Timber.d("Initiating connection to device...")

            // This will trigger permission dialog if needed
            factory.createConnection(driver.device)
            Timber.d("Connection request sent, waiting for permission...")

            // Give user time to click permission dialog
            Timber.d("Waiting 5 seconds for permission dialog...")
            delay(5000)

            // Wait for connection to establish with detailed status
            var connection: SerialConnection? = null
            var attempts = 0
            val maxAttempts = 50 // 5 seconds total (50 * 100ms)

            Timber.d("Polling connection state (max ${maxAttempts * 100}ms)...")

            while (connection == null && attempts < maxAttempts) {
                val state = factory.connectionState.value

                when (state) {
                    is SerialConnectionFactory.ConnectionState.Connected -> {
                        connection = state.connection
                        Timber.d("✓ Connection established after ${attempts * 100}ms")
                    }
                    is SerialConnectionFactory.ConnectionState.Connecting -> {
                        if (attempts % 10 == 0) { // Log every second
                            Timber.d("  Still connecting... (${attempts * 100}ms elapsed)")
                        }
                    }
                    is SerialConnectionFactory.ConnectionState.RequestingPermission -> {
                        if (attempts % 10 == 0) {
                            Timber.d("  Waiting for permission... (${attempts * 100}ms elapsed)")
                        }
                    }
                    is SerialConnectionFactory.ConnectionState.Disconnected -> {
                        Timber.w("  Connection state: Disconnected")
                    }
                    is SerialConnectionFactory.ConnectionState.Error -> {
                        Timber.e("  Connection error: ${state.message}")
                        return null
                    }
                }

                if (connection == null) {
                    delay(100)
                    attempts++
                }
            }

            if (connection == null) {
                Timber.e("❌ Timeout waiting for connection after ${maxAttempts * 100}ms")
                Timber.e("Final state: ${factory.connectionState.value}")
            }

            connection

        } catch (e: Exception) {
            Timber.e(e, "❌ Exception while creating connection: ${e.message}")
            null
        }
    }
}