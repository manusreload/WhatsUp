/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.manus.whatsapp;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Manus
 */
public class WhatsRegistration {
    public static String RequestCode(String countryCode, String phone, String method, String ps)
    {
        
         String id = build_identity(new StringBuilder(phone).reverse().toString());
         String token = WaToken.GenerateToken(phone);
         String locale = countryCode;
         String language = Locale.getDefault().getCountry();
        
        String uri = "https://v.whatsapp.net/v2/code?cc=" + countryCode 
                + "&in=" + phone 
                + "&to=" + countryCode + phone 
                + "&lg=" + language 
                + "&lc=" + locale 
                + "&mcc=214&mnc=001&method=" + method 
                + "&id=" + id 
                + "&token=" + URLEncoder.encode(token);
           
        String resp = WhatsHelper.http_get(uri);
        return resp;
    }
     public static String GenerateIdentity(String phoneNumber, String salt)
    {
        return WhatsHelper.sha1( new StringBuilder(phoneNumber + salt).reverse().toString() );
    }
     
    public static String build_identity(String id)
    {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(id.getBytes());
            return URLEncoder.encode(new String(result));
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(WhatsRegistration.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
