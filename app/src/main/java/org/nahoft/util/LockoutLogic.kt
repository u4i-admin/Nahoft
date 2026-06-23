package org.nahoft.util

object LockoutLogic
{
    /**
     * Returns the lockout duration in milliseconds for a given number of failed attempts.
     *
     * Lockout schedule:
     * - 0-5 attempts: no lockout
     * - 6 attempts: 1 minute
     * - 7 attempts: 5 minutes
     * - 8 attempts: 15 minutes
     * - 9+ attempts: 1000 minutes (triggers data wipe)
     */
    fun getLockoutDurationMillis(failedLoginAttempts: Int): Long
    {
        val minutes = when {
            failedLoginAttempts >= 9 -> 1000
            failedLoginAttempts == 8 -> 15
            failedLoginAttempts == 7 -> 5
            failedLoginAttempts == 6 -> 1
            else -> 0
        }

        return minutes * 60 * 1000L
    }

    /**
     * @param lockoutDuration Duration of lockout in milliseconds
     * @param lockoutExpiry Wall clock time when lockout expires
     * @param elapsedAtLockout elapsedRealtime() when lockout was set
     * @param currentTimeMillis Current wall clock time
     * @param currentElapsedRealtime Current elapsedRealtime()
     */
    fun isLockoutExpired(
        lockoutDuration: Long,
        lockoutExpiry: Long,
        elapsedAtLockout: Long,
        currentTimeMillis: Long,
        currentElapsedRealtime: Long
    ): Boolean
    {
        if (lockoutDuration == 0L) return true

        // No lockout data stored
        if (lockoutExpiry == 0L) return true

        // Check for reboot: elapsedRealtime resets to 0 on boot
        // If stored value is greater than current, device has rebooted
        val deviceRebooted = elapsedAtLockout > currentElapsedRealtime
        val wallClockExpired = currentTimeMillis >= lockoutExpiry

        // Can't verify with monotonic time, fall back to wall clock
        if (deviceRebooted) return wallClockExpired

        val elapsedSinceLockout = currentElapsedRealtime - elapsedAtLockout
        val realTimeExpired = elapsedSinceLockout >= lockoutDuration

        return wallClockExpired || realTimeExpired
    }

    /**
     * Detects forward clock manipulation.
     *
     * Returns true if the wall clock shows the lockout has expired,
     * but not enough real time has actually passed.
     *
     * @param lockoutDuration Duration of lockout in milliseconds
     * @param lockoutExpiry Wall clock time when lockout expires
     * @param elapsedAtLockout elapsedRealtime() when lockout was set
     * @param currentTimeMillis Current wall clock time
     * @param currentElapsedRealtime Current elapsedRealtime()
     */
    fun isClockManipulationDetected(
        lockoutDuration: Long,
        lockoutExpiry: Long,
        elapsedAtLockout: Long,
        currentTimeMillis: Long,
        currentElapsedRealtime: Long
    ): Boolean
    {
        // No lockout active
        if (lockoutDuration == 0L) return false

        // No lockout data stored
        if (lockoutExpiry == 0L || elapsedAtLockout == 0L) return false

        // Check for reboot - can't reliably detect manipulation after reboot
        if (elapsedAtLockout > currentElapsedRealtime) return false

        val wallClockExpired = currentTimeMillis >= lockoutExpiry
        val elapsedSinceLockout = currentElapsedRealtime - elapsedAtLockout
        val realTimeExpired = elapsedSinceLockout >= lockoutDuration

        // Manipulation: wall clock says expired, but real time disagrees
        return wallClockExpired && !realTimeExpired
    }

    /**
     * Returns the remaining lockout time in milliseconds.
     * Returns the smaller of wall clock or elapsed time remaining.
     *
     * @param lockoutDuration Duration of lockout in milliseconds
     * @param lockoutExpiry Wall clock time when lockout expires
     * @param elapsedAtLockout elapsedRealtime() when lockout was set
     * @param currentTimeMillis Current wall clock time
     * @param currentElapsedRealtime Current elapsedRealtime()
     */
    fun getRemainingLockoutMillis(
        lockoutDuration: Long,
        lockoutExpiry: Long,
        elapsedAtLockout: Long,
        currentTimeMillis: Long,
        currentElapsedRealtime: Long
    ): Long
    {
        if (lockoutDuration == 0L) return 0L
        if (lockoutExpiry == 0L) return 0L

        val wallClockRemaining = maxOf(0L, lockoutExpiry - currentTimeMillis)

        // Check for reboot
        if (elapsedAtLockout > currentElapsedRealtime) return wallClockRemaining

        // Real time remaining
        val elapsedSinceLockout = currentElapsedRealtime - elapsedAtLockout
        val realTimeRemaining = maxOf(0L, lockoutDuration - elapsedSinceLockout)

        return minOf(wallClockRemaining, realTimeRemaining)
    }
}