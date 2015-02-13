package com.chigix.bio.proxy.channel;

import com.chigix.bio.proxy.FormatDateTime;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Channel {

    private final Socket socket;

    private InputStream in;
    private OutputStream out;

    private boolean isClosedFlag;
    private final ConcurrentLinkedQueue<Integer> writeBuffer;

    private final Set<Tunnel> parentTunnels;

    public Channel(Socket socket) {
        this.isClosedFlag = false;
        this.socket = socket;
        this.writeBuffer = new ConcurrentLinkedQueue();
        this.parentTunnels = new HashSet<Tunnel>();
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
     * Push a data in integer into the buffer queue.
     *
     * @param data
     * @throws java.net.SocketException
     */
    public void pushToBuffer(int data) throws SocketException {
        if (this.isClosed()) {
            throw new SocketException();
        }
        this.writeBuffer.add(data);
    }

    public void pushToBuffer(byte[] data) throws SocketException {
        List<Integer> copy = new LinkedList<Integer>();
        for (int i = 0; i < data.length; i++) {
            copy.add(Byte.valueOf(data[i]).intValue());
        }
        if (this.isClosed()) {
            throw new SocketException();
        }
        this.writeBuffer.addAll(copy);
    }

    /**
     * Flush the datas in the buffer queue to channel bound.
     *
     * @throws IOException
     */
    public void flushBuffer() throws IOException {
        synchronized (this.writeBuffer) {
            byte[] buffer = new byte[this.writeBuffer.size()];
            int countdown = 0;
            Integer buffer_element = null;
            while ((buffer_element = this.writeBuffer.poll()) != null) {
                buffer[countdown++] = buffer_element.byteValue();
            }
            this.out.write(buffer);
        }
    }

    /**
     * Close this channel.
     */
    public void close() {
        System.out.println(FormatDateTime.toTimeString(new Date()) + " SOCKET CLOSE:" + this.socket.getInetAddress());
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
     * Returns the raw IP address of the <code>InetAddress</code> object for
     * local address. The result is in network byte order: the highest order
     * byte of the address is in <code>getAddress()[0]</code>.
     *
     * @return the raw IP address of this object.
     */
    public byte[] getLocalAddress() {
        return this.socket.getLocalAddress().getAddress();
    }

    /**
     * Returns the raw IP address of this <code>InetAddress</code> object for
     * the connected address. The result is in network byte order: the highest
     * order byte of the address is in <code>getAddress()[0]</code>.
     *
     * @return the raw IP address of this object.
     */
    public byte[] getRemoteAddress() {
        return this.socket.getInetAddress().getAddress();
    }

    public String getRemoteHostAddress() {
        return this.socket.getInetAddress().getHostAddress();
    }

    public void registerTunnel(Tunnel tunnel) {
        this.parentTunnels.add(tunnel);
    }

    public Tunnel[] getParentTunnels() {
        return this.parentTunnels.toArray(new Tunnel[this.parentTunnels.size()]);
    }

}
