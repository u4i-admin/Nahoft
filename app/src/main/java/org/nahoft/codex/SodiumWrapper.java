package org.nahoft.codex;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

// https://doc.libsodium.org/public-key_cryptography/authenticated_encryption
public class SodiumWrapper
{

    public SodiumWrapper(){
        NaCl.sodium(); // required to load the native C library
    }

    public byte[] encrypt(byte[] messageBytes, byte[] nonce, byte[] receiverPublicKey, byte[] senderPrivateKey) throws SecurityException
    {
        // cipherText should be at least crypto_box_MACBYTES + messageBytes.length bytes long
        byte[] cipherText = new byte[Sodium.crypto_box_macbytes() + messageBytes.length];

        //This function writes the authentication tag, whose length is crypto_box_MACBYTES bytes, in cipherText,
        // immediately followed by the encrypted message, whose length is the same as the messageBytes
        int result = Sodium.crypto_box_easy(
                cipherText,
                messageBytes,
                messageBytes.length,
                nonce,
                receiverPublicKey,
                senderPrivateKey);

        if (result != 0 ) {
            throw new SecurityException("Failed to encrypt the message.");
        } else {
            // Return nonce + cipher text
            byte[] fullMessage = new byte[SodiumConstants.NONCE_BYTES + cipherText.length];
            System.arraycopy(nonce, 0, fullMessage, 0, nonce.length);
            System.arraycopy(cipherText, 0, fullMessage, nonce.length, cipherText.length);

            return fullMessage;
        }
    }

    public byte[] decrypt(byte[] encryptedBytes, byte[] senderPublicKey, byte[] receiverPrivateKey) throws SecurityException
    {
        if (encryptedBytes.length <= SodiumConstants.NONCE_BYTES)
        {
            throw new SecurityException("Failed to decrypt the message.");
        }

        // Get the nonce from the encrypted bytes
        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
        System.arraycopy(encryptedBytes, 0, nonce, 0, nonce.length);

        // get the cipher text from the encrypted bytes
        byte[] cipherText = new byte[encryptedBytes.length - nonce.length];
        System.arraycopy(encryptedBytes, nonce.length, cipherText, 0, cipherText.length);

        // container for the decrypt results
        byte[] decryptedMessageBytes = new byte[(int) (cipherText.length - Sodium.crypto_box_macbytes())];

        // The function returns -1 if the verification fails, and 0 on success.
        int result = Sodium.crypto_box_open_easy(
                decryptedMessageBytes,
                cipherText,
                cipherText.length,
                nonce,
                senderPublicKey,
                receiverPrivateKey
        );

        if (result != 0 ) {
            throw new SecurityException("Failed to decrypt the message.");
        }

        return decryptedMessageBytes;
    }
}
