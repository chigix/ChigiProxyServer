package com.chigix.bio.proxy.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Channel {

    private int closecount;

    private final Socket socket;

    private InputStream in;
    private OutputStream out;

    private boolean isClosedFlag;

    public Channel(Socket socket) {
        this.isClosedFlag = false;
        this.socket = socket;
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        } catch (IOException ex) {
            Logger.getLogger(Channel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    /**
     * Close this channel.
     */
    public void close() {
        System.out.println("SOCKET CLOSE:" + this.socket.getInetAddress());
        this.isClosedFlag = true;
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Channel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isClosed() {
        if (this.isClosedFlag) {
            return true;
        }
        if (this.socket.isClosed()) {
            return true;
        }
        return false;
    }

    /**
     * Returns the raw IP address of this <code>InetAddress</code> object. The
     * result is in network byte order: the highest order byte of the address is
     * in <code>getAddress()[0]</code>.
     *
     * @return the raw IP address of this object.
     */
    public byte[] getLocalAddress() {
        return this.socket.getLocalAddress().getAddress();
    }

    public byte[] getRemoteAddress() {
        return this.socket.getInetAddress().getAddress();
    }

    public String getRemoteHostAddress() {
        return this.socket.getInetAddress().getHostAddress();
    }

}
