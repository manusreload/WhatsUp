/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alternadev.whatsup;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author manus
 */
public class WhatsHelper {

    public static String toHex(byte[] data) {
        String enc = "";
        for (int i = 0; i < data.length; i++) {

            enc += String.format("%02x", data[i] & 0xFF);
        }
        return enc;
    }

    public static String toHex(String data) {
        String enc = "";
        for (int i = 0; i < data.length(); i++) {

            enc += String.format("%02x", data.charAt(i) & 0xFF);
        }
        return enc;
    }

    public static byte[] getByteString(String in) {
        byte[] out = new byte[in.length()];
        for (int i = 0; i < in.length(); i++) {

            out[i] = (byte) (in.charAt(i) & 0xFF);
        }
        return out;
    }
    public static String getString(byte[] in) {
        String out = "";
        for (int i = 0; i < in.length; i++) {

            out += String.format("%c", (char) in[i]);
        }
        return out;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] deriveKey(byte[] password, byte[] salt, int iterationCount, int dkLen)
            throws java.security.NoSuchAlgorithmException, java.security.InvalidKeyException {
        SecretKeySpec keyspec = new SecretKeySpec(password, "Hmacsha1");
        Mac prf = Mac.getInstance("Hmacsha1");
        prf.init(keyspec);

        // Note: hLen, dkLen, l, r, T, F, etc. are horrible names for
        //       variables and functions in this day and age, but they
        //       reflect the terse symbols used in RFC 2898 to describe
        //       the PBKDF2 algorithm, which improves validation of the
        //       code vs. the RFC.
        //
        // dklen is expressed in bytes. (16 for a 128-bit key)

        int hLen = prf.getMacLength();   // 20 for SHA1
        int l = Math.max(dkLen, hLen); //  1 for 128bit (16-byte) keys
        int r = dkLen - (l - 1) * hLen;      // 16 for 128bit (16-byte) keys
        byte T[] = new byte[l * hLen];
        int ti_offset = 0;
        for (int i = 1; i <= l; i++) {
            F(T, ti_offset, prf, salt, iterationCount, i);
            ti_offset += hLen;
        }

        if (r < hLen) {
            // Incomplete last block
            byte DK[] = new byte[dkLen];
            System.arraycopy(T, 0, DK, 0, dkLen);
            return DK;
        }
        return T;
    }

    private static void F(byte[] dest, int offset, Mac prf, byte[] S, int c, int blockIndex) {
        final int hLen = prf.getMacLength();
        byte U_r[] = new byte[hLen];
        // U0 = S || INT (i);
        byte U_i[] = new byte[S.length + 4];
        System.arraycopy(S, 0, U_i, 0, S.length);
        INT(U_i, S.length, blockIndex);
        for (int i = 0; i < c; i++) {
            U_i = prf.doFinal(U_i);
            xor(U_r, U_i);
        }

        System.arraycopy(U_r, 0, dest, offset, hLen);
    }

    private static void xor(byte[] dest, byte[] src) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] ^= src[i];
        }
    }

    private static void INT(byte[] dest, int offset, int i) {
        dest[offset + 0] = (byte) (i / (256 * 256 * 256));
        dest[offset + 1] = (byte) (i / (256 * 256));
        dest[offset + 2] = (byte) (i / (256));
        dest[offset + 3] = (byte) (i);
    }
}
