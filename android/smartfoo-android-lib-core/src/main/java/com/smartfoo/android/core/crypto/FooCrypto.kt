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

    @Throws(FooCryptoException::class)
    @JvmStatic
    fun HMACSHA256(
        key: ByteArray,
        buffer: ByteArray,
    ): ByteArray {
        return HMACSHA256(key, buffer, 0, buffer.size)
    }

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

    @Throws(FooCryptoException::class)
    @JvmStatic
    fun SHA256(buffer: ByteArray): ByteArray {
        try {
            return MessageDigest.getInstance(SHA256).digest(buffer)
        } catch (e: NoSuchAlgorithmException) {
            throw FooCryptoException("SHA256(...)", e)
        }
    }

    @JvmStatic
    val randomInt32: Int
        get() {
            val random = SecureRandom()
            return random.nextInt()
        }

    @JvmStatic
    val randomInt64: Long
        get() {
            val random = SecureRandom()
            return random.nextLong()
        }

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
