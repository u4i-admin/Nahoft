package org.nahoft

import org.junit.Assert.*
import org.junit.Test
import org.nahoft.util.LockoutLogic

class LockoutLogicTest
{

    // Constants for readability
    private val ONE_MINUTE_MS = 60 * 1000L
    private val FIVE_MINUTES_MS = 5 * 60 * 1000L
    private val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L

    // ========================================================================
    // getLockoutDurationMillis tests
    // ========================================================================

    @Test
    fun getLockoutDuration_noLockoutForFiveOrFewerAttempts() {
        for (attempts in 0..5) {
            assertEquals(
                "Expected no lockout for $attempts attempts",
                0L,
                LockoutLogic.getLockoutDurationMillis(attempts)
            )
        }
    }

    @Test
    fun getLockoutDuration_oneMinuteForSixAttempts() {
        assertEquals(ONE_MINUTE_MS, LockoutLogic.getLockoutDurationMillis(6))
    }

    @Test
    fun getLockoutDuration_fiveMinutesForSevenAttempts() {
        assertEquals(FIVE_MINUTES_MS, LockoutLogic.getLockoutDurationMillis(7))
    }

    @Test
    fun getLockoutDuration_fifteenMinutesForEightAttempts() {
        assertEquals(FIFTEEN_MINUTES_MS, LockoutLogic.getLockoutDurationMillis(8))
    }

    @Test
    fun getLockoutDuration_thousandMinutesForNineOrMoreAttempts() {
        val thousandMinutes = 1000 * 60 * 1000L
        assertEquals(thousandMinutes, LockoutLogic.getLockoutDurationMillis(9))
        assertEquals(thousandMinutes, LockoutLogic.getLockoutDurationMillis(10))
        assertEquals(thousandMinutes, LockoutLogic.getLockoutDurationMillis(100))
    }

    // ========================================================================
    // isLockoutExpiredLogic tests
    // ========================================================================

