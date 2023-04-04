package app.cbo.oidc.java.server.credentials;

import java.util.Arrays;

public class Base32 {
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int MASK = 0x1f;
    private static final int SHIFT = 5;
    private static final int[] INDEXES = new int[128];
    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < BASE32_ALPHABET.length(); i++) {
            INDEXES[BASE32_ALPHABET.charAt(i)] = i;
        }
    }

    public static String encode(byte[] data) {
        StringBuilder builder = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < SHIFT) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xff;
                    bitsLeft += 8;
                } else {
                    int pad = SHIFT - bitsLeft;
                    buffer <<= pad;
                    builder.append(BASE32_ALPHABET.charAt(buffer & MASK));
                    buffer = 0;
                    bitsLeft = 0;
                }
            } else {
                bitsLeft -= SHIFT;
                builder.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & MASK));
            }
        }
        return builder.toString();
    }

    public static byte[] decode(String input) {
        byte[] data = input.getBytes();
        int length = (data.length * 5 + 7) / 8;
        byte[] output = new byte[length];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (int i = 0; i < data.length; i++) {
            int index = data[i] & 0x7f;
            int value = INDEXES[index];
            if (value == -1) {
                throw new IllegalArgumentException("Invalid character in input: " + (char) data[i]);
            }
            buffer <<= SHIFT;
            buffer |= value & MASK;
            bitsLeft += SHIFT;
            if (bitsLeft >= 8) {
                output[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return output;
    }
}
