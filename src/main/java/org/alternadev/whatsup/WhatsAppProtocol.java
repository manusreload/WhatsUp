package org.alternadev.whatsup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhatsAppProtocol {

    private static final int MAX_BUFFER_LENG = 5 * 1024;
    private String[] dic;
    private byte[] input = new byte[MAX_BUFFER_LENG];
    private int leng = 0;
    private int readPos = 0;

    public WhatsAppProtocol(String[] dic) {
        this.dic = dic;
    }
    public void addData(byte[] data, int leng)
    {
        for(int i = 0; i < leng; i++)
        {
            input[i+this.leng] = data[i];
        }
        this.leng += leng;
    }
    public ProtocolNode nextTree() throws InvalidTokenException {
        int playloadSize = this.readInt16();
        
        if(playloadSize > leng)
        {
            readPos =0;
            return null;
        }
        if (playloadSize > 0) {
            return this.nextTreeInternal();
        }
        return null;
    }

    protected String getToken(int token) throws InvalidTokenException {
        if (token >= 0 && token < dic.length) {
            return dic[token];
        }
        throw new InvalidTokenException(token);
    }
    private byte readByte()
    {
        return input[readPos++];
    }
    protected String readString(int token) throws InvalidTokenException {
        String ret = "";
        if (token == -1) {
            throw new InvalidTokenException(token);
        }

        if (token > 4 && token < 0xf5) {
            ret = this.getToken(token);
        }
        if (token == 0) {
            ret = "";
        } else if (token == 0xfc) {
            int size = this.readInt8();
            ret = this.getString(size);
        } else if (token == 0xfd) {
            int size = this.readInt24();
            ret = this.getString(size);
        } else if (token == 0xfe) {
            int size = this.readInt8();
            ret = this.getString(size + 0xf5);
        } else if (token == 0xfa) {
            String user = this.readString(this.readInt8());
            String server = this.readString(this.readInt8());
            if (user.length() > 0 && server.length() > 0) {
                ret = user + "@" + server;
            } else if (server.length() > 0) {
                ret = server;
            }
        }
        return ret;
    }

    protected Map<String, String> readAttributes(int size)
            throws InvalidTokenException {
        Map<String, String> map = new HashMap<String, String>();
        int attribCount = (size - 2 + size % 2) / 2;
        for (int i = 0; i < attribCount; i++) {
            String key = this.readString(this.readInt8());
            String value = this.readString(this.readInt8());
            map.put(key, value);
        }
        return map;
    }

    protected ProtocolNode nextTreeInternal() throws InvalidTokenException {
        int token = this.readInt8();
        int size = this.readListSize(token);

        token = this.readInt8();
        if (token == 1) {
            return new ProtocolNode("start", this.readAttributes(size), null,
                    "");

        } else if (token == 2) {
            return null;
        }
        String tag = this.readString(token);
        Map<String, String> attributes = this.readAttributes(size);
        if (size % 2 == 1) {
            removeReaded();
            return new ProtocolNode(tag, attributes, null, "");
        }
        token = this.readInt8();
        if (this.isListTag(token)) {
            List<ProtocolNode> list = this.readList(token);
            removeReaded();
            return new ProtocolNode(tag, attributes, list, "");
        }
        String data = this.readString(token);
        removeReaded();
        return new ProtocolNode(tag, attributes, null, data);
    }

    protected boolean isListTag(int token) {
        return (token == 248 || token == 0 || token == 259);
    }

    protected List<ProtocolNode> readList(int token)
            throws InvalidTokenException {
        int size = this.readListSize(token);
        List<ProtocolNode> ret = new ArrayList<ProtocolNode>();
        for (int i = 0; i < size; i++) {
            ret.add(this.nextTreeInternal());
        }
        return ret;
    }

    protected int readListSize(int token) throws InvalidTokenException {
        int size = 0;
        if (token == 0xf8) {
            size = this.readInt8();
        } else if (token == 0xf9) {
            size = this.readInt16();
        } else {
            throw new InvalidTokenException(token);
        }

        return size;
    }

    protected int readInt24() {
        int ret = 0;
        if (this.input.length >= 3) {
            ret = (readByte() & 0xFF) << 16;
            ret += (readByte() & 0xFF) << 8;
            ret += (readByte() & 0xFF);
        }
        return ret;
    }

    private void removeReaded() {
        for (int i = readPos; i < leng; i++) {
            input[i - readPos] = input[i];
        }
        leng = leng - readPos;
        readPos = 0;    
    }

    protected int peekInt16() {
        int ret = 0;
        if (leng >= 2) {
            ret = (input[readPos] << 8) & 0xFF;
            ret ^= input[readPos+1] & 0xFF;
        }

        return ret;
    }

    protected int readInt16() {
        int ret = peekInt16();
        if (ret > 0) {
            readPos+=2;
        }
        return ret;
    }

    protected int readInt8() {
        int ret = 0;
        if (leng >= 1) {
            ret = readByte() & 0xFF;
        }

        return ret;
    }

    protected String getString(int len) {
        String ret = "";
        byte[] arr = new byte[len];
        if(leng >= len)
        {
            for(int i = 0; i < len;i++)
            {
                arr[i] = readByte();
            }
            ret = new String(arr);
        }
        return ret;
    }
}
