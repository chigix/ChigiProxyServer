/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.channel;

import com.chigix.chigiproxytunnel.handler.ChannelHandler;
import com.chigix.chigiproxytunnel.handler.ChannelHandlerThread;
import com.chigix.event.Event;
import com.chigix.event.Listenable;
import com.chigix.event.Listener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Channel implements Listenable {

    private final Socket socket;
    private boolean ClosedFlag;
    private InputStream in;
    private OutputStream out;
    private ConcurrentLinkedQueue<Listener> listeners;

    public Channel(Socket socket) {
        this.socket = socket;
        this.ClosedFlag = false;
        this.listeners = new ConcurrentLinkedQueue<>();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        this.in = in;
        this.out = out;
    }

    public boolean isClosedFlag() {
        return ClosedFlag;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public ChannelHandler getCurrentHandler() {
        return ChannelHandlerThread.findHandlerByChannel(this);
    }

    /**
     * Close this channel.
     */
    public void close() {
        this.ClosedFlag = true;
        CloseEvent e = new CloseEvent(this);
        this.castEvent(e);
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Returns the raw IP address of the <code>InetAddress</code> object for
     * local address. The result is in network byte order: the highest order
     * byte of the address is in <code>getAddress()[0]</code>.
     *
     * @return
     */
    public byte[] getLocalAddress() {
        return this.socket.getLocalAddress().getAddress();
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public int getRemotePort() {
        return this.socket.getPort();
    }

    public String getRemoteHostAddress() {
        return this.socket.getInetAddress().getHostAddress();
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void resetInputStream() throws IOException {
        this.in = this.socket.getInputStream();
    }

    public void resetOutputStream() throws IOException {
        this.out = this.socket.getOutputStream();
    }

    @Override
    public void addListener(Listener l) {
        this.listeners.offer(l);
    }

    /**
     * Remove a single instance of listener registered in this channel if there
     * is one or more such listeners registered.
     *
     * @param l
     * @return {@code true} if the channel's registered listeners contained this
     * specified listener.
     */
    public boolean removeListener(Listener l) {
        return this.listeners.remove(l);
    }

    protected final void castEvent(Event e) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            Listener next = it.next();
            next.performEvent(e);
        }
    }

    public class CloseEvent implements Event {

        @Override
        public String getName() {
            return "CHIGIX_CHANNEL_CLOSE_EVENT";
        }

        private final Channel channelToClose;

        public CloseEvent(Channel channelToClose) {
            this.channelToClose = channelToClose;
        }

        public Channel getChannelToClose() {
            return channelToClose;
        }

    }

}
