package app.cbo.oidc.java.server.credentials;


/**
 Copyright (c) 2011 IETF Trust and the persons identified as
 authors of the code. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, is permitted pursuant to, and subject to the license
 terms contained in, the Simplified BSD License set forth in Section
 4.c of the IETF Trust's Legal Provisions Relating to IETF Documents
 (http://trustee.ietf.org/license-info).
 */

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;


/**
 * This is an example implementation of the OATH
 * TOTP algorithm.
 * Visit www.openauthentication.org for more information.
 *
 * @author Johan Rydell, PortWise, Inc.
 */

public class TOTP {

    /**
     * Generate a 6 digits OTP from the current time and the given b32 secret key, using HmacSHA1 and a 30s ttl
     *
     * @param b32 secret key
     *
     */
    public static String get(String b32){
        var secret = Base32.decode(b32);
        var asHex = bytesToHex(secret);
        var time = (Instant.now().getEpochSecond())/30;
        return TOTP.generateTOTP(asHex, Long.toHexString(time).toUpperCase(), "6");
    }

    /**
     * Generate 6 digits OTP from the current time and the given b32 secret key, using HmacSHA1 and a 30s ttl
     * This method computes previous and next totp as well, to manage time skew/client input delay
     *
     * @param b32 secret key
     * @param skewAfter number of previous TOTP to compute
     * @param skewBefore number of next TOTP to compute
     * @returns topts (including previous and next ones)
     */
    static List<String> get(String b32, int skewBefore, int skewAfter){
        var secret = Base32.decode(b32);
        var asHex = bytesToHex(secret);

        return IntStream.rangeClosed(-1*skewBefore, skewAfter)
                .boxed()
                .map(i -> {
                    var time = i + ((Instant.now().getEpochSecond()) / 30);
                    return TOTP.generateTOTP(asHex, Long.toHexString(time).toUpperCase(), "6");
                }).toList();
    }


    /**
     * Checks a provided totp against the (stored) secretkey
     * This method will accept the previous, current and next totp to handle time skew/user input delay
     * @param providedTotp as provided by the user
     * @param totpKey as stored on the system
     * @return true if the provided totp matches the last, current or next totp computed from the secret key
     */
    public static boolean confront(String providedTotp, String totpKey) {
        if(totpKey == null){
            return false;
        }
        if(providedTotp == null){
            return false;
        }

        return get(totpKey,1,1)
                .stream()
                .anyMatch(t -> t.equals(providedTotp));

    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);


    static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    private TOTP() {}

    /**
     * This method uses the JCE to provide the crypto algorithm.
     * HMAC computes a Hashed Message Authentication Code with the
     * crypto hash algorithm as a parameter.
     *
     * @param crypto: the crypto algorithm (HmacSHA1, HmacSHA256,
     *                             HmacSHA512)
     * @param keyBytes: the bytes to use for the HMAC key
     * @param text: the message or text to be authenticated
     */



    private static byte[] hmac_sha(String crypto, byte[] keyBytes,
                                   byte[] text){
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey =
                    new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }


    /**
     * This method converts a HEX string to Byte[]
     *
     * @param hex: the HEX string
     *
     * @return: a byte array
     */

    private static byte[] hexStr2Bytes(String hex){
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        byte[] bArray = new BigInteger("10" + hex,16).toByteArray();

        // Copy all the REAL bytes, not the "first"
        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++)
            ret[i] = bArray[i+1];
        return ret;
    }

    private static final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8
            = {1,10,100,1000,10000,100000,1000000,10000000,100000000 };


    /**
     * This method generates a TOTP value for the given
     * set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return a numeric String in base 10
     */

    static String generateTOTP(String key,
                                      String time,
                                      String returnDigits){
        return generateTOTP(key, time, returnDigits, "HmacSHA1");
    }


    /**
     * This method generates a TOTP value for the given
     * set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return a numeric String in base 10
     */

    static String generateTOTP256(String key,
                                         String time,
                                         String returnDigits){
        return generateTOTP(key, time, returnDigits, "HmacSHA256");
    }


    /**
     * This method generates a TOTP value for the given
     * set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return a numeric String in base 10
     */

    static String generateTOTP512(String key,
                                         String time,
                                         String returnDigits){
        return generateTOTP(key, time, returnDigits, "HmacSHA512");
    }


    /**
     * This method generates a TOTP value for the given
     * set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     * @param crypto: the crypto function to use
     *
     * @return a numeric String in base 10
     */

    static String generateTOTP(String key,
                                      String time,
                                      String returnDigits,
                                      String crypto){
        int codeDigits = Integer.decode(returnDigits);
        StringBuilder result;

        // Using the counter
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        StringBuilder timeBuilder = new StringBuilder(time);
        while (timeBuilder.length() < 16 )
            timeBuilder.insert(0, "0");
        time = timeBuilder.toString();

        // Get the HEX in a Byte[]
        byte[] msg = hexStr2Bytes(time);
        byte[] k = hexStr2Bytes(key);




        byte[] hash = hmac_sha(crypto, k, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary =
                ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[codeDigits];

        result = new StringBuilder(Integer.toString(otp));
        while (result.length() < codeDigits) {
            result.insert(0, "0");
        }
        return result.toString();
    }

}
