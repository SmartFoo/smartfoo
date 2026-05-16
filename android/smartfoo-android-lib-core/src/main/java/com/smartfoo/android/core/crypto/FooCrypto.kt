package com.smartfoo.android.core.crypto

import com.smartfoo.android.core.FooException
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Suppress("FunctionName", "unused")
object FooCrypto {
    /**
     * Needs to match ...
     */
    const val AES = "AES"

    /**
     * Needs to match https://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#impl
     */
    val AES_ECB_PADDING_NONE = "$AES/ECB/NoPadding"

    /**
     * Needs to match https://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#KeyGenerator
     */
    const val HMACSHA256 = "HmacSHA256"

    /**
     * Need to match https://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
     */
    const val SHA256 = "SHA-256"

    /**
     * Computes a proprietary 16-character uppercase hex token derived from an MD5 digest of
     * [string].
     *
     * The token is extracted as characters 8–23 (inclusive) of the 32-character hex digest.
     * This is intentionally non-standard and should not be used as a general-purpose hash.
     *
     * @param string the input string to hash; must be UTF-8 encodable
     * @return a 16-character uppercase hex string
     * @throws RuntimeException if MD5 or UTF-8 are unexpectedly unavailable on the platform
     */
    @JvmStatic
    fun proprietaryHash(string: String): String {
        val hash: ByteArray?

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.toByteArray(charset("UTF-8")))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Huh, MD5 should be supported?", e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Huh, UTF-8 should be supported?", e)
        }

        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            val i = (b.toInt() and 0xFF)
            if (i < 0x10) {
                hex.append('0')
            }
            hex.append(Integer.toHexString(i))
        }

        return hex.toString().uppercase(Locale.getDefault()).substring(8, 24)
    }

    /**
     * Computes an HMAC-SHA-256 message authentication code over the entire [buffer].
     *
     * @param key    the secret key bytes
     * @param buffer the data to authenticate
     * @return the 32-byte HMAC-SHA-256 result
     * @throws FooCryptoException if the HMAC-SHA-256 algorithm or key is unavailable
     */
    @Throws(FooCryptoException::class)
    @JvmStatic
    fun HMACSHA256(
        key: ByteArray,
        buffer: ByteArray,
    ): ByteArray {
        return HMACSHA256(key, buffer, 0, buffer.size)
    }

    /**
     * Computes an HMAC-SHA-256 message authentication code over a slice of [buffer].
     *
     * @param key    the secret key bytes
     * @param buffer the data buffer
     * @param offset the starting index within [buffer]
     * @param length the number of bytes to include starting at [offset]
     * @return the 32-byte HMAC-SHA-256 result
     * @throws FooCryptoException if the HMAC-SHA-256 algorithm or key is unavailable
     */
    @Throws(FooCryptoException::class)
    @JvmStatic
    fun HMACSHA256(
        key: ByteArray,
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): ByteArray {
        val signingKey = SecretKeySpec(key, HMACSHA256)
        try {
            val mac = Mac.getInstance(signingKey.algorithm)
            mac.init(signingKey)
            mac.update(buffer, offset, length)
            return mac.doFinal()
        } catch (e: NoSuchAlgorithmException) {
            throw FooCryptoException("HMACSHA256(...)", e)
        } catch (e: InvalidKeyException) {
            throw FooCryptoException("HMACSHA256(...)", e)
        }
    }

    /**
     * Computes a SHA-256 digest of [buffer].
     *
     * @param buffer the data to hash
     * @return the 32-byte SHA-256 digest
     * @throws FooCryptoException if the SHA-256 algorithm is unavailable on this platform
     */
    @Throws(FooCryptoException::class)
    @JvmStatic
    fun SHA256(buffer: ByteArray): ByteArray {
        try {
            return MessageDigest.getInstance(SHA256).digest(buffer)
        } catch (e: NoSuchAlgorithmException) {
            throw FooCryptoException("SHA256(...)", e)
        }
    }

    /**
     * Returns a cryptographically secure random 32-bit integer.
     *
     * @return a random [Int] value from [SecureRandom]
     */
    @JvmStatic
    val randomInt32: Int
        get() {
            val random = SecureRandom()
            return random.nextInt()
        }

    /**
     * Returns a cryptographically secure random 64-bit integer.
     *
     * @return a random [Long] value from [SecureRandom]
     */
    @JvmStatic
    val randomInt64: Long
        get() {
            val random = SecureRandom()
            return random.nextLong()
        }

    /**
     * Returns a byte array of the specified length filled with cryptographically secure random
     * bytes.
     *
     * @param count the number of random bytes to generate; must be >= 0
     * @return a new [ByteArray] of length [count]
     */
    @JvmStatic
    fun getRandomBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        val random = SecureRandom()
        random.nextBytes(bytes)
        return bytes
    }

    class FooCryptoException : FooException {
        constructor(source: String?, message: String?) : super(source, message)

        constructor(source: String?, innerException: Exception?) : super(source, innerException)

        constructor(source: String?, message: String?, innerException: Exception?) : super(
            source,
            message,
            innerException
        )
    }
}
