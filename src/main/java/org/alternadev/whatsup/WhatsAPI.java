package org.alternadev.whatsup;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.alternadev.whatsup.handler.IWhatsUp;

public class WhatsAPI implements Runnable {

    private String phoneNumber;
    private String imei;
    private String name;
    private int port = 5222;
    // private int timeoutSec = 2;
    // private int timeoutUsec = 0;
    private byte[] incompleteMessage;
    private String disconnectedStatus = "disconnected";
    private String connectedStatus = "connected";
    private String loginStatus = disconnectedStatus;
    private Map<String, String> accountInfo;
    private List<ProtocolNode> messageQueue = new ArrayList<ProtocolNode>();
    ;

	private Map<String, String> challenge = new HashMap<String, String>();
    ;

	private Socket socket;
    private WhatsAppProtocol reader;
    private BinTreeNodeWriter writer;
    private IWhatsUp handler;
    private Thread listen_thread;
    private boolean connected = false;

    public WhatsAPI(IWhatsUp handler) {
        Decode.init();
        reader = new WhatsAppProtocol(Decode.getDictionary());
        writer = new BinTreeNodeWriter(Decode.getDictionary());
        this.handler = handler;
    }

    protected ProtocolNode addFeatures() {
        ProtocolNode child = new ProtocolNode("receipt_acks", null, null, "");
        List<ProtocolNode> children = new ArrayList<ProtocolNode>();
        children.add(child);
        ProtocolNode parent = new ProtocolNode("stream:features", null,
                children, "");
        return parent;
    }

    protected ProtocolNode addAuth() {
        Map<String, String> auth = new HashMap<String, String>();
        auth.put("mechanism", "WAUTH-1");

        auth.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
        return new ProtocolNode("auth", auth, null, "");
    }

    public String encryptPassword() {
        if (this.imei.indexOf(":") > -1) {
            this.imei = imei.toUpperCase();
            return md5(imei + imei);

        } else {
            return md5(reverse(this.imei));
        }
    }

    private String reverse(String imei2) {
        return new StringBuffer(imei2).reverse().toString();
    }

