package com.manus.whatsapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BinTreeNodeReader {

    private String[] dic;
    private byte[] input = new byte[1024 * 5];
    private int _leng;
    private KeyStream _key = null;

    public BinTreeNodeReader(String[] dic) {
        this.dic = dic;
    }

    public ProtocolNode nextTree() throws InvalidTokenException,
            IncompleteMessageException {
        return nextTree(null, _leng);
    }

    public ProtocolNode nextTree(byte[] input, int leng) throws InvalidTokenException,
            IncompleteMessageException {
        if(_leng >= 3)
        {
            if(this.peekInt16(1) > _leng)
            {
                _leng = 0;
            }
        }
        
        
        if (input != null) {
            append(input, leng);
        }
        if (_leng <= 0) {
            return null;
        }
        int stanzaFlag = (peekInt8() & 0xF0) >> 4;
        int stanzaSize = this.peekInt16(1);
        //System.out.println("\t\t Flag: " + stanzaFlag + " & Size = " + stanzaSize + " Buffer leng: " + _leng);
        if (stanzaSize > _leng) {
//            throw new IncompleteMessageException(
//                    ("Incomplete Message! ("
//                    + stanzaSize + ", " + _leng + ")"),
//                    this.input);
            //Whait for more data :D
            return null;
        }
        this.readInt24();
        if (((stanzaFlag & 8) == 8) && (getKey() != null)) {
            int t = 0;
            byte[] decoded = _key.decode(this.input, 0, stanzaSize);
            //System.out.println("Stanza: " + stanzaSize + " decoded: " + decoded.length + " total size: " + _leng);
            for (int i = 0; i < decoded.length; i++) {
                this.input[t++] = decoded[i];
            }
            for (int i = stanzaSize; i < _leng; i++) {
                this.input[t++] = this.input[i];
            }
        }
        if (stanzaSize > 0) {
            return nextTreeInternal();
        }
        return null;
    }

    private void append(byte[] newData, int leng) {
        int t = 0;
        for (int i = _leng; i < _leng + leng; i++) {
            input[i] = newData[t++];
        }
        _leng += leng;
    }

    protected String getToken(int token) throws InvalidTokenException {
        if (token >= 0 && token < dic.length) {
            return dic[token];
        }
        throw new InvalidTokenException(token);
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
            ret = this.fillArray(size);
        } else if (token == 0xfd) {
            int size = this.readInt24();
            ret = this.fillArray(size);
        } else if (token == 0xfe) {
            int size = this.readInt8();
            ret = this.fillArray(size + 0xf5);
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

    protected LinkedHashMap<String, String> readAttributes(int size)
            throws InvalidTokenException {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        int attribCount = (size - 2 + size % 2) / 2;
        for (int i = 0; i < attribCount; i++) {
            String key = this.readString(this.readInt8());
            String value = this.readString(this.readInt8());
            map.put(key, value);
        }
        return map;
    }

    protected ProtocolNode nextTreeInternal() throws InvalidTokenException {
        ProtocolNode ret;
        //this.readInt8();
        int token = this.readInt8();
        int size = this.readListSize(token);

        token = this.readInt8();
        if (token == 1) {
            ret = new ProtocolNode("start", this.readAttributes(size), null,
                    "");

        } else if (token == 2) {
            ret = null;
        } else {

            String tag = this.readString(token);
            LinkedHashMap<String, String> attributes = this.readAttributes(size);
            if (size % 2 == 1) {
                ret = new ProtocolNode(tag, attributes, null, "");
            } else {
                token = this.readInt8();
                if (this.isListTag(token)) {
                    ret = new ProtocolNode(tag, attributes, this.readList(token), "");
                } else {
                    ret = new ProtocolNode(tag, attributes, null, this.readString(token));
                }
            }
        }
//        System.out.println("Buffer status: " + _leng);
//        if(_leng > 0)
//            System.out.println("WARNING!: " + _leng);
        return ret;
    }

    protected boolean isListTag(int token) {
        return (token == 248 || token == 0 || token == 249);
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
            //size = this.readInt8();
            //throw new InvalidTokenException(token);
        }

        return size;
    }

    private void removeFromInput(int num) {
        int t = 0;
        for (int i = num; i < _leng; i++) {
            input[t++] = input[i];
        }
        _leng -= num;
    }

    protected int peekInt8() {
        int ret = 0;
        if (_leng >= 1) {
            ret = input[0] & 0xFF;
        }

        return ret;
    }

    protected int readInt8() {
        int ret = 0;
        if (_leng >= 1) {
            ret = input[0] & 0xFF;
            removeFromInput(1);
        }

        return ret;
    }

    protected int peekInt16() {
        return peekInt16(0);
    }

    protected int peekInt16(int offset) {
        int ret = 0;
        if (_leng >= 2 + offset) {
            ret = (input[offset] & 0xFF) << 8;
            ret += input[1 + offset] & 0xFF;
        }

        return ret;
    }

    protected int readInt16() {
        int ret = peekInt16();
        if (ret > 0) {
            removeFromInput(2);
        }
        return ret;
    }

    protected int peekInt24() {
        int ret = 0;
        if (_leng >= 3) {
            ret = (input[0] & 0xFF) << 16;
            ret += (input[1] & 0xFF) << 8;
            ret += (input[2] & 0xFF) << 0;
        }

        return ret;
    }

    protected int readInt24() {
        int ret = peekInt24();
        if (ret > 0) {
            removeFromInput(3);
        }
        return ret;
    }

    protected String fillArray(int len) {
        String ret = "";
        if (_leng >= len) {
            ret = new String(intToCharArray(input)).substring(0, len);
            removeFromInput(len);
        }
        return ret;
    }

    private char[] intToCharArray(byte[] in) {
        char[] peda = new char[in.length];
        for (int i = 0; i < in.length; i++) {
            peda[i] = (char) (((char) in[i] & 0xFF));
        }
        return peda;
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
