package com.smartfoo.android.core.crypto;

import com.smartfoo.android.core.FooException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class FooCrypto
{
    //
    // Names need to be per: http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html
    //
    protected static final String AES                  = "AES";
    protected static final String AES_ECB_PADDING_NONE = AES + "/ECB/NoPadding";
    protected static final String HMAC_SHA256          = "HmacSHA256";
    protected static final String SHA256               = "SHA-256";

    public static class PbCryptoException
            extends FooException
    {
        public PbCryptoException(String source, String message)
        {
            super(source, message);
        }

        public PbCryptoException(String source, Exception innerException)
        {
            super(source, innerException);
        }

        public PbCryptoException(String source, String message, Exception innerException)
        {
            super(source, message, innerException);
        }
    }

    private FooCrypto()
    {
    }

    public static String proprietaryHash(String string)
    {
        byte[] hash;

        try
        {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash)
        {
            int i = (b & 0xFF);
            if (i < 0x10)
            {
                hex.append('0');
            }
            hex.append(Integer.toHexString(i));
        }

        return hex.toString().toUpperCase().substring(8, 24);
    }

    public static byte[] HMACSHA256(byte[] key, byte[] buffer) //
            throws PbCryptoException
    {
        return HMACSHA256(key, buffer, 0, buffer.length);
    }

    public static byte[] HMACSHA256(byte[] key, byte[] buffer, int offset, int length) //
            throws PbCryptoException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA256);
        try
        {
            Mac mac = Mac.getInstance(signingKey.getAlgorithm());
            mac.init(signingKey);
            mac.update(buffer, offset, length);
            return mac.doFinal();
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new PbCryptoException("HMACSHA256(...)", e);
        }
    }

    public static byte[] SHA256(byte[] buffer) //
            throws PbCryptoException
    {
        try
        {
            return MessageDigest.getInstance(SHA256).digest(buffer);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new PbCryptoException("SHA256(...)", e);
        }
    }

    public static int getRandomInt32()
    {
        SecureRandom random = new SecureRandom();
        return random.nextInt();
    }

    public static long getRandomInt64()
    {
        SecureRandom random = new SecureRandom();
        return random.nextLong();
    }

    public static byte[] getRandomBytes(int count)
    {
        byte[] bytes = new byte[count];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }
}
