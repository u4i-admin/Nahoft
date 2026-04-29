package org.nahoft.nahoft.services

/**
 * Represents the current state of an MFSK receive session.
 *
 * States for a normal session:
 *   Idle → Starting → Running → Stopped
 *
 * Any state can transition to Failed (including session timeout).
 *
 * MFSK receive is a continuous streaming decode — there are no
 * fixed timing windows and no spot accumulation phase. Received messages are
 * delivered via [MFSKReceiveSessionService.lastReceivedMessage] as they arrive,
 * independently of this state flow.
 */
sealed class MFSKReceiveSessionState
{
    /** No active session. */
    object Idle : MFSKReceiveSessionState()

    /**
     * Session is initializing: connecting to USB audio and starting [MFSKStation].
     * Transitions to [Running] on success, [Failed] on error.
     */
    object Starting : MFSKReceiveSessionState()

    /**
     * [MFSKStation] is running and actively decoding the audio stream.
     * Remains in this state until explicitly stopped or a failure occurs.
     * Received messages are emitted on [MFSKReceiveSessionService.lastReceivedMessage].
     */
    object Running : MFSKReceiveSessionState()

    /** Session stopped normally (user-initiated). */
    object Stopped : MFSKReceiveSessionState()

    /**
     * Session ended due to an error or timeout.
     *
     * Timeout emits: Failed("Session timed out")
     *
     * @param reason Human-readable description of what went wrong,
     *               suitable for display in the UI.
     */
    data class Failed(val reason: String) : MFSKReceiveSessionState()
}