package org.nahoft.nahoft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.operatorfoundation.audiocoder.wspr.WSPRConstants
import org.operatorfoundation.ion.storage.NounType
import org.operatorfoundation.ion.storage.Word
import org.operatorfoundation.transmission.SerialConnection
import timber.log.Timber

/**
 * Abstraction layer for the Eden radio hardware.
 *
 * Eden has two radio chips sharing a single antenna via a relay:
 *   - SI5351 synthesizer for TX (CLK0 output)
 *   - SI4735 receiver for RX
 *
 * Communication is over USB serial using the ion binary protocol.
 * Commands are sent as ion INTEGER Words; Eden responds with ASCII
 * acknowledgement strings (e.g. "OK TX FREQ\r\n").
 *
 * Frequencies are expressed in centihertz (Hz × 100) internally to match
 * the SI5351 library's expected format. User-facing input is in kHz.
 *
 * Lifecycle: create when the serial connection is established, discard when
 * it is lost. The ViewModel owns the Eden instance.
 */
class Eden(private val connection: SerialConnection)
{
    // ==================== Public Types ====================

    /** Current antenna relay state. Matches EdenMode in eden.cpp. */
    enum class Mode { RX, TX }

    // ==================== Constants ====================

    companion object
    {
        /**
         * Integer control codes recognized by the Eden firmware.
         * Any integer not in this set is interpreted as a frequency in centihertz.
         */
        private const val CONTROL_OFF = 0   // Turn SI5351 CLK0 off (TX only)
        private const val CONTROL_ON  = 1   // Turn SI5351 CLK0 on  (TX only)
        private const val CONTROL_TX  = 2   // Switch relay to TX chip, mute USB audio
        private const val CONTROL_RX  = 3   // Switch relay to RX chip, unmute USB audio

        const val MAX_RADIO_MESSAGE_BYTES = 130

        /** Millis to wait for Eden to process a command and respond. */
        private const val ACK_TIMEOUT_MS = 3000L

        /** Millis of silence after last byte before treating a response as complete. */
        private const val RESPONSE_IDLE_MS = 200L

        /** Poll interval while waiting for a response. */
        private const val POLL_INTERVAL_MS = 50L
    }

    // ==================== State ====================

    /** Last mode we sent to Eden. Null = unknown (Eden has just powered on). */
    var currentMode: Mode? = null
        private set

    // ==================== Public API ====================

