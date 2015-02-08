package com.chigix.bio.proxy.handler;

import com.chigix.bio.proxy.ChigiProxy;
import com.chigix.bio.proxy.FormatDateTime;
import com.chigix.bio.proxy.channel.Channel;
import com.chigix.bio.proxy.utils.ReadBuffer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Socks4aServerChannelHandler extends ChannelHandler {

    private ChannelHandler proxyChannel;

    private Socks4aOperation operation;

    private final ReadBuffer<Integer> buffer;

    private int dstPort = 0;
    private String dstHost = null;

    public Socks4aServerChannelHandler(Channel channel) {
        super(channel);
        this.buffer = new ReadBuffer<Integer>();
        this.operation = Socks4aOperation.REQUEST_AUTH;
    }

    @Override
    public void channelRead(Channel channel, int input) {
        if (this.operation == Socks4aOperation.SEND_TO_PROXY_DIRECT) {
            try {
                this.proxyChannel.getChannel().getOutputStream().write(input);
            } catch (IOException ex) {
                Logger.getLogger(Socks4aServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                channel.close();
                this.proxyChannel.getChannel().close();
            }
            return;
        }
        synchronized (this.buffer) {
            this.buffer.push(input);
        }
        try {
            switch (this.operation) {
                case REQUEST_AUTH:
                    this.requestAuth(channel, this.buffer.toArrayCopy());
                    break;
                case REQUEST_DOMAIN:
                    this.requestDomain(channel, this.buffer.toArrayCopy());
                    break;
                default:
                    throw new ReadContinueException();
            }
        } catch (ReadContinueException ex) {
            return;
        }
        synchronized (this.buffer) {
            this.buffer.clear();
        }
    }

    @Override
    public void channelInactive(Channel channel) {
        System.out.println(FormatDateTime.toTimeString(new Date()) + " BROWSER INACTIVE " + this.dstHost + ":" + this.dstPort);
        if (this.proxyChannel != null) {
            this.proxyChannel.getChannel().close();
        }
    }

    @Override
    public void channelActive(Channel channel) {
        System.out.println("BROWSER ACTIVE");
    }

    private void requestAuth(Channel channel, List<Integer> buffer) throws ReadContinueException {
        if (buffer.size() > 8 && buffer.get(buffer.size() - 1) == 0) {
        } else {
            throw new ReadContinueException();
        }
        if (buffer.get(0) != 4) {
            channel.close();
            return;
        }
        this.dstPort += buffer.get(2) * 256;
        this.dstPort += buffer.get(3);
        if (buffer.get(4) != 0) {
            System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECT ERROR: " + buffer);
            channel.close();
            return;
        }
        switch (buffer.get(1)) {
            case 1:
                this.operation = Socks4aOperation.REQUEST_DOMAIN;
                break;
            case 2:
                this.operation = Socks4aOperation.REQUEST_IP;
                break;
            default:
                channel.close();
                break;
        }
    }

    private void requestDomain(Channel channel, List<Integer> buffer) throws ReadContinueException {
        if (buffer.get(buffer.size() - 1) != 0) {
            throw new ReadContinueException();
        }
        StringBuilder host = new StringBuilder();
        for (Integer b : buffer) {
            if (b == 0) {
                break;
            }
            host.append((char) b.byteValue());
        }
        this.dstHost = host.toString();
        try {
            channel.pushToBuffer(0);
            this.operation = Socks4aOperation.NONE;
            System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECT TO DOMAIN: " + this.dstHost + ":" + this.dstPort);
        } catch (SocketException ex) {
            Logger.getLogger(Socks4aServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
            channel.close();
            return;
        }
        byte port_b1 = 0;
        byte port_b2 = 0;
        if (true) {
            int port_int1 = this.dstPort / 256;
            int port_int2 = this.dstPort - 256 * port_b1;
            port_b1 = Integer.valueOf(port_int1).byteValue();
            port_b2 = Integer.valueOf(port_int2).byteValue();
        }
        Channel dstChannel = null;
        try {
            dstChannel = new Channel(new Socket(this.dstHost, this.dstPort));
        } catch (UnknownHostException ex) {
            System.out.println(FormatDateTime.toTimeString(new Date()) + " UNKNOWN HOST: " + this.dstHost + ":" + this.dstPort);
            try {
                channel.pushToBuffer(new byte[]{91, port_b1, port_b2, 0, 0, 0, 0});
                channel.flushBuffer();
            } catch (IOException ex1) {
                channel.close();
            }
            return;
        } catch (ConnectException ex) {
            System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECT ERROR: " + this.dstHost + ":" + this.dstPort);
            try {
                channel.pushToBuffer(new byte[]{91, port_b1, port_b2, 0, 0, 0, 0});
                channel.flushBuffer();
            } catch (IOException ex1) {
                channel.close();
            }
            return;
        } catch (IOException ex) {
            Logger.getLogger(Socks4aServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        try {
            channel.pushToBuffer(new byte[]{90, port_b1, port_b2});
            channel.pushToBuffer(dstChannel.getRemoteAddress());
            channel.flushBuffer();
        } catch (IOException ex) {
            Logger.getLogger(Socks4aServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
            dstChannel.close();
            channel.close();
            return;
        }
        System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECTED TO [" + this.dstHost + ":" + this.dstPort + "]");
        final String connectionTarget = this.dstHost + ":" + this.dstPort;
        final Channel browserChannel = channel;
        this.proxyChannel = new ChannelHandler(dstChannel) {

            @Override
            public void channelRead(Channel channel, int input) {
                try {
                    browserChannel.getOutputStream().write(input);
                } catch (IOException ex) {
                    browserChannel.close();
                    channel.close();
                    System.out.println(FormatDateTime.toTimeString(new Date()) + " Browser Closed + [" + connectionTarget + "]");
                }
            }

            @Override
            public void channelInactive(Channel channel) {
                System.out.println(FormatDateTime.toTimeString(new Date()) + " PROXY DISCONNECTED [" + connectionTarget + "]");
                browserChannel.close();
            }

            @Override
            public void channelActive(Channel channel) {
                System.out.println(FormatDateTime.toTimeString(new Date()) + " PROXY CONNECTED: " + connectionTarget);
            }

        };
        ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(this.proxyChannel));
        this.operation = Socks4aOperation.SEND_TO_PROXY_DIRECT;
    }

}
