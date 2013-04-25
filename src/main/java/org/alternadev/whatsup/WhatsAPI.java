package org.alternadev.whatsup;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WhatsAPI {

    public static final int DEBUG_LEVEL_SEE_PACKETS = 1;
    public static final int DEBUG_LEVEL_SEE_NODES = 2;
    private String phoneNumber;
    private String imei;
    private String name;
    private String pw;
    private String whatsAppHost = "bin-short.whatsapp.net";
    private String whatsAppServer = "s.whatsapp.net";
    private String whatsAppGroupServer = "g.us";
    private String whatsAppRealm = "s.whatsapp.net";
    private String whatsAppDigest = "xmpp/s.whatsapp.net";
    private String device = "iPhone";
    private String whatsAppVer = "2.8.7";
    private int port = 5222;
    // private int timeoutSec = 2;
    // private int timeoutUsec = 0;
    private byte[] incompleteMessage;
    private String _whatsAppUserAgent = "WhatsApp/2.3.53 S40Version/14.26 Device/Nokia302";
    private String _whatsAppToken = "PdA2DJyKoUrwLw1Bg6EIhzh502dF9noR9uFCllGk1354754753509";
    private String disconnectedStatus = "disconnected";
    private String connectedStatus = "connected";
    private String loginStatus;
    private Map<String, String> accountInfo;
    private List<ProtocolNode> messageQueue;
    private WhatsAPIHandler handler;
    private byte[] challenge;
    private Socket socket;
    private BinTreeNodeReader reader;
    private BinTreeNodeWriter writer;
    private KeyStream _inputKey;
    private KeyStream _outputKey;
    private int msgCounter = 1;
    private int _debug_level =  DEBUG_LEVEL_SEE_PACKETS | DEBUG_LEVEL_SEE_NODES;
    private boolean listenning = false;

    public WhatsAPI() {
        init();
    }

    public WhatsAPI(String number, String imei, String name) {

        this.phoneNumber = number;
        this.imei = imei;
        this.name = name;
        init();
    }
    

    private void init() {
        Decode.initV287();
        reader = new BinTreeNodeReader(Decode.getDictionary());
        writer = new BinTreeNodeWriter(Decode.getDictionary());
        //this.challenge = "";
        this.messageQueue = new ArrayList<ProtocolNode>();
        this.loginStatus = this.disconnectedStatus;
    }

    public void setHandler(WhatsAPIHandler handler) {
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
        LinkedHashMap<String, String> auth = new LinkedHashMap<String, String>();

        auth.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
        auth.put("mechanism", "WAUTH-1");
        auth.put("user", this.getPhoneNumber());
        return new ProtocolNode("auth", auth, null, "");
    }

    public byte[] encryptPassword() {
        try {
            return com.sun.org.apache.xml.internal.security.utils.Base64.decode(getPassword());
            //		if (this.imei.indexOf(":") > -1) {
            //			this.imei = imei.toUpperCase();
            //			return md5(imei + imei);
            //
            //		} else {
            //		}
            //		}
        } catch (Base64DecodingException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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

    protected byte[] authenticate() {
        try {
            //this.challenge = WhatsHelper.hexStringToByteArray("6adb3e8a60a9073f474a53aef0810421a2f71678");
            byte[] key = WhatsHelper.deriveKey(this.encryptPassword(), this.challenge, 16, 20);
            if(key == null) return null;
            _inputKey = new KeyStream(key);
            _outputKey = new KeyStream(key);
            String d = getPhoneNumber() + WhatsHelper.getString(challenge) + "1366667401"; //((int) (System.currentTimeMillis() / 1000));
            return _outputKey.encode(WhatsHelper.getByteString(d), 0, d.length(), false);

        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected ProtocolNode addAuthResponse() {
        byte[] resp = this.authenticate();
        if(resp == null) return null;
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
        ProtocolNode node = new ProtocolNode("response", map, null,
                WhatsHelper.getString(resp));
        return node;
    }

    protected void sendData(String in) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(
                    this.socket.getOutputStream());
            for (int i = 0; i < in.length(); i++) {
                out.write((byte) in.charAt(i));
            }
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void sendNode(ProtocolNode node) {
        if ((_debug_level & DEBUG_LEVEL_SEE_PACKETS) == DEBUG_LEVEL_SEE_PACKETS) {
            System.out.println(node.nodeString("tx  "));
        }
        String data = this.writer.write(node);
        sendData(data);
    }

    protected int readData(byte[] buffer) {
        try {
            // BufferedReader in = new BufferedReader(new InputStreamReader(
            // socket.getInputStream()));
            int leng = socket.getInputStream().read(buffer);

            return leng;
            // ret += new String(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return -1;
    }

    public static int unsignedToBytes(byte a) {
        int b = ((a & 0xFF));
        return ((b));
    }

    protected void processChallenge(ProtocolNode node) {
        this.challenge = WhatsHelper.getByteString(node.data);
    }

    protected void processInboundData(byte[] data, int leng) {
        try {
            ProtocolNode node = reader.nextTree(data, leng);
            while (node != null) {
                if ((_debug_level & DEBUG_LEVEL_SEE_PACKETS) == DEBUG_LEVEL_SEE_PACKETS) {
                    System.out.println(node.nodeString("rx  "));
                }
                if (node.tag.equals("challenge")) {
                    this.processChallenge(node);
                } else if (node.tag.equals("success")) {
                    this.loginStatus = this.connectedStatus;
                    this.accountInfo = new HashMap<String, String>();
                    accountInfo.put("status", node.getAttribute("status"));
                    accountInfo.put("kind", node.getAttribute("kind"));
                    accountInfo.put("creation", node.getAttribute("creation"));
                    accountInfo.put("expiration",
                            node.getAttribute("expiration"));
                    if (handler != null) {
                        handler.onLogin(getPhoneNumber(), node.getAttribute("status"), node.getAttribute("kind"), Long.parseLong(node.getAttribute("creation")) * 1000, Long.parseLong(node.getAttribute("expiration")) * 1000);
                    }
                }
                if (node.tag.equals("message")) {
                    messageQueue.add(node);
                    sendMessageReceived(node);
                    if (handler != null) {
                        handler.onMessageReceived(node);
                    }
                    if (node.getChild("composing") != null) {
                        if (handler != null) {
                            handler.onUserComposing(getPhoneNumber(), node.getAttribute("from"));
                        }
                    }
                    if (node.getChild("paused") != null) {
                        if (handler != null) {
                            handler.onUserPaused(getPhoneNumber(), node.getAttribute("from"));
                        }
                    }
                    if (node.getChild("notify") != null && !node.getChild("notify").getAttribute("name").equals("")
                             && node.getChild("body") != null) {
                        if (node.getChild("body") != null) {
                            if (handler != null) {
                                handler.onMessageReceivedText(getPhoneNumber(),
                                        node.getAttribute("from"),
                                        node.getAttribute("id"),
                                        node.getAttribute("type"),
                                        Long.parseLong(node.getAttribute("t")) * 1000,
                                        node.getChild("notify").getAttribute("name"),
                                        node.getChild("body").data);
                            }
                        } else if (node.getChild("media") != null) {
                            ProtocolNode media = node.getChild("media");
                            if (media.getAttribute("type").equals("image")) {
                                if (handler != null) {
                                    handler.onMessageReceivedImage(getPhoneNumber(),
                                            node.getAttribute("from"),
                                            node.getAttribute("id"),
                                            node.getAttribute("type"),
                                            Long.parseLong(node.getAttribute("t")) * 1000,
                                            node.children.get(0).getAttribute("name"),
                                            Integer.parseInt(media.getAttribute("size")),
                                            media.getAttribute("url"),
                                            media.getAttribute("file"),
                                            media.getAttribute("mimetype"),
                                            media.getAttribute("filehash"),
                                            Integer.parseInt(media.getAttribute("width")),
                                            Integer.parseInt(media.getAttribute("height")),
                                            media.data);
                                }
                            } else if (media.getAttribute("type").equals("video")) {
                                if (handler != null) {
                                    handler.onMessageReceivedVideo(getPhoneNumber(),
                                            node.getAttribute("from"),
                                            node.getAttribute("id"),
                                            node.getAttribute("type"),
                                            Long.parseLong(node.getAttribute("t")) * 1000,
                                            node.children.get(0).getAttribute("name"),
                                            Integer.parseInt(media.getAttribute("size")),
                                            media.getAttribute("url"),
                                            media.getAttribute("file"),
                                            media.getAttribute("mimetype"),
                                            media.getAttribute("filehash"),
                                            Integer.parseInt(media.getAttribute("duration")),
                                            media.getAttribute("vcodec"),
                                            media.getAttribute("acodec"),
                                            media.data);
                                }
                            } else if (media.getAttribute("type").equals("audio")) {
                                if (handler != null) {
                                    handler.onMessageReceivedAudio(getPhoneNumber(),
                                            node.getAttribute("from"),
                                            node.getAttribute("id"),
                                            node.getAttribute("type"),
                                            Long.parseLong(node.getAttribute("t")) * 1000,
                                            node.children.get(0).getAttribute("name"),
                                            Integer.parseInt(media.getAttribute("size")),
                                            media.getAttribute("url"),
                                            media.getAttribute("file"),
                                            media.getAttribute("mimetype"),
                                            media.getAttribute("filehash"),
                                            Integer.parseInt(media.getAttribute("duration")),
                                            media.getAttribute("acodec"));
                                }
                            }
                        }
                    }
                    if (node.getChild("x") != null) {
                        if (handler != null) {
                            handler.onMessageReceivedServer(getPhoneNumber(), node.getAttribute("from"), node.getAttribute("id"), node.getAttribute("type"), Long.parseLong(node.getAttribute("t")) * 1000);
                        }
                    }
                    if (node.getChild("received") != null) {
                        if (handler != null) {
                            handler.onMessageReceivedClient(getPhoneNumber(), node.getAttribute("from"), node.getAttribute("id"), node.getAttribute("type"), Long.parseLong(node.getAttribute("t")) * 1000);
                        }
                    }

                }
                if (node.tag.equals("iq")
                        && node.attributeHash.get("type").equals("get")
                        && node.children.get(0).tag.equals("ping")) {
                    this.pong(node.getAttribute("id"));
                }
                if (node.tag.equals("failure")) {
                    if (handler != null) {
                        handler.onError(node);
                    }
                }

                node = this.reader.nextTree();
            }
        } catch (IncompleteMessageException e) {
            this.incompleteMessage = e.getData();
            e.printStackTrace();
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        }

    }

    public Map<String, String> accountInfo() {
        return this.accountInfo;
    }

    public void connect() throws UnknownHostException, IOException {
        socket = new Socket(this.whatsAppHost, this.port);
    }

    public boolean login(String pw){
        this.setPassword(pw);
        return login();
    }
    public boolean login() {

        String res = this.device + "-" + this.whatsAppVer + "-" + this.port;
        String data = this.writer.startStream(this.whatsAppServer, res);
        ProtocolNode feat = this.addFeatures();
        ProtocolNode auth = this.addAuth();
        this.sendData(data);
        this.sendNode(feat);
        this.sendNode(auth);
        byte[] buffer = new byte[1024 * 2];
        int leng = this.readData(buffer);
        if(leng < 0) return false;
        this.processInboundData(buffer, leng);

        ProtocolNode data2 = this.addAuthResponse();
        if(data2 == null) return false;
        this.sendNode(data2);
        reader.setKey(_inputKey);
        writer.setKey(_outputKey);
        int i = 0;
        do {
            if (!pollMessages()) {
                return false;
            }
        } while ((i++ < 100)
                && this.loginStatus.equals(this.disconnectedStatus));
        return true;
    }
    byte[] poll_buffer = new byte[1024 * 2];

    public boolean pollMessages() {
        int leng = this.readData(poll_buffer);
        if (leng < 0) {
            if (handler != null) {
                handler.onDisconnected();
            }
            disconnect();
            return false;
        }
        this.processInboundData(poll_buffer, leng);
        return true;
    }

    public void listen() {
        listenning = true;
        new Thread(new Runnable() {
            public void run() {
                while (pollMessages() && listenning);
            }
        }).start();
    }

    public List<ProtocolNode> getMessages() {
        List<ProtocolNode> ret = new ArrayList<ProtocolNode>();
        ret.addAll(this.messageQueue);
        this.messageQueue.clear();
        return ret;

    }

    private String getMsgId() {
        return String.valueOf((int) System.currentTimeMillis() / 1000) + "-" + this.msgCounter++;
    }

    protected String sendMessageNode(String to, ProtocolNode node) {
        ProtocolNode serverNode = new ProtocolNode("server", null, null, "");

        LinkedHashMap<String, String> xHash = new LinkedHashMap<String, String>();
        xHash.put("xmlns", "jabber:x:event");
        List<ProtocolNode> nodes = new ArrayList<ProtocolNode>();
        nodes.add(serverNode);
        ProtocolNode xNode = new ProtocolNode("x", xHash, nodes, "");
        LinkedHashMap<String, String> notify = new LinkedHashMap<String, String>();
        notify.put("xmlns", "urn:xmpp:whatsapp");
        notify.put("name", this.getName());
        ProtocolNode notNode = new ProtocolNode("notify", notify, null, "");

        LinkedHashMap<String, String> request = new LinkedHashMap<String, String>();
        request.put("xmlns", "urn:xmpp:receipts");
        ProtocolNode reqNode = new ProtocolNode("request", request, null, "");

        LinkedHashMap<String, String> msgHash = new LinkedHashMap<String, String>();
        if(to.indexOf("-") > 0)
        {
            msgHash.put("to", to + "@" + this.whatsAppGroupServer);
        }
        else
        {
            msgHash.put("to", to + "@" + this.whatsAppServer);
        }
        msgHash.put("type", "chat");
        msgHash.put("id", getMsgId());
        msgHash.put("t", String.valueOf((int) (System.currentTimeMillis() / 1000)));
        List<ProtocolNode> list = new ArrayList<ProtocolNode>();
        list.add(xNode);
        list.add(notNode);
        list.add(reqNode);
        list.add(node);
        ProtocolNode messageNode = new ProtocolNode("message", msgHash, list,
                "");
        this.sendNode(messageNode);
        return msgHash.get("id");
    }

    public String sendMessage(String to, String txt) {
        ProtocolNode bodyNode = new ProtocolNode("body", null, null, txt);
        return this.sendMessageNode(to, bodyNode);
    }

    protected void sendMessageReceived(ProtocolNode msg) {
        ProtocolNode reqNode = msg.getChild("request");
        if (reqNode != null) {
            String xmlns = reqNode.getAttribute("xmlns");
            if (xmlns.equals("urn:xmpp:receipts")) {
                LinkedHashMap<String, String> recHash = new LinkedHashMap<String, String>();
                recHash.put("xmlns", "urn:xmpp:receipts");
                ProtocolNode recNode = new ProtocolNode("received", recHash,
                        null, "");
                LinkedHashMap<String, String> msgHash = new LinkedHashMap<String, String>();
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

    public void sendMessageImage(String to, String url,
            String file, String size, String icon) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("xmlns", "urn:xmpp:whatsapp:mms");
        map.put("type", "image");
        map.put("url", url);
        map.put("file", file);
        map.put("size", size);

        ProtocolNode mediaNode = new ProtocolNode("media", map, null, icon);
        this.sendMessageNode(to, mediaNode);
    }

    public void pong(String msgid) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("to", this.whatsAppServer);
        map.put("id", msgid);
        map.put("type", "result");
        this.sendNode(new ProtocolNode("iq", map, null, ""));
    }

    public void disconnect()
    {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(WhatsAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        init();
    }
    private String randomUUID() {
        UUID karl = UUID.randomUUID();
        return karl.toString();
    }

    /**
     * @return the phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @param phoneNumber the phoneNumber to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return the imei
     */
    public String getImei() {
        return imei;
    }

    /**
     * @param imei the imei to set
     */
    public void setImei(String imei) {
        this.imei = imei;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the pw
     */
    public String getPassword() {
        return pw;
    }

    /**
     * @param pw the pw to set
     */
    public void setPassword(String pw) {
        this.pw = pw;
    }
}