    /**
     * Switches the antenna relay to the SI4735 receiver and sets the RX frequency.
     *
     * Call this before starting a WSPR receive session. You do not need to turn
     * the receiver on or off — RX mode is active whenever the relay is in RX
     * position and the USB audio connection is open.
     *
     * A short-lived reader coroutine logs all firmware responses without blocking.
     *
     * @param frequencyKHz Dial frequency in kilohertz (e.g. 14095 for 20m WSPR)
     * @return True if all commands were written successfully
     */
    suspend fun startReceiving(frequencyKHz: Int): Boolean = withContext(Dispatchers.IO)
    {
        Timber.d("Eden: startReceiving at $frequencyKHz kHz")

        // Short-lived reader coroutine — logs all firmware responses
        // without blocking the command sequence
        val readerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive)
            {
                try
                {
                    val bytes = connection.readAvailable()

                    if (bytes != null && bytes.isNotEmpty())
                    {
                        val responses = bytes.decodeToString()
                            .split("\r\n", "\n", "\r")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        for (response in responses)
                        {
                            Timber.i("[Received Eden]: $response")
                        }
                    }
                }
                catch (e: Exception)
                {
                    Timber.w(e, "Eden: error reading serial buffer during startReceiving")
                }
            }
        }

        return@withContext try
        {
            // Switch relay to RX (also the firmware default on startup)
            Timber.d("Eden.kt: sending CONTROL_RX")
            Word.to_conn(connection, Word.make(CONTROL_RX, NounType.INTEGER.value))
            currentMode = Mode.RX

            // Set RX frequency
            val frequencyCHz = frequencyKHz.toCentihertz()
            Timber.d("Eden.kt: sending RX frequency $frequencyCHz cHz")
            Word.to_conn(connection, Word.make(frequencyCHz.toInt(), NounType.INTEGER.value))

            Timber.i("Eden: RX mode active at $frequencyKHz kHz")
            true
        }
        catch (e: Exception)
        {
            Timber.e(e, "Eden: failed to start receiving at $frequencyKHz kHz")
            false
        }
        finally
        {
            // Give firmware a brief moment to process before stopping the reader
            delay(500)
            readerJob.cancel()
        }
    }

    /**
     * Transmits a WSPR message as a sequence of FSK symbols.
     *
     * TX sequence:
     *   1. Switch relay to TX
     *   2. Set initial frequency and enable SI5351 output
     *   3. Step through all 162 symbol frequencies
     *   4. Disable SI5351 output and switch relay back to RX
     *
     * A single reader coroutine owns all serial reads for the entire
     * transmission, logging all firmware responses.
     *
     * Symbol timing is paced by WSPRConstants.SYMBOL_DURATION_MS (683ms)
     * on the Kotlin side — the firmware does not enforce inter-symbol delay.
     * Suspends for the ~110 second WSPR transmission duration.
     *
     * @param symbolFrequenciesCHz 162-element LongArray from WSPREncoder.encodeToFrequencies()
     * @param onSymbolSent Optional progress callback, called with (symbolIndex, total)
     * @return True if all symbols were sent successfully
     */
    suspend fun transmitWSPR(symbolFrequenciesCHz: LongArray, onSymbolSent: ((Int, Int) -> Unit)? = null): Boolean = withContext(Dispatchers.IO)
    {
        require(symbolFrequenciesCHz.size == 162)
        {
            "WSPR requires exactly 162 symbols, got ${symbolFrequenciesCHz.size}"
        }

        Timber.d("Eden: beginning WSPR transmission (${symbolFrequenciesCHz.size} symbols)")
        return@withContext transmitSymbols(symbolFrequenciesCHz, WSPRConstants.SYMBOL_DURATION_MS, onSymbolSent)
    }

    /**
     * Transmits an MFSK message as a sequence of FSK symbols.
     *
     * Symbol frequencies should be derived as:
     *   `(baseFrequencyHz + symbolIndex * toneSpacingHz) * 100` (centihertz)
     *
     * @param symbolFrequenciesCHz Frequencies in centihertz, one per symbol.
     * @param symbolDurationMs     Duration of each symbol in milliseconds.
     *                             Derive from [org.operatorfoundation.audiocoder.mfsk.MFSKMode.symbolDurationSeconds].
     * @param onSymbolSent         Optional progress callback, called with (symbolIndex, total).
     * @return True if all symbols were sent successfully.
     */
    suspend fun transmitMFSK(
        symbolFrequenciesCHz: LongArray,
        symbolDurationMs: Long,
        onSymbolSent: ((Int, Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO)
    {
        require(symbolFrequenciesCHz.isNotEmpty())
        {
            "symbolFrequenciesCHz must not be empty"
        }

        Timber.d("Eden: beginning MFSK transmission (${symbolFrequenciesCHz.size} symbols)")
        return@withContext transmitSymbols(symbolFrequenciesCHz, symbolDurationMs, onSymbolSent)
    }

    private suspend fun transmitSymbols(
        symbolFrequenciesCHz: LongArray,
        symbolDurationMs: Long,
        onSymbolSent: ((Int, Int) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO)
    {
        Timber.d("Eden: beginning transmission (${symbolFrequenciesCHz.size} symbols at ${symbolDurationMs}ms each)")

        val readerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive)
            {
                try
                {
                    val bytes = connection.readAvailable()
                    if (bytes != null && bytes.isNotEmpty())
                    {
                        bytes.decodeToString()
                            .split("\r\n", "\n", "\r")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { Timber.i("[Received Eden]: $it") }
                    }
                }
                catch (e: Exception)
                {
                    Timber.w(e, "Eden: error reading serial buffer during transmission")
                }
            }
        }

        try
        {
            Word.to_conn(connection, Word.make(CONTROL_TX, NounType.INTEGER.value))
            currentMode = Mode.TX

            // Anchor the timing reference before the first frequency is sent.
            val transmissionStartMs = System.currentTimeMillis()

            sendFrequency(symbolFrequenciesCHz[0])
            Word.to_conn(connection, Word.make(CONTROL_ON, NounType.INTEGER.value))

            for (index in 0 until symbolFrequenciesCHz.size)
            {
                // Compute remaining time until the target end of this symbol.
                // Using an absolute target corrects for accumulated overshoot —
                // if a previous iteration ran long, the next delay is shorter to compensate.
                val targetMs = transmissionStartMs + (index + 1).toLong() * symbolDurationMs
                val remainingMs = targetMs - System.currentTimeMillis()
                if (remainingMs > 0) delay(remainingMs)

                onSymbolSent?.invoke(index, symbolFrequenciesCHz.size)

                if (index + 1 < symbolFrequenciesCHz.size)
                {
                    sendFrequency(symbolFrequenciesCHz[index + 1])
                }
            }

            Word.to_conn(connection, Word.make(CONTROL_OFF, NounType.INTEGER.value))
            Word.to_conn(connection, Word.make(CONTROL_RX, NounType.INTEGER.value))
            currentMode = Mode.RX

            Timber.i("Eden: transmission complete")
            true
        }
        finally
        {
            readerJob.cancel()

            if (currentMode == Mode.TX)
            {
                Timber.w("Eden: transmission interrupted — attempting return to RX mode")
                try
                {
                    Word.to_conn(connection, Word.make(CONTROL_OFF, NounType.INTEGER.value))
                    Word.to_conn(connection, Word.make(CONTROL_RX, NounType.INTEGER.value))
                    currentMode = Mode.RX
                }
                catch (e: Exception)
                {
                    Timber.e(e, "Eden: failed to return to RX mode after interrupted transmission")
                }
            }
        }
    }

    /**
     * Switches the antenna relay to RX without setting a frequency.
     *
     * Use this as a safety net after transmitting to guarantee we are not
     * stuck in TX mode. Does not touch the receiver frequency — that is
     * the responsibility of [startReceiving].
     */
    suspend fun switchToRX(): Boolean = withContext(Dispatchers.IO)
    {
        try
        {
            Timber.d("Eden.kt: switchToRX — sending CONTROL_RX")
            Word.to_conn(connection, Word.make(CONTROL_RX, NounType.INTEGER.value))
            currentMode = Mode.RX
            true
        }
        catch (e: Exception)
        {
            Timber.e(e, "Eden.kt: failed to switch to RX mode")
            false
        }
    }

    /**
     * Closes the serial connection. Call when the ViewModel is cleared or the
     * USB device is detached.
     */
    fun close()
    {
        try
        {
            connection.close()
        }
        catch (e: Exception)
        {
            Timber.w(e, "Eden.kt: error closing connection")
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Sends a frequency value in centihertz. Fire-and-forget — does not wait
     * for acknowledgement.
     *
     * FIXME: Word.make() takes Int. Frequencies above ~21 MHz expressed in cHz
     * will overflow.
     */
    private suspend fun sendFrequency(frequencyCHz: Long) = withContext(Dispatchers.IO)
    {
        Timber.d("Eden.kt: sending frequency ${frequencyCHz} cHz")
        try
        {
            Word.to_conn(connection, Word.make(frequencyCHz.toInt(), NounType.INTEGER.value))
        }
        catch (e: Exception)
        {
            Timber.e(e, "Eden.kt: failed to send frequency ${frequencyCHz} cHz")
        }
    }

    // ==================== Extensions ====================

    /**
     * Converts a user-facing kilohertz value to the centihertz value Eden expects.
     *
     * Example: 14095 kHz → 1,409,500,000 cHz
     *
     * WSPREncoder adds a 1500 Hz audio center offset on top of this, so the
     * effective RF frequency is (frequencyKHz * 1000 + 1500) Hz, matching the
     * WSPR convention of dial frequency + 1500 Hz audio tone.
     */
    private fun Int.toCentihertz(): Long = this * 100_000L
}