    @Test
    fun isLockoutExpired_trueWhenNoLockoutDuration() {
        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = 0L,
            lockoutExpiry = 1000L,
            elapsedAtLockout = 500L,
            currentTimeMillis = 500L,
            currentElapsedRealtime = 600L
        )
        assertTrue("Should be expired when lockout duration is 0", result)
    }

    @Test
    fun isLockoutExpired_trueWhenNoExpiryStored() {
        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = 0L,
            elapsedAtLockout = 500L,
            currentTimeMillis = 500L,
            currentElapsedRealtime = 600L
        )
        assertTrue("Should be expired when no expiry stored", result)
    }

    @Test
    fun isLockoutExpired_trueWhenWallClockPastExpiry() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry + 1, // Wall clock past expiry
            currentElapsedRealtime = 100L + ONE_MINUTE_MS + 1 // Real time also past
        )
        assertTrue("Should be expired when wall clock is past expiry", result)
    }

    @Test
    fun isLockoutExpired_trueWhenRealTimePastDuration() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry - 1000, // Wall clock NOT past expiry
            currentElapsedRealtime = 100L + ONE_MINUTE_MS + 1 // But real time IS past
        )
        assertTrue("Should be expired when real time exceeds duration", result)
    }

    @Test
    fun isLockoutExpired_falseWhenBothMeasuresShowActive() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry - 30000, // 30 seconds before expiry
            currentElapsedRealtime = 100L + 30000 // Only 30 seconds elapsed
        )
        assertFalse("Should NOT be expired when both measures show active", result)
    }

    @Test
    fun isLockoutExpired_fallsBackToWallClockOnReboot() {
        val lockoutExpiry = 1000L

        // Simulate reboot: currentElapsedRealtime < elapsedAtLockout
        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 50000L, // Was set when elapsed was 50000
            currentTimeMillis = lockoutExpiry + 1, // Wall clock past expiry
            currentElapsedRealtime = 1000L // After reboot, elapsed is only 1000
        )
        assertTrue("Should fall back to wall clock after reboot", result)
    }

    @Test
    fun isLockoutExpired_respectsWallClockOnRebootWhenNotExpired() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isLockoutExpired(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 50000L,
            currentTimeMillis = lockoutExpiry - 1, // Wall clock NOT past expiry
            currentElapsedRealtime = 1000L // Rebooted
        )
        assertFalse("Should respect wall clock on reboot when not expired", result)
    }

    // ========================================================================
    // isClockManipulationDetectedLogic tests
    // ========================================================================

    @Test
    fun isClockManipulationDetected_falseWhenNoLockout() {
        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = 0L,
            lockoutExpiry = 1000L,
            elapsedAtLockout = 100L,
            currentTimeMillis = 2000L,
            currentElapsedRealtime = 150L
        )
        assertFalse("No manipulation when no lockout active", result)
    }

    @Test
    fun isClockManipulationDetected_falseWhenNoExpiryStored() {
        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = 0L,
            elapsedAtLockout = 100L,
            currentTimeMillis = 2000L,
            currentElapsedRealtime = 150L
        )
        assertFalse("No manipulation when no expiry stored", result)
    }

    @Test
    fun isClockManipulationDetected_trueWhenWallClockExpiredButRealTimeNot() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry + 1000, // Wall clock past expiry
            currentElapsedRealtime = 100L + 10000 // Only 10 seconds real time (not enough)
        )
        assertTrue("Should detect manipulation: wall expired but real time hasn't", result)
    }

    @Test
    fun isClockManipulationDetected_falseWhenBothExpired() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry + 1000,
            currentElapsedRealtime = 100L + ONE_MINUTE_MS + 1000 // Real time also expired
        )
        assertFalse("No manipulation when both measures show expired", result)
    }

    @Test
    fun isClockManipulationDetected_falseWhenNeitherExpired() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry - 30000, // Wall clock not expired
            currentElapsedRealtime = 100L + 30000 // Real time not expired
        )
        assertFalse("No manipulation when neither measure shows expired", result)
    }

    @Test
    fun isClockManipulationDetected_falseAfterReboot() {
        val lockoutExpiry = 1000L

        // After reboot, we can't reliably detect manipulation
        val result = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 50000L,
            currentTimeMillis = lockoutExpiry + 1000, // Would look like manipulation
            currentElapsedRealtime = 1000L // But device rebooted
        )
        assertFalse("Should not detect manipulation after reboot", result)
    }

    // ========================================================================
    // getRemainingLockoutMillisLogic tests
    // ========================================================================

    @Test
    fun getRemainingLockoutMillis_zeroWhenNoLockoutDuration() {
        val result = LockoutLogic.getRemainingLockoutMillis(
            lockoutDuration = 0L,
            lockoutExpiry = 1000L,
            elapsedAtLockout = 100L,
            currentTimeMillis = 500L,
            currentElapsedRealtime = 150L
        )
        assertEquals(0L, result)
    }

    @Test
    fun getRemainingLockoutMillis_zeroWhenNoExpiryStored() {
        val result = LockoutLogic.getRemainingLockoutMillis(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = 0L,
            elapsedAtLockout = 100L,
            currentTimeMillis = 500L,
            currentElapsedRealtime = 150L
        )
        assertEquals(0L, result)
    }

    @Test
    fun getRemainingLockoutMillis_returnsWallClockRemainingAfterReboot() {
        val lockoutExpiry = 100000L
        val currentTime = 70000L
        val expectedRemaining = lockoutExpiry - currentTime // 30000ms

        val result = LockoutLogic.getRemainingLockoutMillis(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 50000L, // Higher than current elapsed = reboot
            currentTimeMillis = currentTime,
            currentElapsedRealtime = 1000L
        )
        assertEquals(expectedRemaining, result)
    }

    @Test
    fun getRemainingLockoutMillis_returnsSmallerOfTwoMeasures() {
        val lockoutExpiry = 100000L
        val elapsedAtLockout = 1000L
        val currentTime = 70000L
        val currentElapsed = 31000L // 30 seconds elapsed

        val wallClockRemaining = lockoutExpiry - currentTime // 30000ms
        val realTimeRemaining = ONE_MINUTE_MS - (currentElapsed - elapsedAtLockout) // 30000ms

        val result = LockoutLogic.getRemainingLockoutMillis(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = elapsedAtLockout,
            currentTimeMillis = currentTime,
            currentElapsedRealtime = currentElapsed
        )

        assertEquals(minOf(wallClockRemaining, realTimeRemaining), result)
    }

    @Test
    fun getRemainingLockoutMillis_neverReturnsNegative() {
        val lockoutExpiry = 1000L

        val result = LockoutLogic.getRemainingLockoutMillis(
            lockoutDuration = ONE_MINUTE_MS,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = 100L,
            currentTimeMillis = lockoutExpiry + 100000, // Way past expiry
            currentElapsedRealtime = 100L + ONE_MINUTE_MS + 100000
        )
        assertEquals("Should never return negative", 0L, result)
    }

    // ========================================================================
    // Scenario tests - simulating the original attack
    // ========================================================================

    @Test
    fun scenario_spamAttackDoesNotExtendLockout() {
        // User fails login 6 times, gets 1 minute lockout
        val lockoutDuration = ONE_MINUTE_MS
        val lockoutExpiry = 1000000L // Set at wall clock 1000000 - 60000 = 940000
        val elapsedAtLockout = 5000L // Set when elapsedRealtime was 5000

        // Attacker spams TimeChangeReceiver every 10 seconds
        // Each spam: check if manipulation detected
        // Simulate 10 spam attempts over ~100 seconds

        for (spamAttempt in 1..10) {
            val elapsedNow = elapsedAtLockout + (spamAttempt * 10000L) // 10 sec intervals
            val wallClockNow = 940000L + (spamAttempt * 10000L)

            val manipulationDetected = LockoutLogic.isClockManipulationDetected(
                lockoutDuration = lockoutDuration,
                lockoutExpiry = lockoutExpiry,
                elapsedAtLockout = elapsedAtLockout,
                currentTimeMillis = wallClockNow,
                currentElapsedRealtime = elapsedNow
            )

            assertFalse(
                "Spam attempt $spamAttempt should NOT detect manipulation",
                manipulationDetected
            )
        }

        // After 70 seconds, lockout should be expired
        val finalElapsed = elapsedAtLockout + 70000L
        val finalWallClock = 940000L + 70000L

        val isExpired = LockoutLogic.isLockoutExpired(
            lockoutDuration = lockoutDuration,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = elapsedAtLockout,
            currentTimeMillis = finalWallClock,
            currentElapsedRealtime = finalElapsed
        )

        assertTrue("Lockout should expire normally despite spam", isExpired)
    }

    @Test
    fun scenario_forwardClockManipulationDetected() {
        // User fails login 6 times, gets 1 minute lockout
        val lockoutDuration = ONE_MINUTE_MS
        val lockoutSetTime = 1000000L
        val lockoutExpiry = lockoutSetTime + lockoutDuration
        val elapsedAtLockout = 5000L

        // User waits 10 seconds, then moves clock forward 2 minutes
        val realTimeElapsed = 10000L // Only 10 seconds real time
        val wallClockNow = lockoutExpiry + 60000L // 1 minute past expiry

        val manipulationDetected = LockoutLogic.isClockManipulationDetected(
            lockoutDuration = lockoutDuration,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = elapsedAtLockout,
            currentTimeMillis = wallClockNow,
            currentElapsedRealtime = elapsedAtLockout + realTimeElapsed
        )

        assertTrue("Should detect forward clock manipulation", manipulationDetected)
    }

    @Test
    fun scenario_legitimateTimezoneChangeAllowsLogin() {
        // User fails login 6 times, gets 1 minute lockout
        val lockoutDuration = ONE_MINUTE_MS
        val lockoutSetTime = 1000000L
        val lockoutExpiry = lockoutSetTime + lockoutDuration
        val elapsedAtLockout = 5000L

        // User travels west, clock moves back 2 hours
        // But they wait the full lockout duration in real time
        val realTimeElapsed = lockoutDuration + 1000L // Waited full duration + 1 sec
        val wallClockNow = lockoutSetTime - (2 * 60 * 60 * 1000) + realTimeElapsed // 2 hours back

        val isExpired = LockoutLogic.isLockoutExpired(
            lockoutDuration = lockoutDuration,
            lockoutExpiry = lockoutExpiry,
            elapsedAtLockout = elapsedAtLockout,
            currentTimeMillis = wallClockNow,
            currentElapsedRealtime = elapsedAtLockout + realTimeElapsed
        )

        assertTrue("Should allow login after real time passes despite timezone change", isExpired)
    }
}