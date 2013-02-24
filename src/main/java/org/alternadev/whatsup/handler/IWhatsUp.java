/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alternadev.whatsup.handler;

import java.util.List;
import java.util.Map;
import org.alternadev.whatsup.ProtocolNode;
import org.alternadev.whatsup.WhatsAPI;

/**
 *
 * @author Manus
 */
public interface IWhatsUp {
    
    public void onDisconect(WhatsAPI api);

    public void onMessageReceived(WhatsAPI api, ProtocolNode message);

    public void onLogin(WhatsAPI api, Map<String, String> accountInfo);
}