    private String md5(String s) {
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

    protected String authenticate(String nonce) {
        String NC = "00000001";
        String qop = "auth";
        String cnonce = randomUUID();

        String data1 = this.phoneNumber;
        data1 += ":";
        data1 += Constants.WHATSAPP_SERVER;
        data1 += ":";
        data1 += this.encryptPassword();

        String data2 = (md5(data1));
        data2 += ":";
        data2 += nonce;
        data2 += ":";
        data2 += cnonce;

        String data3 = "AUTHENTICATE:";
        data3 += Constants.WHATSAPP_DIGEST;

        String data4 = md5(data2);
        data4 += ":";
        data4 += nonce;
        data4 += ":";
        data4 += NC;
        data4 += ":";
        data4 += cnonce;
        data4 += ":";
        data4 += qop;
        data4 += ":";
        data4 += md5(data3);

        String data5 = md5(data4);

        String response = String
                .format("username=\"%s\",realm=\"%s\",nonce=\"%s\",cnonce=\"%s\",nc=%s,qop=%s,digest-uri=\"%s\",response=%s,charset=utf-8",
                this.phoneNumber, Constants.WHATSAPP_REALM, nonce, cnonce,
                NC, qop, Constants.WHATSAPP_DIGEST, data5);
        return response;
    }
    //TODO: 
    protected String getResponse(String host, String query)
    {
        return "";
    }
    
    protected ProtocolNode addAuthResponse() {
        String nonce = this.challenge.get("nonce");
        String resp = this.authenticate(nonce);
        Map<String, String> map = new HashMap<String, String>();
        map.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
        ProtocolNode node = new ProtocolNode("response", map, null,
                Base64.encode(resp));
        return node;
    }

    protected void sendData(String in) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(
                    this.socket.getOutputStream());
            out.write(in.getBytes());

            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void sendNode(ProtocolNode node) {
        System.out.println(node.nodeString("tx  "));
        sendData(this.writer.write(node));
    }

    protected int readData(byte[] buffer, int leng) {
        try {
            // BufferedReader in = new BufferedReader(new InputStreamReader(
            // socket.getInputStream()));
            int read = socket.getInputStream().read(buffer, 0, leng);
            return read;
            // ret += new String(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return -1;
    }

    protected void processChallenge(ProtocolNode node) {
        String challenge = Base64.decode(node.data);
        String[] strs = challenge.split(",");
        for (String s : strs) {
            String[] k = s.split("=");
            this.challenge.put(k[0], k[1].replace("\"", ""));
        }
    }

    protected void sendMessageReceived(ProtocolNode msg) {
        ProtocolNode reqNode = msg.getChild("request");
        if (reqNode != null) {
            String xmlns = reqNode.getAttribute("xmlns");
            if (xmlns.equals("urn:xmpp:receipts")) {
                Map<String, String> recHash = new HashMap<String, String>();
                recHash.put("xmlns", "urn:xmpp:receipts");
                ProtocolNode recNode = new ProtocolNode("received", recHash,
                        null, "");
                Map<String, String> msgHash = new HashMap<String, String>();
                msgHash.put("to", msg.getAttribute("from"));
                msgHash.put("type", "chat");
                msgHash.put("id", msg.getAttribute("id"));
                List<ProtocolNode> children = new ArrayList<ProtocolNode>();
                children.add(recNode);
                ProtocolNode messageNode = new ProtocolNode("message", msgHash,
                        children, "");
                this.sendNode(messageNode);
            }
        }
    }

    public Map<String, String> accountInfo() {
        return this.accountInfo;
    }

    public boolean connect() {
        try {
            socket = new Socket(Constants.WHATSAPP_HOST, this.port);
            connected = true;
            this.listen_thread = new Thread(this);
            this.listen_thread.start();
            return true;
        } catch (UnknownHostException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void login(String number, String imei, String name) {
        this.phoneNumber = number;
        this.imei = imei;
        this.name = name;
        String res = Constants.WHATSAPP_DEVICE + "-" + Constants.WHATSAPP_VERSION + "-" + this.port;
        String data = this.writer.startStream(Constants.WHATSAPP_SERVER, res);
        ProtocolNode feat = this.addFeatures();
        ProtocolNode auth = this.addAuth();
        this.sendData(data);
        this.sendNode(feat);
        this.sendNode(auth);


    }

    public List<ProtocolNode> getMessages() {
        List<ProtocolNode> ret = new ArrayList<ProtocolNode>();
        ret.addAll(this.messageQueue);
        this.messageQueue.clear();
        return ret;
    }

    protected void sendMessageNode(String msgid, String to, ProtocolNode node) {
        ProtocolNode serverNode = new ProtocolNode("server", null, null, "");

        Map<String, String> xHash = new HashMap<String, String>();
        xHash.put("xmlns", "jabber:x:event");
        List<ProtocolNode> nodes = new ArrayList<ProtocolNode>();
        nodes.add(serverNode);
        ProtocolNode xNode = new ProtocolNode("x", xHash, nodes, "");

        Map<String, String> msgHash = new HashMap<String, String>();
        msgHash.put("to", to + "@" + Constants.WHATSAPP_SERVER);
        msgHash.put("type", "chat");
        msgHash.put("id", msgid);
        List<ProtocolNode> list = new ArrayList<ProtocolNode>();
        list.add(xNode);
        list.add(node);
        ProtocolNode messageNode = new ProtocolNode("message", msgHash, list,
                "");
        this.sendNode(messageNode);
    }

    public void sendMessage(String msgid, String to, String txt) {
        ProtocolNode bodyNode = new ProtocolNode("body", null, null, txt);
        this.sendMessageNode(msgid, to, bodyNode);
    }

    public void sendMessageImage(String msgid, String to, String url,
            String file, String size, String icon) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("xmlns", "urn:xmpp:whatsapp:mms");
        map.put("type", "image");
        map.put("url", url);
        map.put("file", file);
        map.put("size", size);

        ProtocolNode mediaNode = new ProtocolNode("media", map, null, icon);
        this.sendMessageNode(msgid, to, mediaNode);
    }

    public void pong(String msgid) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("to", Constants.WHATSAPP_SERVER);
        map.put("id", msgid);
        map.put("type", "result");
        this.sendNode(new ProtocolNode("iq", map, null, ""));
    }

    private String randomUUID() {
        UUID karl = UUID.randomUUID();
        return karl.toString();
    }

    public void run() {
        //Listen for incoming messages
        int buff_leng = 1024;
        byte[] buffer = new byte[buff_leng];
        int read_leng = 0;
        while (this.connected) {
            if ((read_leng = this.readData(buffer, buff_leng)) > -1) {
                processData(buffer, read_leng);
            } else {
                //Connection Error!
                break;
            }
        }
        if (handler != null) {
            handler.onDisconect(this);
        }
        connected = false;
    }

    private void processData(byte[] data, int leng) {
        reader.addData(data, leng);
        processData();
    }

    private void processData() {
        try {
            ProtocolNode node;;
            // if(node != null) System.out.println(node != null);
            while ((node = reader.nextTree()) != null) {
                System.out.println(node.nodeString("rx  "));
                if (node.tag.equals("challenge")) {
                    this.processChallenge(node);
                    ProtocolNode data2 = this.addAuthResponse();
                    this.sendNode(data2);
                } else if (node.tag.equals("success")) {
                    this.loginStatus = this.connectedStatus;
                    this.accountInfo = new HashMap<String, String>();
                    accountInfo.put("status", node.getAttribute("status"));
                    accountInfo.put("kind", node.getAttribute("kind"));
                    accountInfo.put("creation", node.getAttribute("creation"));
                    accountInfo.put("expiration",
                            node.getAttribute("expiration"));
                    if (handler != null) {
                        handler.onLogin(this, accountInfo);
                    }
                }
                if (node.tag.equals("message")) {
                    messageQueue.add(node);
                    sendMessageReceived(node);
                    if (handler != null) {
                        handler.onMessageReceived(this, node);
                    }
                }
                if (node.tag.equals("iq")
                        && node.attributeHash.get("type").equals("get")
                        && node.children.get(0).tag.equals("ping")) {
                    this.pong(node.attributeHash.get("id"));
                }
            }

        } catch (InvalidTokenException e) {
            e.printStackTrace();
        }
    }
}
