/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alternadev.whatsup;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Encoder;

/**
 *
 * @author manus
 */
public class KeyStream {

    private RC4 rc4;
    private byte[] _key;
    SecretKeySpec keySpec;

    public KeyStream(byte[] key) {
        this._key = key;
        keySpec = new SecretKeySpec(
                key,
                "Hmacsha1");
        this.rc4 = new RC4(key, 256);
    }

    public byte[] encode(byte[] data) {
        return encode(data, 0, data.length);
    }
    public byte[] encode(byte[] data, int offset, int length) {
        return encode(data, offset, length, true);
    }

    public byte[] encode(byte[] data, int offset, int length, boolean append) {
        try {
            //this.rc4 = new RC4(_key, 256);
            byte[] d = rc4.cipher(data, offset, length);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(keySpec);
            byte[] h = mac.doFinal(d);
            if (append) {
                return append(d,0,d.length,
                              h,0,4);
            } else {
                return append(h,0,4,
                              d,0,d.length);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KeyStream.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(KeyStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public byte[] decode(byte[] data, int offset, int length) {
        return rc4.cipher(data, offset + 4, length - 4);
    }
    public byte[] append(byte[] data1, int off1, int leng1, byte[] data2, int off2, int leng2)
    {
        byte[] res = new byte[leng1+leng2];
        int t = 0;
        for(int i = off1; i < off1 + leng1; i++)
        {
            res[t++] = data1[i];
        }
        for(int i = off2; i < off2 + leng2; i++)
        {
            res[t++] = data2[i];
        }
        return res;
    }
}
