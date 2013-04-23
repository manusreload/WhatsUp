/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alternadev.whatsup;

/**
 *
 * @author manus
 */
public class RC4 {

    private int[] S = new int[256];
    private int[] a;
    private int i = 0;
    private int j = 0;
    private int keylen;

    public RC4(byte[] key, int drop) {
        int j = 0;
        int k = 0;
        keylen = key.length;
        for (int i = 0; i < 256; i++) {
            S[i] = (byte) i;
        }
        for (int i = 0; i < 256; i++) {
            k = key[i % keylen];
            j = (j + k + S[i]) & 255;
            swap(i, j);
        }
        byte[] T = new byte[drop];
        for (int i = 0; i < drop; i++) {
            T[i] = (byte) i;
        }
        cipher(T, 0, drop);
    }
    public byte[] cipher(byte[] data, int offset, int length)
    {
        a = S.clone();
        //$r = '';
        int t = 0;
        byte[] res = new byte[length];
        for (int n = length; n > 0; n--) {
            i = (i + 1) & 255;
            j = (j + S[i]) & 255;
            swap(i, j);
            int d = data[offset++] & 0xFF;
            //$r .= chr($d ^ $this->s[($this->s[$this->i] + $this->s[$this->j]) & 255]);
            res[t++] = (byte) (d ^ S[(S[i] + S[j]) & 255]);
        }
        //S = a.clone();

        return res;
    }
    protected void swap(int i,int j)
    {
        int aux = S[i];
        S[i] = S[j];
        S[j] = aux;
    }
}