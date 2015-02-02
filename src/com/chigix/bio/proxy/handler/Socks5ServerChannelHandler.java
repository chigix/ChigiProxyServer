package com.chigix.bio.proxy.handler;

import com.chigix.bio.proxy.FormatDateTime;
import com.chigix.bio.proxy.channel.Channel;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Administrator
 */
public class Socks5ServerChannelHandler extends ChannelHandler {

    private Socks5Operation operation;

    private Queue<Integer> readBuffer;
    private final ReadBufferMonitor readBufferMonitor;

    private String dstHost;
    private int dstPort;
    private boolean schemaSet = false;
    private boolean auth_injected = false;
    private int sendCount = 0;

    private ChannelHandler proxyServerChannel = null;

    public Socks5ServerChannelHandler(Channel channel) {
        super(channel);
        this.operation = Socks5Operation.AUTHENTICATION;
        //this.readBuffer = new ArrayList<Integer>();
        this.readBufferMonitor = new ReadBufferMonitor();
        this.readBuffer = new ConcurrentLinkedQueue<Integer>();
    }

    @Override
    public void channelActive(Channel channel) {
        System.out.println("ACTIVE");
    }

    @Override
    public void channelInactive(Channel channel) {
        System.out.println("BROWSER INACTIVE");
        if (this.proxyServerChannel != null) {
            this.proxyServerChannel.getChannel().close();
        }
    }

    @Override
    public void channelRead(Channel channel, int input) {
        if (this.operation == Socks5Operation.DIRECTLY_SEND_BACK) {
            try {
                this.proxyServerChannel.getChannel().getOutputStream().write(input);
            } catch (IOException ex) {
                this.proxyServerChannel.getChannel().close();
                channel.close();
            }
            //System.out.print((char) input);
            return;
        }
        synchronized (this.readBufferMonitor) {
            this.readBufferMonitor.lengthPace();
            readBuffer.add(input);
        }
        try {
            switch (this.operation) {
                case AUTHENTICATION:
                    this.auth(channel, this.readBuffer);
                    break;
                case REQUEST:
                    this.request(channel, this.readBuffer);
                    break;
                case DOMAIN_REQUEST:
                    this.domainRequest(channel, this.readBuffer);
                    break;
                case IP_REQUEST:
                    this.ipRequest(channel, this.readBuffer);
                    break;
//                case HTTP_CONN:
//                      打入 HTTP 代理信息
//                    if (input == 10) {
//                        this.httpConnSend(channel, readBuffer);
//                    } else {
//                        throw new ReadContinueException();
//                    }
//                    break;
                default:
                    System.out.println("DEFAULT:");
                    System.out.println(this.readBuffer);
                    throw new ReadContinueException();
            }
        } catch (ReadContinueException e) {
            return;
        }
        synchronized (this.readBufferMonitor) {
            this.readBufferMonitor.initLength();
            this.readBuffer = new ConcurrentLinkedQueue<Integer>();
        }
    }

