package org.nahoft.nahoft.services

/**
 * Represents the current state of an MFSK transmit session.
 *
 * States for a successful transmission:
 *   Idle → Preparing → Transmitting → Complete
 *
 * Any state can transition to Failed or Cancelled.
 *
 * Note: MFSK transmission is encrypted only. Unencrypted MFSK TX is not
 * currently supported.
 */
sealed class MFSKTransmitSessionState
{
    /** No active session. Frequency and settings are editable. */
    object Idle : MFSKTransmitSessionState()

    /**
     * Encrypting and encoding the message into MFSK symbols.
     * Transitions to [Transmitting] on success, [Failed] on error.
     */
    object Preparing : MFSKTransmitSessionState()

    /**
     * Actively transmitting MFSK symbols via Eden.
     *
     * No per-symbol callbacks are issued — the fragment drives a deterministic
     * progress animation using [totalDurationMs] and elapsed wall-clock time.
     *
     * @param totalDurationMs Expected transmission duration in milliseconds.
     *                        Derived from: symbolCount × mode.symbolDurationSeconds × 1000.
     */
    data class Transmitting(val totalDurationMs: Long) : MFSKTransmitSessionState()

    /**
     * All symbols transmitted successfully. Message has been sent and saved.
     */
    object Complete : MFSKTransmitSessionState()

    /**
     * Transmission failed. The message was not fully sent.
     *
     * @param reason Human-readable description of what went wrong,
     *               suitable for display in the UI.
     */
    data class Failed(val reason: String) : MFSKTransmitSessionState()

    /**
     * Transmission was cancelled by the user before completion.
     * Eden has been returned to RX mode.
     */
    object Cancelled : MFSKTransmitSessionState()
}