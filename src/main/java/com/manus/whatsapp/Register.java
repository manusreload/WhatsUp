/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.manus.whatsapp;

/**
 *
 * @author Manus
 */
public class Register {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length == 0)
        {
            usagre(args);
            return;
        }
        //Request
        if(args[0].equals("-r"))
        {
            WhatsRegistration.RequestCode(args[1], args[2], args[3], "");
        }
        
    }
    
    public static void usagre(String[] args)
    {
        System.out.println("Usagre:");
        System.out.println("-r <cc> <phone> <method=sms|voice> <personalPassword>    Request code");
        System.out.println("-v <cc> <phone> <code>   Verify code");
    }
    
}
