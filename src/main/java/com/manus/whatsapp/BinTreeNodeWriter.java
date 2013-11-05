package com.manus.whatsapp;

import java.util.HashMap;
import java.util.Map;

public class BinTreeNodeWriter {

    private String output = "";
    private Map<String, Integer> tokenMap;
    private KeyStream _key = null;
    public BinTreeNodeWriter(String[] dic) {
        tokenMap = new HashMap<String, Integer>();
        for (int i = 0; i < dic.length; i++) {
            if (dic[i] != null) {
                if (dic[i].length() > 0) {
                    this.tokenMap.put(dic[i], i);
                }
            }
        }
    }

    public String startStream(String domain, String resource) {
        output = "";
        Map<String, String> attributes = new HashMap<String, String>();
        String headers = "WA";
        //output += "\u0001\u0002";
        headers += writeInt8(1);
        headers += writeInt8(2);
        attributes.put("to", domain);
        attributes.put("resource", resource);
        this.writeListStart(attributes.size() * 2 + 1);

        output += writeInt8(1);
        this.writeAttributes(attributes);
        String ret = headers + this.flushBuffer();
        return ret;

    }

    public String write(ProtocolNode node) {
        this.output = "";
        if (node == null) {
            this.output += "\u0000";
        } else {
            this.writeInternal(node);
        }
        return this.flushBuffer();
    }

    private void writeInternal(ProtocolNode node) {
        int len = 1;
        if (node.attributeHash != null) {
            len += node.attributeHash.size() * 2;
        }
        if (node.children != null && node.children.size() > 0) {
            len += 1;
        }
        if (node.data.length() > 0) {
            len += 1;
        }
        this.writeListStart(len);
        this.writeString(node.tag);
        this.writeAttributes(node.attributeHash);
        if (node.data.length() > 0) {
            this.writeBytes(node.data);
        }
        if (node.children != null && node.children.size() > 0) {
            this.writeListStart(node.children.size());
            for (ProtocolNode node1 : node.children) {
                this.writeInternal(node1);
            }
        }
    }

    private String flushBuffer() {
        String data = (getKey() != null)?WhatsHelper.getString(_key.encode(WhatsHelper.getByteString(output))):output;
        int size = data.length();
        String ret  = writeInt8(getKey() != null ? (1 << 4) : 0);
        ret += writeInt16(size);
        ret += data;
        output = "";
        return ret;
    }

    private void writeToken(int token) {
        if (token < 0xf5) {
            this.output += (char) token;
        } else if (token <= 0x1f4) {
            this.output += writeInt8(0xfe) + (char) (token - 0xf5);
        }
    }

    private void writeJid(String user, String server) {
        this.output += writeInt8(0xfa);
        if (user.length() > 0) {
            this.writeString(user);
        } else {
            this.writeToken(0);
        }
        this.writeString(server);
    }

    private String writeInt8(int v) {
        return String.format("%c", (v & 0xFF));
    }

    private String writeInt16(int v) {
        String ret = "";
        ret += writeInt8((v & 0xff00) >> 8);
        ret += writeInt8((v & 0x00ff) >> 0);
        return ret;
    }

    private String writeInt24(int v) {
        String output = "";
        output += writeInt8((v & 0xff0000) >> 16);
        output += writeInt8((v & 0x00ff00) >> 8);
        output += writeInt8((v & 0x0000ff) >> 0);
        return output;
    }

    private void writeBytes(String bytes) {
        int len = bytes.length();
        if (len >= 0x100) {
            this.output += writeInt8(0xfd);
            this.output += this.writeInt24(len);
        } else {
            this.output += writeInt8(0xfc);
            this.output += this.writeInt8(len);
        }
        this.output += bytes;
    }

    private void writeString(String tag) {
        if (this.tokenMap.get(tag) != null) {
            Integer key = this.tokenMap.get(tag);
            this.writeToken(key);
        } else {
            int index = tag.indexOf('@');
            if (index >= 0) {
                String server = tag.substring(index + 1);
                String user = tag.substring(0, index);
                this.writeJid(user, server);
            } else {
                this.writeBytes(tag);
            }
        }
    }

    private void writeAttributes(Map<String, String> attributes) {
        if (attributes.size() > 0) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                this.writeString(entry.getKey());
                this.writeString(entry.getValue());
            }
        }
    }

    private void writeListStart(int len) {
        if (len == 0) {
            this.output += String.format("%c", 0);
        } else if (len < 256) {
            this.output += String.format("%c%c", 0xf8,(char) len);
        } else {
            this.output += String.format("%c%c", 0xf9,(char) len);
        }
    }

    /**
     * @return the _key
     */
    public KeyStream getKey() {
        return _key;
    }

    /**
     * @param key the _key to set
     */
    public void setKey(KeyStream key) {
        this._key = key;
    }
}
