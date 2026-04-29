package org.nahoft.nahoft.services

/**
 * Represents the current state of a WSPR transmit session.
 *
 * States progress linearly for a successful transmission:
 *   Idle → Preparing → WaitingForWindow → TransmittingSpot → (repeat) → Complete
 *
 * Any state can transition to Failed or Cancelled.
 */
sealed class WSPRTransmitSessionState
{
    /** No active session. Frequency is editable. */
    object Idle : WSPRTransmitSessionState()

    /**
     * Encrypting and encoding the message into WSPR symbol arrays.
     * Transitions to [WaitingForWindow] on success, [Failed] on error.
     */
    object Preparing : WSPRTransmitSessionState()

    /**
     * Waiting for the next even UTC minute before transmitting the next spot.
     *
     * @param spotIndex    Zero-based index of the spot about to be transmitted
     * @param totalSpots   Total number of spots required for this message
     * @param msRemaining  Milliseconds until the transmission window opens
     */
    data class WaitingForWindow(
        val spotIndex: Int,
        val totalSpots: Int,
        val msRemaining: Long
    ) : WSPRTransmitSessionState()

    /**
     * Actively transmitting WSPR symbols for one spot.
     *
     * @param spotIndex    Zero-based index of the spot currently transmitting
     * @param totalSpots   Total number of spots required for this message
     * @param symbolIndex  Zero-based index of the symbol currently being sent (0–161)
     */
    data class TransmittingSpot(
        val spotIndex: Int,
        val totalSpots: Int,
        val symbolIndex: Int
    ) : WSPRTransmitSessionState()

    /**
     * Spot transmitted successfully. Switching relay back to RX between spots.
     *
     * @param spotIndex   Zero-based index of the spot just completed
     * @param totalSpots  Total number of spots required for this message
     */
    data class SwitchingToRx(
        val spotIndex: Int,
        val totalSpots: Int
    ) : WSPRTransmitSessionState()

    /**
     * All spots transmitted successfully. Message has been sent.
     *
     * @param totalSpots  Total number of spots that were transmitted
     */
    data class Complete(val totalSpots: Int) : WSPRTransmitSessionState()

    /**
     * Transmission failed. The message was not fully sent.
     *
     * @param reason  Human-readable description of what went wrong,
     *                suitable for display in the UI.
     */
    data class Failed(val reason: String) : WSPRTransmitSessionState()

    /**
     * Transmission was cancelled by the user before completion.
     * Eden has been returned to RX mode.
     */
    object Cancelled : WSPRTransmitSessionState()
}