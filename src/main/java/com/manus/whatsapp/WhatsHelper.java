/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.manus.whatsapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

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
        if (password == null) {
            return null;
        }
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
    public static String md5(String s) {
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        m.reset();
        m.update(s.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String hashtext = bigInt.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }
    // HTTP GET request
    public static String http_get(String uri) {
        try {
            URL obj = new URL(uri);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            
            // optional default is GET
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", Constants.WHATSAPP_USER_AGENT);
            con.setRequestProperty("Accept", "text/json");
            int responseCode = con.getResponseCode();
            
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (MalformedURLException ex) {
            Logger.getLogger(WhatsHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WhatsHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }
    
    static String sha1(byte[] input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(input);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(WhatsHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    static String sha1(String input) {
        return sha1(input.getBytes());
    }
}
