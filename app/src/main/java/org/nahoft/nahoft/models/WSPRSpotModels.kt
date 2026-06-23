package org.nahoft.nahoft.models

sealed class NahoftSpotStatus
{
    /**
     * Default status for all decoded WSPR spots.
     * Every spot is a potential Nahoft candidate — we have no way to confirm
     * until decryption succeeds.
     */
    object Unconfirmed : NahoftSpotStatus()

    /**
     * Spot confirmed as part of a successfully decrypted Nahoft message.
     * Assigned retroactively when decryption succeeds.
     *
     * @param groupId    Identifies which message this spot belongs to
     * @param partNumber 1-based position within the message
     * @param totalParts Total spots that made up the message
     */
    data class Decrypted(
        val groupId: Int,
        val partNumber: Int,
        val totalParts: Int
    ) : NahoftSpotStatus()
}

/**
 * Position of a spot within a visual group (for rendering connector lines).
 */
enum class GroupPosition
{
    /** Not part of a group (regular WSPR spot) */
    NONE,
    /** First item in a group (top bracket) */
    FIRST,
    /** Middle item in a group (vertical line) */
    MIDDLE,
    /** Last item in a group (bottom bracket) */
    LAST,
    /** Only item in group so far (single bracket, may grow) */
    SINGLE
}

/**
 * A single WSPR spot as displayed in the spots list.
 *
 * Contains both the raw WSPR decode data and Nahoft-specific tracking info.
 */
data class WSPRSpotItem(
    /** Decoded callsign (e.g., "N5HIM" or "Q0ABC" for Nahoft) */
    val callsign: String,

    /** Maidenhead grid square (e.g., "EM89") */
    val gridSquare: String,

    /** Transmitted power level in dBm */
    val powerDbm: Int,

    /** Signal-to-noise ratio in dB */
    val snrDb: Float,

    /** Timestamp when this spot was decoded (System.currentTimeMillis()) */
    val timestamp: Long,

    /** Processing status for this spot */
    val nahoftStatus: NahoftSpotStatus,

    /**
     * Visual grouping position, computed when building the display list.
     * Determines which connector lines/brackets to draw.
     */
    val groupPosition: GroupPosition = GroupPosition.NONE
)
{
    val statusDisplay: String?
        get() = when (val status = nahoftStatus)
        {
            is NahoftSpotStatus.Unconfirmed -> null
            is NahoftSpotStatus.Decrypted   ->
                "✓ Part ${status.partNumber} of ${status.totalParts}"
        }
}