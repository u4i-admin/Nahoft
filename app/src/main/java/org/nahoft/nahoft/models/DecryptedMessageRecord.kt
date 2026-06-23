package org.nahoft.nahoft.models

/**
 * Metadata record created each time a Nahoft message is successfully
 * decrypted during a receive session.
 *
 * Intentionally contains NO plaintext, NO ciphertext, and NO keys.
 * Used only to drive the session message counter and populate the
 * ReceivedMessagesDialogFragment row headers. Decryption happens
 * on-demand in the dialog, scoped to that call only.
 *
 * @property timestamp System.currentTimeMillis() at the moment of decryption.
 *                     Used to locate the corresponding saved Message by timestamp proximity.
 * @property spotCount Number of WSPR spots that were used to decode this message.
 */
data class DecryptedMessageRecord(
    val timestamp: Long,
    val spotCount: Int
)