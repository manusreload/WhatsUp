/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.manus.whatsapp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class App {

    static final String PHONE_NUMBER = "";
    static final String IMEI = ""; //MAC-Address on iPhone
    static final String USERNAME = "";
    static final String PW = "";
    
    static final String TEST_PHONE_NUMBER = "";

    public static void main(String[] args) throws IOException {

        WhatsAPI api = new WhatsAPI(PHONE_NUMBER, IMEI, USERNAME);

        api.setHandler(new WhatsAppHandler());
        api.connect();
        api.login(PW);
        api.listen();

        Scanner s = new Scanner(System.in);
        String line;
        while ((line = s.nextLine()) != null) {
            if (line.equals("/q")) {
                break;
            }
            if (line.equals("/send")) {
                api.sendMessage(TEST_PHONE_NUMBER,
                        "Hello world!");
            }
        }

    }

    public static ProtocolNode getSubNode(ProtocolNode node, String tag) {
        for (int i = 0; i < node.children.size(); i++) {
            if (node.children.get(i).tag.equals(tag)) {
                return node.children.get(i);
            }
        }
        return null;
    }

    public static String getUser(String from) {
        return from.substring(0, from.indexOf("@"));
    }

    public static class WhatsAppHandler implements WhatsAPIHandler {

        public void onError(ProtocolNode error) {
            System.out.println(error.nodeString());
        }

        public void onDisconnected() {
            System.out.println("Disconnected from server!");

        }

        public void onLogin(String phone, String status, String kind, long creation, long expiration) {

            System.out.println("Login: " + phone + " Status: " + status + " Created on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(creation)));
        }

        public void onUserComposing(String phone, String from) {
            System.out.println("onUserComposing: " + from);

        }

        public void onUserPaused(String phone, String from) {

            System.out.println("onUserPaused: " + from);
        }

        public void onMessageReceived(ProtocolNode message) {
        }

        public void onMessageReceivedText(String phone, String from, String id, String type, long time, String userName, String body) {

            System.out.println("onMessageReceivedText: " + from + " name: " + userName + " text: " + body);
        }

        public void onMessageReceivedImage(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file, String mimeType, String fileHash, int width, int height, String data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedVideo(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file, String mimeType, String fileHash, int duration, String video_coded, String auido_codec, String data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedAudio(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file, String mimeType, String fileHash, int duration, String auido_codec) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedVCard(String phone, String from, String id, String type, long time, String userName, String cardName, String data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedLocation(String phone, String from, String id, String type, long time, String userName, String longitude, String latitude, String data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedPlace(String phone, String from, String id, String type, long time, String userName, String longitude, String latitude, String url, String data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void onMessageReceivedServer(String phone, String to, String id, String type, long time) {
            System.out.println("onMessageReceivedServer: " + to + " id: " + id + " type: " + type);
        }

        public void onMessageReceivedClient(String phone, String to, String id, String type, long time) {

            System.out.println("onMessageReceivedClient: " + to + " id: " + id + " type: " + type);
        }
    }
}