    public void auth(Channel channel, Queue<Integer> buffer) throws ReadContinueException {
        if (this.readBufferMonitor.getLength() < 3) {
            throw new ReadContinueException();
        }
        if (Arrays.equals(buffer.toArray(), new Integer[]{5, 1, 0})) {
            try {
                channel.getOutputStream().write(new byte[]{5, 0});
                channel.getOutputStream().flush();
            } catch (IOException ex) {
                Logger.getLogger(ChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.operation = Socks5Operation.REQUEST;
        }
    }

    public void request(Channel channel, Queue<Integer> buffer) throws ReadContinueException {
        if (this.readBufferMonitor.getLength() < 4) {
            throw new ReadContinueException();
        }
        Integer[] bufferArray = new Integer[this.readBufferMonitor.getLength()];
        int iteratorCountDown = 0;
        for (Iterator<Integer> it = buffer.iterator(); it.hasNext();) {
            Integer integer = it.next();
            bufferArray[iteratorCountDown] = integer;
            iteratorCountDown++;
        }
        if (Arrays.equals(bufferArray, new Integer[]{5, 1, 0, 3})) {
            this.operation = Socks5Operation.DOMAIN_REQUEST;
        } else if (Arrays.equals(bufferArray, new Integer[]{5, 1, 0, 1})) {
            this.operation = Socks5Operation.IP_REQUEST;
        } else {
            System.out.println("UNIMPLEMENT: " + Arrays.toString(bufferArray));
            this.operation = Socks5Operation.NONE;
        }
    }

    public void ipRequest(Channel channel, Queue<Integer> buffer) throws ReadContinueException {
        if (this.readBufferMonitor.getLength() < 6) {
            throw new ReadContinueException();
        }
        StringBuilder domain = new StringBuilder();
        int port = 0;
        Iterator<Integer> it = buffer.iterator();
        Integer integer = it.next();
        domain.append(String.valueOf(integer)).append(".");
        integer = it.next();
        domain.append(String.valueOf(integer)).append(".");
        integer = it.next();
        domain.append(String.valueOf(integer)).append(".");
        integer = it.next();
        domain.append(String.valueOf(integer));
        integer = it.next();
        port += integer * 256;
        integer = it.next();
        port += integer;
        this.dstHost = domain.toString();
        this.dstPort = port;
        // Create the connection to proxy server.
        final String connectionTarget = this.dstHost + ":" + this.dstPort;
        System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECT TO IP:" + connectionTarget);
        try {
            Channel c = new Channel(new Socket(this.dstHost, this.dstPort));
            final Channel browserChannel = channel;
            this.sendReply(0, c.getRemoteAddress(), browserChannel);
            System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECTED TO IP:" + connectionTarget);
            this.proxyServerChannel = new ChannelHandler(c) {

                @Override
                public void channelRead(Channel channel, int input) {
                    //System.out.print((char) input);
                    try {
                        browserChannel.getOutputStream().write(input);
                    } catch (IOException ex) {
                        browserChannel.close();
                        channel.close();
                        System.out.println("BROWSER CHANNEL WRITE ERROR: " + browserChannel.isClosed() + " [" + connectionTarget + "]");
                    }
                }

                @Override
                public void channelInactive(Channel channel) {
                    System.out.println("PROXY DISCONNECTED [" + connectionTarget + "]");
                }

                @Override
                public void channelActive(Channel channel) {
                    System.out.println("PROXY CONNECTED [" + connectionTarget + "]");
                }

            };
            new Thread(new ChannelHandlerThread(this.proxyServerChannel)).start();
        } catch (UnknownHostException ex) {
            try {
                this.sendReply(4, new byte[]{0, 0, 0, 0}, channel);
                System.out.println(FormatDateTime.toTimeString(new Date()) + " Host Unreachable [" + connectionTarget + "]");
            } catch (IOException ex1) {
                Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (ConnectException ex) {
            try {
                this.sendReply(5, new byte[]{0, 0, 0, 0}, channel);
                System.out.println(FormatDateTime.toTimeString(new Date()) + " Connection refused [" + connectionTarget + "]");
            } catch (IOException ex1) {
                Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (IOException ex) {
            channel.close();
            System.out.println(FormatDateTime.toTimeString(new Date()) + " SOCKET OPEN ERROR: " + this.dstHost + ":" + this.dstPort);
            this.operation = Socks5Operation.NONE;
            Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.operation = Socks5Operation.DIRECTLY_SEND_BACK;
    }

    public void domainRequest(Channel channel, Queue<Integer> buffer) throws ReadContinueException {
        int domainLen = buffer.peek();
        if (this.readBufferMonitor.getLength() < (domainLen + 3)) {
            throw new ReadContinueException();
        }
        StringBuilder domain = new StringBuilder();
        int port = 0;
        int iteratorCountDown = 0;
        for (Integer integer : buffer) {
            if (this.readBufferMonitor.getLength() - 1 == iteratorCountDown) {
                port += integer;
            } else if (this.readBufferMonitor.getLength() - 2 == iteratorCountDown) {
                port += integer * 256;
            } else if (0 == iteratorCountDown) {
                ;
            } else {
                domain.append((char) integer.byteValue());
            }
            iteratorCountDown++;
        }
        this.dstHost = domain.toString();
        this.dstPort = port;
        System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECT TO DOMAIN:" + this.dstHost + ":" + this.dstPort);
        // Create the connection to proxy server.
        try {
            Channel c = new Channel(new Socket(this.dstHost, this.dstPort));
            final Channel browserChannel = channel;
            final String connectionTarget = this.dstHost + "/" + c.getRemoteHostAddress() + ":" + this.dstPort;
            this.sendReply(0, c.getRemoteAddress(), browserChannel);
            System.out.println(FormatDateTime.toTimeString(new Date()) + " CONNECTED TO DOMAIN:" + connectionTarget);
            this.proxyServerChannel = new ChannelHandler(c) {

                @Override
                public void channelRead(Channel channel, int input) {
                    //System.out.print((char) input);
                    try {
                        browserChannel.getOutputStream().write(input);
                    } catch (IOException ex) {
                        browserChannel.close();
                        channel.close();
                        System.out.println("BROWSER CHANNEL WRITE ERROR: " + browserChannel.isClosed() + " [" + connectionTarget + "]");
                    }
                }

                @Override
                public void channelInactive(Channel channel) {
                    System.out.println("PROXY DISCONNECTED [" + connectionTarget + "]");
                    browserChannel.close();
                }

                @Override
                public void channelActive(Channel channel) {
                    System.out.println("PROXY CONNECTED [" + connectionTarget + "]");
                }

            };
            new Thread(new ChannelHandlerThread(this.proxyServerChannel)).start();
        } catch (UnknownHostException ex) {
            try {
                this.sendReply(4, new byte[]{0, 0, 0, 0}, channel);
                System.out.println(FormatDateTime.toTimeString(new Date()) + " Host unreachable [" + this.dstHost + ":" + this.dstPort + "]");
            } catch (IOException ex1) {
                Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (ConnectException ex) {
            try {
                this.sendReply(5, new byte[]{0, 0, 0, 0}, channel);
                System.out.println(FormatDateTime.toTimeString(new Date()) + " Connection refused [" + this.dstHost + ":" + this.dstPort + "]");
            } catch (IOException ex1) {
                Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (IOException ex) {
            System.out.println(FormatDateTime.toTimeString(new Date()) + " SOCKET OPEN ERROR:" + this.dstHost + ":" + this.dstPort);
            Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.operation = Socks5Operation.DIRECTLY_SEND_BACK;
//        if (this.dstPort == 80) {
//            this.operation = Socks5Operation.HTTP_CONN;
//        } else {
//            this.operation = Socks5Operation.DIRECTLY_SEND_BACK;
//        }
    }
//
//    public void httpConnSend(Channel channel, Queue<Integer> buffer) {
//        byte[] request = new byte[this.readBufferMonitor.getLength()];
//        int iteratorCountDown = 0;
//        for (Iterator<Integer> it = buffer.iterator(); it.hasNext();) {
//            Integer integer = it.next();
//            request[iteratorCountDown] = integer.byteValue();
//            iteratorCountDown++;
//        }
//        System.out.println("ConnSend: " + new String(request));
//        if (!schemaSet) {
//            // CHECK IF SCHEMA TO SET
//            String request_string = new String(request);
//            request = request_string.replaceFirst("^(GET|POST|PUT|HEAD|DELETE)[ ]+(/.+)", "$1 http://" + this.dstHost + "$2").getBytes();
//            this.schemaSet = true;
//        }
//        if (!this.auth_injected) {
//            String request_string = new String(request);
//            if (request_string.matches("^(GET|POST|PUT|HEAD|DELETE) .+ HTTP.+([\n\r]|\r\n)")) {
//                request = new StringBuilder(request_string)
//                        .append("Proxy-Connection: Keep-Alive\n")
//                        .append("Proxy-Authorization: Basic ")
//                        .append(new String(Base64.encodeBase64("username:password".getBytes()))).append("\n")
//                        .toString().getBytes();
//                this.auth_injected = true;
//            } else {
//                System.out.println(false);
//                System.out.println(request_string);
//            }
//        }
//        System.out.println(new String(request));
//        try {
//            this.proxyServerChannel.getChannel().getOutputStream().write(request);
//            this.proxyServerChannel.getChannel().getOutputStream().flush();
//            this.sendCount++;
//            this.operation = Socks5Operation.DIRECTLY_SEND_BACK;
//        } catch (IOException ex) {
//            Logger.getLogger(Socks5ServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    private void sendReply(int type, byte[] ip, Channel channel) throws IOException {
        int port_b1 = this.dstPort / 256;
        int port_b2 = this.dstPort - 256 * port_b1;
        channel.getOutputStream().write(new byte[]{
            5, Integer.valueOf(type).byteValue(), 0, 1,
            ip[0], ip[1], ip[2], ip[3],
            Integer.valueOf(port_b1).byteValue(), Integer.valueOf(port_b2).byteValue()
        });
        System.out.println(Arrays.toString((new byte[]{
            5, Integer.valueOf(type).byteValue(), 0, 1,
            ip[0], ip[1], ip[2], ip[3],
            Integer.valueOf(port_b1).byteValue(), Integer.valueOf(port_b2).byteValue()
        })));
    }

    private class ReadBufferMonitor {

        private int length = 0;

        public ReadBufferMonitor() {
            this.length = 0;
        }

        public void initLength() {
            this.length = 0;
        }

        public void lengthPace() {
            this.length++;
        }

        public int getLength() {
            return this.length;
        }

    }

}
