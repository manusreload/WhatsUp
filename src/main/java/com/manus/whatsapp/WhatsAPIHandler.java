/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.manus.whatsapp;

import java.util.Map;

/**
 *
 * @author manus
 */
public interface WhatsAPIHandler {
    
    public void onError(ProtocolNode error);
    public void onDisconnected();
    public void onLogin(String phone, String status, String kind, long creation, long expiration);
    public void onUserComposing(String phone, String from);
    public void onUserPaused(String phone, String from);
    public void onMessageReceived(ProtocolNode message);
    public void onMessageReceivedText(String phone, String from, String id, String type, long time, String userName, String body);
    public void onMessageReceivedImage(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file,String mimeType, String fileHash, int width, int height, String data);
    public void onMessageReceivedVideo(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file,String mimeType, String fileHash, int duration, String video_coded, String auido_codec, String data);
    public void onMessageReceivedAudio(String phone, String from, String id, String type, long time, String userName, int imageSize, String url, String file,String mimeType, String fileHash, int duration, String auido_codec);
    public void onMessageReceivedVCard(String phone, String from, String id, String type, long time, String userName, String cardName, String data);
    public void onMessageReceivedLocation(String phone, String from, String id, String type, long time, String userName, String longitude, String latitude, String data);
    public void onMessageReceivedPlace(String phone, String from, String id, String type, long time, String userName, String longitude, String latitude, String url, String data);
    public void onMessageReceivedServer(String phone, String to, String id, String type, long time);
    public void onMessageReceivedClient(String phone, String to, String id, String type, long time);
    
    
}
