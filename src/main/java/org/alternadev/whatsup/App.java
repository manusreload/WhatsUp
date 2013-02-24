/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alternadev.whatsup;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.alternadev.whatsup.handler.IWhatsUp;

/**
 *
 * @author Manus
 */
public class App {
// The user phone number including the country code without '+' or '00'.
    static final String PHONE_NUMBER = "";
    static final String IMEI = ""; //MAC-Address on iPhone
    static final String USERNAME = "";

    public static void main(String[] args) {
        WhatsAPI api = new WhatsAPI(new TestWhatsUp.WhatsApp());
        if (api.connect()) {

            api.login(PHONE_NUMBER, IMEI, USERNAME);
            api.sendMessage(System.currentTimeMillis() + "-1", "",
                    "Ola k ase");

            while (true) {
                //api.pollMessages();
                List<ProtocolNode> list = api.getMessages();

                for (ProtocolNode msg : list) {
                    System.out.println(msg.nodeString());
                }
            }
        }
        else
        {
            System.out.println("I can't connect!");
            
        }

    }

    public static class WhatsApp implements IWhatsUp {

        public void onDisconect() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void onMessageReceived(ProtocolNode message) {
            System.out.println(message.nodeString());
        }

        public void onDisconect(WhatsAPI api) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void onMessageReceived(WhatsAPI api, ProtocolNode message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void onLogin(WhatsAPI api, Map<String, String> accountInfo) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
